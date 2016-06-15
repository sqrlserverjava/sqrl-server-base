package com.github.dbadia.sqrl.server;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

/**
 * Various utility methods used by the rest of the SQRL code, including the SQRL base64url derivitive
 * 
 * @author Dave Badia
 *
 */
public class SqrlUtil {
	private static final Logger logger = LoggerFactory.getLogger(SqrlUtil.class);

	private static final ThreadLocal<String> threadLocalLogHeader = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "";
		}
	};

	private SqrlUtil() {
	}

	/**
	 * base64url encoding with the = characters removed per the SQRL spec
	 */
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
	 * base64url encoding with the = characters removed per the SQRL spec
	 * 
	 */
	public static String sqrlBase64UrlEncode(final String toEncode) {
		try {
			return sqrlBase64UrlEncode(toEncode.getBytes(SqrlConstants.UTF8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException ", e);
		}
	}

	public static byte[] base64UrlDecode(final String toDecodeParam) throws SqrlException {
		try {
			return Base64.getUrlDecoder().decode(toDecodeParam.getBytes());
		} catch (final IllegalArgumentException e) {
			throw new SqrlException("Error base64 decoding: " + toDecodeParam, e);
		}
	}

	public static String base64UrlDecodeToString(final String toDecode) throws SqrlException {
		return new String(base64UrlDecode(toDecode));
	}

	/**
	 * @deprecated use {@link #base64UrlDecode(String)}
	 */
	@Deprecated
	public static byte[] base64Decode(final String toDecode) {
		return Base64.getDecoder().decode(toDecode.getBytes());
	}

	/**
	 * Note, the SQRL protocol requires {@link SqrlUtil#sqrlBase64UrlEncode(byte[])
	 */
	public static String base64Encode(final byte[] toEncode) {
		return new String(Base64.getEncoder().encode(toEncode));
	}

	public static boolean verifyED25519(final byte[] signatureFromMessage, final byte[] messageBytes,
			final byte[] publicKey) throws SqrlException {
		try {
			final Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
			final EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.CURVE_ED25519_SHA512);

			final PublicKey vKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(publicKey, spec));
			sgr.initVerify(vKey);

			sgr.update(messageBytes);
			final boolean result = sgr.verify(signatureFromMessage);
			return result;
		} catch (final GeneralSecurityException e) {
			throw new SqrlException("Got exception during EC sig verify", e);
		}
	}

	/**
	 * Provides the functionality of Apache commons StringUtils.isBlank() without bringing in the dependency
	 */
	public static boolean isBlank(final String string) {
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Provides the functionality of Apache commons StringUtils.isNotBlank() without bringing in the dependency
	 */
	public static boolean isNotBlank(final String string) {
		return !isBlank(string);
	}

	public static InetAddress ipStringToInetAddresss(final String ipAddressString) throws SqrlException {
		if (SqrlUtil.isBlank(ipAddressString)) {
			throw new SqrlException("ipAddressString was null or empty");
		}
		try {
			return InetAddress.getByName(ipAddressString);
		} catch (final UnknownHostException e) {
			throw new SqrlException("Got UnknownHostException for <" + ipAddressString + ">");
		}
	}

	public static String computeSfnFromUrl(final HttpServletRequest request) {
		return request.getServerName();
	}

	public static String buildRequestParamList(final HttpServletRequest servletRequest) {
		final Enumeration<String> params = servletRequest.getParameterNames();
		final StringBuilder buf = new StringBuilder();
		while (params.hasMoreElements()) {
			final String paramName = params.nextElement();
			buf.append(paramName).append("=").append(servletRequest.getParameter(paramName)).append("  ");
		}
		return buf.toString();
	}

	public static void initLoggingHeader(final HttpServletRequest servletRequest) {
		final String sqrlAgentString = servletRequest.getHeader("user-agent");
		logger.info("sqrlagent={}", sqrlAgentString);
		threadLocalLogHeader.set(sqrlAgentString);
	}

	public static String updateLogHeader(final String logHeader) {
		threadLocalLogHeader.set(threadLocalLogHeader.get() + " " + logHeader);
		return logHeader;
	}

	public static void clearLogHeader() {
		threadLocalLogHeader.remove();
	}

	public static String getLogHeader() {
		return threadLocalLogHeader.get();
	}
}
