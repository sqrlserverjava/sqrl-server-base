package com.github.dbadia.sqrl.server.backchannel;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfigOperations;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.enums.SqrlClientParam;
import com.github.dbadia.sqrl.server.enums.SqrlRequestCommand;
import com.github.dbadia.sqrl.server.enums.SqrlRequestOpt;
import com.github.dbadia.sqrl.server.enums.SqrlServerSideKey;
import com.github.dbadia.sqrl.server.enums.SqrlSignatureType;
import com.github.dbadia.sqrl.server.exception.SqrlClientRequestProcessingException;
import com.github.dbadia.sqrl.server.exception.SqrlException;
import com.github.dbadia.sqrl.server.exception.SqrlInvalidDataException;
import com.github.dbadia.sqrl.server.exception.SqrlInvalidRequestException;
import com.github.dbadia.sqrl.server.util.SqrlConstants;
import com.github.dbadia.sqrl.server.util.SqrlSanitize;
import com.github.dbadia.sqrl.server.util.SqrlUtil;
import com.github.dbadia.sqrl.server.util.SqrlVersionUtil;

/**
 * Parses a SQRL client request and validates all signatures
 *
 * @author Dave Badia
 *
 */
public class SqrlClientRequest {
	private static final Logger logger = LoggerFactory.getLogger(SqrlClientRequest.class);

	private static final String NUT_EQUALS = "nut=";

	private final String logHeader;

	private final Integer				negotiatedSqrlProtocolVersion;
	private final SqrlNutToken			nut;
	private final SqrlRequestCommand	clientCommand;
	private final Map<SqrlServerSideKey, byte[]>	requestKeyTableRaw			= new ConcurrentHashMap<>();
	private final Map<SqrlServerSideKey, String>	requestKeyTableBase64	= new ConcurrentHashMap<>();
	private final List<SqrlRequestOpt>	optList				= new ArrayList<>();

	private final HttpServletRequest	servletRequest;
	private final String				clientParam;
	private final String				serverParam;
	private final String				correlator;

