package com.github.dbadia.sqrl.server.backchannel;

import java.io.BufferedReader;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfigOperations;
import com.github.dbadia.sqrl.server.SqrlException;
import com.github.dbadia.sqrl.server.SqrlUtil;

/**
 * Encapsulates a request received from a SQRL client
 * 
 * @author Dave Badia
 *
 */
public class SqrlRequest {
	private static final Logger logger = LoggerFactory.getLogger(SqrlRequest.class);

	private static final String KEY_TYPE_IDENTITY = "idk";
	private static final String KEY_TYPE_SUC = "suk";
	private static final String KEY_TYPE_VUK = "vuk";
	private static final String KEY_TYPE_PREVIOUS_IDENTITY = "pidk";
	private static final List<String> ALL_KEY_TYPES = Arrays
			.asList(new String[] { KEY_TYPE_IDENTITY, KEY_TYPE_SUC, KEY_TYPE_VUK, KEY_TYPE_PREVIOUS_IDENTITY });

	private static final String CLIENT_PARAM_VER = "ver";
	private static final String CLIENT_PARAM_CMD = "cmd";
	private static final String CLIENT_PARAM_OPT = "opt";

	private final static List<String> KEY_IDS = new ArrayList<>();
	private static final String NUT_EQUALS = "nut=";

	private String clientVersion;
	private final SqrlNutToken nut;
	private final String clientCommand;
	private final Map<String, byte[]> clientKeys = new ConcurrentHashMap<>();
	private final Map<String, String> clientKeysBsse64 = new ConcurrentHashMap<>();
	private final List<SqrlClientOpt> optList = new ArrayList();
	/**
	 * The decoded server param, which contains the query string we put in the QR code
	 */
	private final String serverParrot;

	SqrlRequest(final HttpServletRequest servletRequest, final SqrlConfigOperations configOps)
			throws SqrlException {
		final String clientParam = getRequiredParameter(servletRequest, "client");
		final String serverParam = getRequiredParameter(servletRequest, "server");
		final String decoded = SqrlUtil.base64UrlDecodeToString(clientParam);

		// parse client
		final Map<String, String> clientNameValuePairTable = parseLinesToNameValueMap(decoded);
		final String version = clientNameValuePairTable.get(CLIENT_PARAM_VER);
		if(!"1".equals(version)) {
			throw new SqrlException("Unsupported SQRL Client version " + version, null);
		}

		// parse opt
		final String optListString = clientNameValuePairTable.get(CLIENT_PARAM_OPT);
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
			if (ALL_KEY_TYPES.contains(entry.getKey())) {
				final byte[] keyBytes = SqrlUtil.base64UrlDecode(entry.getValue());
				clientKeys.put(entry.getKey(), keyBytes);
				clientKeysBsse64.put(entry.getKey(), entry.getValue());
			}
		}

		// parse server - not a name value pair, just the query string we gave
		serverParrot = SqrlUtil.base64UrlDecodeToString(serverParam);
		nut = new SqrlNutToken(configOps, extractFromServerString(NUT_EQUALS));
		// TODO: compare serverParrot somewhere / somehow

		final String idsParam = servletRequest.getParameter("ids");
		final byte[] signatureFromMessage = SqrlUtil.base64UrlDecode(idsParam);

		// Validate the signatures
		// TODO: check other signatures
		try {
			final byte[] publicKey = clientKeys.get(KEY_TYPE_IDENTITY);
			if (publicKey == null) {
				throw new GeneralSecurityException(KEY_TYPE_IDENTITY + " not found in client param: " + clientParam);
			}
			final byte[] messageBytes = (clientParam + serverParam).getBytes();
			final boolean isSignatureValid = SqrlUtil.verifyED25519(signatureFromMessage, messageBytes, publicKey);
			if (!isSignatureValid) {
				throw new SqrlException("Signature invalid, mismatch", null);
			}
		} catch (final SqrlException e) {
			throw e;
		} catch (final Exception e) {
			throw new SqrlException("Error computing signature", e);
		}

		clientCommand = clientNameValuePairTable.get(CLIENT_PARAM_CMD);
	}

	String extractFromServerString(final String variableToFind) throws SqrlException {
		String toFind = variableToFind;
		if (!variableToFind.endsWith("=")) {
			toFind += "=";
		}
		// Find the nut param
		int index = serverParrot.indexOf(toFind);
		if (index == -1) {
			throw new SqrlException("Could not find " + toFind + " in server parrot: " + serverParrot);
		}
		String value = serverParrot.substring(index + toFind.length());
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

	public String getClientCommand() {
		return clientCommand;
	}

	public SqrlNutToken getNut() {
		return nut;
	}

	public Map<String, String> getKeysToBeStored() {
		final Map<String, String> toBeStored = new ConcurrentHashMap<>(clientKeysBsse64);
		toBeStored.remove(KEY_TYPE_IDENTITY);
		toBeStored.remove(KEY_TYPE_PREVIOUS_IDENTITY);
		return toBeStored;
	}

	public String getIdk() {
		return clientKeysBsse64.get(KEY_TYPE_IDENTITY);
	}

	public boolean hasPidk() {
		return clientKeysBsse64.containsKey(KEY_TYPE_PREVIOUS_IDENTITY);
	}

	public String getPidk() {
		return clientKeysBsse64.get(KEY_TYPE_PREVIOUS_IDENTITY);
	}

	public List<SqrlClientOpt> getOptList() {
		return optList;
	}
}
