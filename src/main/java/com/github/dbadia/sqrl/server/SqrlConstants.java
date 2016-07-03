package com.github.dbadia.sqrl.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQRL constants
 * 
 * @author Dave Badia
 *
 */
public class SqrlConstants {
	public static final String UTF8 = "UTF-8";
	public static final String SCHEME_HTTP = "http";
	public static final String SCHEME_HTTP_COLON = SCHEME_HTTP + ":";
	public static final String SCHEME_HTTPS = "https";
	public static final String SCHEME_HTTPS_COLON = SCHEME_HTTPS + ":";
	public static final String SCHEME_QRL = "qrl";
	public static final String SCHEME_SQRL = "sqrl";
	public static final String FORWARD_SLASH = "/";

	public static final CharSequence FORWARD_SLASH_X2_LOCALHOST = "//localhost";
	@SuppressWarnings("CERT:MSC03-J") 
	public static final CharSequence FORWARD_SLASH_X2_127_0_0_1 = "//127.0.0.1";
	public static final int AES_KEY_LENGTH = 16;
	public static final String CLIENT_PARAM_CORRELATOR = "cor";

	public static final String SQRL_SIGNATURE_URS = "urs";

	public static final String SQRL_KEY_TYPE_IDENTITY = "idk";
	public static final String KEY_TYPE_SUC = "suk";
	public static final String KEY_TYPE_VUK = "vuk";
	public static final String KEY_TYPE_PREVIOUS_IDENTITY = "pidk";

	public static final String SIGNATURE_TYPE_IDS = "ids";
	public static final String SIGNATURE_TYPE_URS = "urs";

	public static final String CLIENT_PARAM_VER = "ver";
	public static final String CLIENT_PARAM_CMD = "cmd";
	public static final String CLIENT_PARAM_OPT = "opt";


	private static final List<String> ALL_KEY_TYPES = new ArrayList<>();
	private static final List<String> ALL_SIGNATURE_TYPES = new ArrayList<>();
	private static final Map<String, String> SIGNATURE_TO_KEY_PARAM_TABLE = new ConcurrentHashMap<>();
	public static final String TRANSIENT_NAME_SERVER_PARROT = "lastServerParam";

	private SqrlConstants() {
		// Constants class
	}

	static {
		SqrlConstants.SIGNATURE_TO_KEY_PARAM_TABLE.put(SIGNATURE_TYPE_IDS, SqrlConstants.SQRL_KEY_TYPE_IDENTITY);
		SqrlConstants.SIGNATURE_TO_KEY_PARAM_TABLE.put(SIGNATURE_TYPE_URS, SqrlConstants.KEY_TYPE_SUC);
		SqrlConstants.SIGNATURE_TO_KEY_PARAM_TABLE.put("pids", SqrlConstants.KEY_TYPE_PREVIOUS_IDENTITY);
		SqrlConstants.ALL_KEY_TYPES.addAll(SqrlConstants.SIGNATURE_TO_KEY_PARAM_TABLE.values());
		SqrlConstants.ALL_SIGNATURE_TYPES.addAll(SqrlConstants.SIGNATURE_TO_KEY_PARAM_TABLE.keySet());
	}

	public static final List<String> getAllKeyTypes() {
		return new ArrayList<>(ALL_KEY_TYPES);
	}

	public static final List<String> getAllSignatureTypes() {
		return new ArrayList<>(ALL_SIGNATURE_TYPES);
	}

	public static final Map<String, String> getSignatureToKeyParamTable() {
		return new ConcurrentHashMap<>(SIGNATURE_TO_KEY_PARAM_TABLE);
	}
}
