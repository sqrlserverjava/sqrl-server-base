package com.github.dbadia.sqrl.server.backchannel;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfigOperations;
import com.github.dbadia.sqrl.server.SqrlConstants;
import com.github.dbadia.sqrl.server.SqrlException;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlUtil;

/**
 * Parses a SQRL client request and validates all signatures
 * 
 * @author Dave Badia
 *
 */
public class SqrlRequest {
	private static final Logger logger = LoggerFactory.getLogger(SqrlRequest.class);

	private static final String NUT_EQUALS = "nut=";

	private final String sqrlProtocolVersion;
	private final SqrlNutToken nut;
	private final String clientCommand;
	private final Map<String, byte[]> clientKeys = new ConcurrentHashMap<>();
	private final Map<String, String> clientKeysBsse64 = new ConcurrentHashMap<>();
	private final List<SqrlClientOpt> optList = new ArrayList();

	private final HttpServletRequest servletRequest;
	private final String clientParam;
	private final String serverParam;
	private final String correlator;


	SqrlRequest(final HttpServletRequest servletRequest, final SqrlPersistence persistence,
			final SqrlConfigOperations configOps) throws SqrlException {
		this.servletRequest = servletRequest;
		clientParam = getRequiredParameter(servletRequest, "client");
		serverParam = getRequiredParameter(servletRequest, "server");
		nut = new SqrlNutToken(configOps, extractFromServerString(NUT_EQUALS));
		final String decoded = SqrlUtil.base64UrlDecodeToString(clientParam);
		// parse server - not a name value pair, just the query string we gave
		correlator = extractFromServerString(SqrlConstants.CLIENT_PARAM_CORRELATOR);

		// parse client
		final Map<String, String> clientNameValuePairTable = parseLinesToNameValueMap(decoded);
		sqrlProtocolVersion = clientNameValuePairTable.get(SqrlConstants.CLIENT_PARAM_VER);
		if (!"1".equals(sqrlProtocolVersion)) {
			throw new SqrlInvalidRequestException("Unsupported SQRL Client version " + sqrlProtocolVersion, null);
		}

		// parse opt
		final String optListString = clientNameValuePairTable.get(SqrlConstants.CLIENT_PARAM_OPT);
		if(SqrlUtil.isNotBlank(optListString)) {
			for (final String optString : optListString.split("~")) {
				final SqrlClientOpt clientOpt = SqrlClientOpt.valueOf(optString);
				if(clientOpt == null) {
					logger.error("Unknown SQRL client option {}", optString);
				} else {
					optList.add(clientOpt);
				}
			}
		}

		// parse keys
		for (final Map.Entry<String, String> entry : parseLinesToNameValueMap(decoded).entrySet()) {
			if (SqrlConstants.getAllKeyTypes().contains(entry.getKey())) {
				final byte[] keyBytes = SqrlUtil.base64UrlDecode(entry.getValue());
				clientKeys.put(entry.getKey(), keyBytes);
				clientKeysBsse64.put(entry.getKey(), entry.getValue());
			}
		}

		// Per the SQRL spec, since the server response is not signed, we must check the value that comes
		// back to ensure it wasn't tampered with
		final String expectedServerValue = persistence.fetchTransientAuthData(correlator,
				SqrlPersistence.TRANSIENT_NAME_SERVER_PARROT);
		if (!expectedServerValue.equals(serverParam)) {
			if (logger.isInfoEnabled()) {
				logger.info("Server parrot mismatch, possible tampering.  Expected={}, Received={}",
						expectedServerValue, serverParam);
			}
			throw new SqrlException("Server parrot mismatch, possible tampering");
		}

		// Validate the signatures
		boolean idsFound = false;
		for (final String aSignatureType : SqrlConstants.getAllSignatureTypes()) {
			final String signatureParamValue = servletRequest.getParameter(aSignatureType);
			if (SqrlUtil.isNotBlank(signatureParamValue)) {
				// Validate the signature
				validateSignature(SqrlConstants.getSignatureToKeyParamTable().get(aSignatureType), signatureParamValue);
				if (aSignatureType.equals(SqrlConstants.SIGNATURE_TYPE_IDS)) {
					idsFound = true;
				}
			}
		}

		// All requests must have the ids signature
		if (!idsFound) {
			throw new SqrlInvalidRequestException(
					"ids was missing in SQRL client request: " + clientNameValuePairTable);
		}

		clientCommand = clientNameValuePairTable.get(SqrlConstants.CLIENT_PARAM_CMD);
	}