	public SqrlClientRequest(final HttpServletRequest servletRequest, final SqrlPersistence persistence,
			final SqrlConfigOperations configOps) throws SqrlClientRequestProcessingException {
		this.logHeader = SqrlClientRequestLoggingUtil.getLogHeader();
		this.servletRequest = servletRequest;
		this.clientParam = getRequiredParameter(servletRequest, "client");
		this.serverParam = getRequiredParameter(servletRequest, "server");
		this.nut = new SqrlNutToken(configOps, extractFromSqrlCsvString(serverParam, NUT_EQUALS));
		final String decoded = SqrlUtil.base64UrlDecodeDataFromSqrlClientToString(clientParam);
		// parse server - not a name value pair, just the query string we gave
		this.correlator = extractFromSqrlCsvString(serverParam, SqrlClientParam.cor.toString());

		// parse client
		final Map<String, String> clientNameValuePairTable = parseLinesToNameValueMap(decoded);
		// clientVersionString format is 1[,n],[n-m]
		final String clientVersionString = clientNameValuePairTable.get(SqrlClientParam.ver.toString());
		final Collection<Integer> clientVersionsSupported = SqrlVersionUtil
				.parseClientVersionString(clientVersionString);
		final Integer commonProtocolVersion = SqrlVersionUtil
				.findHighestCommonVersion(SqrlConstants.SUPPORTED_SQRL_VERSIONS, clientVersionsSupported);
		if (commonProtocolVersion == null) {
			throw new SqrlClientRequestProcessingException("No common SQRL protocol version found");
		}
		// Sanity check; we probably need to take different actions for different versions so account for that here
		if (!SqrlConstants.SQRL_VERSION_1.equals(commonProtocolVersion)) {
			throw new SqrlClientRequestProcessingException(
					"Unable to process SQRL protcol version " + commonProtocolVersion);
		}
		this.negotiatedSqrlProtocolVersion = commonProtocolVersion;

		// parse opt
		final String optListString = clientNameValuePairTable.get(SqrlClientParam.opt.toString());
		if (SqrlUtil.isNotBlank(optListString)) {
			for (final String optString : optListString.split("~")) {
				try {
					final SqrlRequestOpt clientOpt = SqrlRequestOpt.valueOf(optString);
					if(!optList.add(clientOpt)) {
						logger.warn("{}Client sent opt {} more than once in clientParam of {}", logHeader, clientOpt, clientParam);
					}
				} catch (final IllegalArgumentException e) {
					throw new SqrlInvalidRequestException("Unknown SQRL client option '" + optString + "'", e);
				}
			}
		}

		// parse keys
		for (final Map.Entry<String, String> entry : parseLinesToNameValueMap(decoded).entrySet()) {
			final SqrlServerSideKey keyType = SqrlServerSideKey.valueOfOrNull(entry.getKey());
			if (keyType == null) {
				continue;
			}
			final String keyBase64 = entry.getValue();
			// Sanity check for a client sending the same key more than once
			if (requestKeyTableRaw.containsKey(keyType)) {
				// Uh oh, is the value different?
				if (requestKeyTableBase64.get(keyType).equals(entry.getValue())) {
					logger.info("Client sent the key " + keyType
							+ " multiple times but with the same value; this should be reported as a minor bug to the client author.  clientParam="
							+ clientParam);
				} else {
					throw new SqrlInvalidRequestException("Client sent the key " + keyType
							+ " multiple times in the same request, each with different values: " + clientParam);
				}
			} else {
				// Store the keys in our tables
				final byte[] keyBytes = SqrlUtil.base64UrlDecodeDataFromSqrlClient(entry.getValue());
				requestKeyTableRaw.put(keyType, keyBytes);
				requestKeyTableBase64.put(keyType, keyBase64);
			}
		}
		logger.debug("{}keys found in request: {}", logHeader, requestKeyTableBase64.keySet());

		// Per the SQRL spec, since the server response is not signed, we must check the value that comes
		// back to ensure it wasn't tampered with
		final String expectedServerValue = persistence.fetchTransientAuthData(correlator,
				SqrlConstants.TRANSIENT_NAME_SERVER_PARROT);
		if (SqrlUtil.isBlank(expectedServerValue)) {
			throw new SqrlInvalidRequestException("Server parrot was not found in persistence");
		}
		if (!expectedServerValue.equals(serverParam)) {
			logger.warn("{}Server parrot mismatch: Expected={}, Received={}", logHeader, expectedServerValue,
					serverParam);
			throw new SqrlInvalidRequestException("Server parrot mismatch, possible tampering");
		}

		// Validate the signatures
		boolean idsFound = false;
		for (final SqrlSignatureType aSignatureType : SqrlSignatureType.values()) {
			final String signatureParamValue = servletRequest.getParameter(aSignatureType.toString());
			if (SqrlUtil.isNotBlank(signatureParamValue)) {
				// Validate the signature
				validateSignature(SqrlSignatureType.getSignatureToKeyParamTable().get(aSignatureType),
						signatureParamValue);
				if (aSignatureType == SqrlSignatureType.ids) {
					idsFound = true;
				}
			}
		}

		// All requests must have the ids signature
		if (!idsFound) {
			throw new SqrlInvalidRequestException(
					"ids was missing in SQRL client request: " + clientNameValuePairTable);
		}

		final String clientCommandString = clientNameValuePairTable.get(SqrlClientParam.cmd.toString());
		try {
			this.clientCommand = SqrlRequestCommand.valueOf(clientCommandString.toUpperCase());
		} catch (final IllegalArgumentException e) {
			// We handle all SQRL v1 verbs, so don't set FUNCTIONS_NOT_SUPPORTED, treat it as an invalid
			// request
			// instead
			throw new SqrlInvalidRequestException(
					logHeader + "Recevied invalid SQRL command from client: '" + clientCommandString + "'");
		}
	}

	/**
	 * The correlator is our only key to determining which user this is, so it's critical we parse this out first
	 */
	public static String parseCorrelatorOnly(final HttpServletRequest servletRequest) throws SqrlException {
		final String serverParam = getRequiredParameter(servletRequest, "server");
		// parse server - not a name value pair, just the query string we gave
		return extractFromSqrlCsvString(serverParam, SqrlClientParam.cor.toString());
	}

	private static String getRequiredParameter(final HttpServletRequest servletRequest, final String requiredParamName)
			throws SqrlInvalidRequestException {
		final String value = servletRequest.getParameter(requiredParamName);
		if (value == null || value.trim().length() == 0) {
			throw new SqrlInvalidRequestException("Missing required parameter " + requiredParamName
					+ ".  Request contained: " + SqrlUtil.buildRequestParamList(servletRequest));
		}
		try {
			SqrlSanitize.inspectIncomingSqrlData(value);
		} catch (final SqrlInvalidDataException e) {
			// Convert to SqrlInvalidRequestException since it came from a SQRL client app
			throw new SqrlInvalidRequestException(e.getMessage(), e);
		}
		return value;
	}

