package com.github.dbadia.sqrl.server;

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
	public static final CharSequence FORWARD_SLASH_X2_127_0_0_1 = "//127.0.0.1"; // NOSONAR
	public static final int AES_KEY_LENGTH = 16;
	public static final String CLIENT_PARAM_CORRELATOR = "cor";

	private SqrlConstants() {
		// Constants class
	}
}
