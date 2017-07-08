package com.github.sqrlserverjava.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * SQRL constants
 *
 * @author Dave Badia
 *
 */
public class SqrlConstants {
	public static final Integer SQRL_VERSION_1 = Integer.valueOf(1);

	/**
	 * The SQRL versions which this library supports, stored in descending order. This is an ugly one liner, but we need
	 * to ensure it is sorted descending and unmodifiable
	 */
	// @formatter:off
	public static final Set<Integer> SUPPORTED_SQRL_VERSIONS = Collections.unmodifiableSet(
			new TreeSet<>(Arrays.asList(new Integer[] {
					SqrlConstants.SQRL_VERSION_1
			})).descendingSet());
	// @formatter:off

	public static final String	UTF8				= "UTF-8";
	public static final String	SCHEME_HTTP			= "http";
	public static final String	SCHEME_HTTP_COLON	= SCHEME_HTTP + ":";
	public static final String	SCHEME_HTTPS		= "https";
	public static final String	SCHEME_HTTPS_COLON	= SCHEME_HTTPS + ":";
	public static final String	SCHEME_QRL			= "qrl";
	public static final String	SCHEME_SQRL			= "sqrl";
	public static final String	FORWARD_SLASH		= "/";

	public static final CharSequence	FORWARD_SLASH_X2_LOCALHOST	= "//localhost";
	public static final CharSequence	FORWARD_SLASH_X2_127_0_0_1	= "//127.0.0.1";
	public static final int				AES_KEY_LENGTH				= 16;

	public static final String					TRANSIENT_NAME_SERVER_PARROT	= "lastServerParam";
	public static final String					TRANSIENT_CPS_NONCE				= "cpsNonce";
	public static final String					ERROR							= "error";

	public static final String	REGEX_BASE64_URL	= "[a-zA-Z0-9_-]+";
	/**
	 * Token being any request param, correlator, nut token, etc
	 */
	public static final int		MAX_SQRL_TOKEN_SIZE	= 30000;

	private SqrlConstants() {
		// Constants class
	}
}