	static String extractFromSqrlCsvString(final String serverParam, final String variableToFind)
			throws SqrlInvalidRequestException {
		final String toSearch = SqrlUtil.base64UrlDecodeDataFromSqrlClientToString(serverParam);
		String toFind = variableToFind;
		if (!variableToFind.endsWith("=")) {
			toFind += "=";
		}
		// Find the nut param
		int index = toSearch.indexOf(toFind);
		if (index == -1) {
			throw new SqrlInvalidRequestException("Could not find " + toFind + " in server param: " + toSearch);
		}
		String value = toSearch.substring(index + toFind.length());
		// Need to find the end of the nut string - could be & if from our login page URL or SqrlServerReply.SEPARATOR
		// if from a server reply
		index = value.indexOf(SqrlClientReply.SEPARATOR);
		if (index > -1) {
			value = value.substring(0, index);
		}
		index = value.indexOf('&');
		if (index > -1) {
			value = value.substring(0, index);
		}
		try {
			SqrlSanitize.inspectIncomingSqrlData(value);
		} catch (final SqrlInvalidDataException e) {
			// Convert to SqrlInvalidRequestException since it came from a SQRL client app
			throw new SqrlInvalidRequestException(e.getMessage(), e);
		}
		return value;
	}

	private void validateSignature(final SqrlServerSideKey keyName, final String signatureParamValue)
			throws SqrlInvalidRequestException {
		final byte[] signatureFromMessage = SqrlUtil.base64UrlDecodeDataFromSqrlClient(signatureParamValue);

		try {
			final byte[] publicKey = requestKeyTableRaw.get(keyName);
			if (publicKey == null) {
				throw new SqrlInvalidRequestException(
						SqrlClientRequestLoggingUtil.getLogHeader() + keyName + " not found in client param: " + clientParam);
			}
			final byte[] messageBytes = (clientParam + serverParam).getBytes();
			final boolean isSignatureValid = SqrlUtil.verifyED25519(signatureFromMessage, messageBytes, publicKey);
			if (!isSignatureValid) {
				throw new SqrlInvalidRequestException(
						SqrlClientRequestLoggingUtil.getLogHeader() + "Signature for " + keyName + " was invalid");
			}
		} catch (final SqrlInvalidRequestException e) {
			throw e;
		} catch (final Exception e) {
			throw new SqrlInvalidRequestException("Error computing signature for " + keyName, e);
		}
	}

	private Map<String, String> parseLinesToNameValueMap(final String decoded) throws SqrlInvalidRequestException {
		final Map<String, String> table = new TreeMap<>();
		final BufferedReader reader = new BufferedReader(new StringReader(decoded));
		try {
			String line = reader.readLine();
			while (line != null) {
				final String[] data = line.split("=");
				if (data.length != 2) {
					logger.info("Received empty param " + line);
				} else {
					table.put(data[0], data[1]);
				}
				line = reader.readLine();
			}
			return table;
		} catch (final Exception e) {
			throw new SqrlInvalidRequestException("Exception parsing decoded <" + decoded + ">", e);
		}
	}

	public SqrlRequestCommand getClientCommand() {
		return clientCommand;
	}

	public SqrlNutToken getNut() {
		return nut;
	}

	public Map<String, String> getKeysToBePersisted() {
		// Convert to type <String, String> to ease persistence call
		final Map<String, String> toBePersisted = new ConcurrentHashMap<>();
		for (final Map.Entry<SqrlServerSideKey, String> entry : requestKeyTableBase64.entrySet()) {
			if (entry.getKey().needsToBePersisted()) {
				toBePersisted.put(entry.getKey().toString(), entry.getValue());
			}
		}
		return toBePersisted;
	}

	public boolean hasKey(final SqrlServerSideKey key) {
		return requestKeyTableBase64.containsKey(key);
	}

	public String getKey(final SqrlServerSideKey key) {
		return requestKeyTableBase64.get(key);
	}

	public List<SqrlRequestOpt> getOptList() {
		return optList;
	}

	public String getNegotiatedSqrlProtocolVersion() {
		return negotiatedSqrlProtocolVersion.toString();
	}

	public String getCorrelator() {
		return correlator;
	}

	/**
	 * @return true if the request contained a valid urs signature
	 */
	public boolean containsUrs() {
		return SqrlUtil.isNotBlank(servletRequest.getParameter(SqrlSignatureType.urs.toString()));
	}
}
