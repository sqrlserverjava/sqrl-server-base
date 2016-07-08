package com.github.dbadia.sqrl.server.backchannel;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import com.github.dbadia.sqrl.server.SqrlConstants;
import com.github.dbadia.sqrl.server.SqrlException;

/**
 * Util methods to be used by other test cases
 * 
 * @author Dave Badia
 *
 */
public class SqrlTestHelper {
	private static final String SQRL_PARAM_EOL = "\r\n";

	protected static String buildClientParam(final String command, final String idk, final String... optArray) {
		final StringBuilder buf = new StringBuilder("ver=1").append(SQRL_PARAM_EOL);
		buf.append("cmd=").append(command).append(SQRL_PARAM_EOL);
		buf.append("idk=").append(idk).append(SQRL_PARAM_EOL);
		if (optArray.length > 0) {
			buf.append("opt=");
			for (final String opt : optArray) {
				buf.append(opt).append("~");
			}
			// Remove
			buf.replace(buf.length() -1, buf.length() , SQRL_PARAM_EOL);
		}
		// Using the real SQRL code in a test case helper is a bad practice
		return sqrlBase64UrlEncode(buf.toString());
	}

	/**
	 * Performs the SQRL required base64URL encoding
	 * 
	 * @param toEncode
	 *            the string to be encoded
	 * @return the encoded string
	 */
	protected static String sqrlBase64UrlEncode(final String toEncode) {
		try {
			return sqrlBase64UrlEncode(toEncode.getBytes(SqrlConstants.UTF8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException ", e);
		}
	}

	public static String sqrlBase64UrlEncode(final byte[] bytes) {
		try {
			String encoded = new String(Base64.getUrlEncoder().encode(bytes), SqrlConstants.UTF8);
			while (encoded.endsWith("=")) {
				encoded = encoded.substring(0, encoded.length() - 1);
			}
			return encoded;
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException during base64 encode", e);
		}
	}

	/**
	 * Performs the SQRL required base64URL decoding
	 * 
	 * @param toDecodeParam
	 *            the data to be decoded
	 * @return the decoded byte array
	 * @throws SqrlException
	 *             if an error occurs
	 */
	public static byte[] base64UrlDecode(final String toDecodeParam) throws SqrlException {
		try {
			return Base64.getUrlDecoder().decode(toDecodeParam.getBytes());
		} catch (final IllegalArgumentException e) {
			throw new SqrlException("Error base64 decoding: " + toDecodeParam, e);
		}
	}

}