	private void validateSignature(final String keyName, final String signatureParamValue) throws SqrlException {
		final byte[] signatureFromMessage = SqrlUtil.base64UrlDecode(signatureParamValue);

		try {
			final byte[] publicKey = clientKeys.get(keyName);
			if (publicKey == null) {
				throw new SqrlInvalidRequestException(
						SqrlLoggingUtil.getLogHeader() + keyName + " not found in client param: " + clientParam);
			}
			final byte[] messageBytes = (clientParam + serverParam).getBytes();
			final boolean isSignatureValid = SqrlUtil.verifyED25519(signatureFromMessage, messageBytes, publicKey);
			if (!isSignatureValid) {
				throw new SqrlInvalidRequestException(
						SqrlLoggingUtil.getLogHeader() + "Signature for " + keyName + " was invalid");
			}
		} catch (final SqrlException e) {
			throw e;
		} catch (final Exception e) {
			throw new SqrlException(SqrlLoggingUtil.getLogHeader() + "Error computing signature for " + keyName, e);
		}

	}

	String extractFromServerString(final String variableToFind) throws SqrlException {
		final String toSearch = SqrlUtil.base64UrlDecodeToString(serverParam);
		String toFind = variableToFind;
		if (!variableToFind.endsWith("=")) {
			toFind += "=";
		}
		// Find the nut param
		int index = toSearch.indexOf(toFind);
		if (index == -1) {
			throw new SqrlException("Could not find " + toFind + " in server parrot: " + toSearch);
		}
		String value = toSearch.substring(index + toFind.length());
		// Need to find the end of the nut string - could be & if from our login page URL or SqrlServerReply.SEPARATOR
		// if from a server reply
		index = value.indexOf(SqrlServerReply.SEPARATOR);
		if (index > -1) {
			value = value.substring(0, index);
		}
		index = value.indexOf('&');
		if (index > -1) {
			value = value.substring(0, index);
		}
		return value;
	}

	private String getRequiredParameter(final HttpServletRequest servletRequest, final String requiredParamName)
			throws SqrlInvalidRequestException {
		final String value = servletRequest.getParameter(requiredParamName);
		if (value == null || value.trim().length() == 0) {
			throw new SqrlInvalidRequestException(
					"Missing required parameter " + requiredParamName + ".  Request contained: "
							+ SqrlUtil.buildRequestParamList(servletRequest));
		}
		return value;
	}

	private Map<String, String> parseLinesToNameValueMap(final String decoded) throws SqrlException {
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
			throw new SqrlException("Exception parsing decoded <" + decoded + ">", e);
		}
	}

	String getClientCommand() {
		return clientCommand;
	}

	SqrlNutToken getNut() {
		return nut;
	}

	Map<String, String> getKeysToBeStored() {
		final Map<String, String> toBeStored = new ConcurrentHashMap<>(clientKeysBsse64);
		toBeStored.remove(SqrlConstants.SQRL_KEY_TYPE_IDENTITY);
		toBeStored.remove(SqrlConstants.KEY_TYPE_PREVIOUS_IDENTITY);
		return toBeStored;
	}

	String getIdk() {
		return clientKeysBsse64.get(SqrlConstants.SQRL_KEY_TYPE_IDENTITY);
	}

	boolean hasPidk() {
		return clientKeysBsse64.containsKey(SqrlConstants.KEY_TYPE_PREVIOUS_IDENTITY);
	}

	String getPidk() {
		return clientKeysBsse64.get(SqrlConstants.KEY_TYPE_PREVIOUS_IDENTITY);
	}

	List<SqrlClientOpt> getOptList() {
		return optList;
	}

	String getSqrlProtocolVersion() {
		return sqrlProtocolVersion;
	}

	public String getCorrelator() {
		return correlator;
	}

	/**
	 * @return true if the request contained a valid urs signature
	 */
	public boolean containsUrs() {
		return SqrlUtil.isNotBlank(servletRequest.getParameter(SqrlConstants.SQRL_SIGNATURE_URS));
	}
}
