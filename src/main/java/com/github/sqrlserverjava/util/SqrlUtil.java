package com.github.sqrlserverjava.util;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.exception.SqrlInvalidRequestException;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

/**
 * Various utility methods used by the rest of the SQRL code, including base64URL
 *
 * @author Dave Badia
 *
 */
public class SqrlUtil {
	private static final Logger					logger				= LoggerFactory.getLogger(SqrlUtil.class);
	private static final Map<String, String>	cookieDomainCache	= new ConcurrentHashMap<>();
	static final Pattern				REGEX_PATTERN_REGEX_BASE64_URL	= Pattern
			.compile(SqrlConstants.REGEX_BASE64_URL);

	private SqrlUtil() {
		// Util class
	}

	/**
	 * Performs the SQRL required base64URL encoding
	 *
	 * @param bytes
	 *            the data to be encoded
	 * @return the encoded string
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
	 * Performs the SQRL required base64URL encoding
	 *
	 * @param toEncode
	 *            the string to be encoded
	 * @return the encoded string
	 */
	public static String sqrlBase64UrlEncode(final String toEncode) {
		try {
			return sqrlBase64UrlEncode(toEncode.getBytes(SqrlConstants.UTF8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException for " + SqrlConstants.UTF8, e);
		}
	}

	/**
	 * Performs the SQRL required base64URL decoding
	 *
	 * @param toDecodeParam
	 *            the data to be decoded
	 * @return the decoded byte array
	 * @throws SqrlException
	 *             if an error occurs during the base64 decode
	 */
	public static byte[] base64UrlDecode(final String toDecodeParam) throws SqrlException {
		try {
			return Base64.getUrlDecoder().decode(toDecodeParam.getBytes());
		} catch (final IllegalArgumentException e) {
			throw new SqrlException("Error base64 decoding: " + toDecodeParam, e);
		}
	}

	/**
	 * Convenience method which performs the SQRL required base64URL decoding but throws SqrlInvalidRequestException
	 * since the assumption is that the data originated from a SQRL client
	 *
	 * @param toDecodeParam
	 *            the data to be decoded
	 * @return the decoded byte array
	 * @throws SqrlInvalidRequestException
	 *             if an error occurs during the base64 decode
	 */
	public static byte[] base64UrlDecodeDataFromSqrlClient(final String toDecodeParam) throws SqrlInvalidRequestException {
		try {
			return base64UrlDecode(toDecodeParam);
		} catch (final SqrlException e) {
			throw new SqrlInvalidRequestException(e.getMessage(), e.getCause());
		}
	}

	/**
	 * Performs the SQRL required base64URL decoding
	 *
	 * @param toDecodeParam
	 *            the data to be decoded
	 * @return the decoded data as a string using the UTF-8 character set
	 * @throws SqrlException
	 *             if UTF8 is not supported
	 */
	public static String base64UrlDecodeToString(final String toDecode) throws SqrlException {
		try {
			return new String(base64UrlDecode(toDecode), SqrlConstants.UTF8);
		} catch (final UnsupportedEncodingException e) {
			// This should never happen as the java specification requires that all JVMs support UTF8
			throw new IllegalStateException("UnsupportedEncodingException for " + SqrlConstants.UTF8, e);
		}
	}

	/**
	 * Convenience method which performs the SQRL required base64URL decoding but throws SqrlInvalidRequestException
	 * since the assumption is that the data originated from a SQRL client
	 *
	 * @param toDecodeParam
	 *            the data to be decoded
	 * @return the decoded data as a string using the UTF-8 character set
	 * @throws SqrlInvalidRequestException
	 *             if the data was not in base64url format
	 * @throws IllegalStateException
	 *             if UTF8 is not supported
	 */
	public static String base64UrlDecodeDataFromSqrlClientToString(final String toDecode)
			throws SqrlInvalidRequestException {
		try {
			return base64UrlDecodeToString(toDecode);
		} catch (final SqrlException e) {
			throw new SqrlInvalidRequestException(e.getMessage(), e.getCause());
		}
	}



	public static String base64UrlDecodeToStringOrErrorMessage(final String toDecode) {
		try {
			return base64UrlDecodeToString(toDecode);
		} catch (final Exception e) {
			logger.error("Error during url decode, returning error string for " + toDecode, e);
			return "<error during base64url decode>";
		}
	}

	/**
	 * Internal use only. Verifies the ED25519 signature
	 *
	 * @param signatureFromMessage
	 *            the signature data
	 * @param messageBytes
	 *            the message that was signed
	 * @param publicKeyBytes
	 *            the public key to be used for verification
	 * @return true if verification was successful
	 * @throws SqrlException
	 *             if an error occurs during ED25519 operations
	 */
	public static boolean verifyED25519(final byte[] signatureFromMessage, final byte[] messageBytes,
			final byte[] publicKeyBytes) throws SqrlException {
		try {
			final Signature signature = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
			final EdDSAParameterSpec edDsaSpec = EdDSANamedCurveTable
					.getByName(EdDSANamedCurveTable.CURVE_ED25519_SHA512);

			final PublicKey publicKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(publicKeyBytes, edDsaSpec));
			signature.initVerify(publicKey);

			signature.update(messageBytes);
			return signature.verify(signatureFromMessage);
		} catch (final GeneralSecurityException e) {
			throw new SqrlException("Got exception during EC signature verification", e);
		}
	}

	/**
	 * Provides the functionality of Apache commons StringUtils.isBlank() without bringing in the dependency
	 *
	 * @param string
	 *            the string to check
	 * @return true if blank, false otherwise
	 */
	public static boolean isBlank(final String string) {
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Provides the functionality of Apache commons StringUtils.isNotBlank() without bringing in the dependency
	 *
	 * @param string
	 *            the string to check
	 * @return true if not blank, false otherwise
	 */
	public static boolean isNotBlank(final String string) {
		return !isBlank(string);
	}

	/**
	 * Internal use only.
	 *
	 * @param ipAddressString
	 *            the ip address to parse
	 * @return the IP address
	 * @throws SqrlException
	 *             if an {@link UnknownHostException} occurs
	 */
	public static InetAddress ipStringToInetAddresss(final String ipAddressString) throws SqrlException {
		if (SqrlUtil.isBlank(ipAddressString)) {
			throw new SqrlException("ipAddressString was null or empty");
		}
		try {
			return InetAddress.getByName(ipAddressString);
		} catch (final UnknownHostException e) {
			throw new SqrlException("Got UnknownHostException for <" + ipAddressString + ">", e);
		}
	}

	/**
	 * Internal use only. Computes the SQRL server friendly name (SFN) from the servers URl. Typically used if a SFN is
	 * not specified in the config
	 *
	 * @param request
	 * @return
	 */
	public static String computeSfnFromUrl(final HttpServletRequest request) {
		return request.getServerName();
	}

	/**
	 * Internal use only. Builds a string of name value pairs from the request
	 *
	 * @param servletRequest
	 *            the request
	 * @return a string of the name value pairs that were in the request
	 */
	public static String buildRequestParamList(final HttpServletRequest servletRequest) {
		final Enumeration<String> params = servletRequest.getParameterNames();
		final StringBuilder buf = new StringBuilder(500);
		while (params.hasMoreElements()) {
			final String paramName = params.nextElement();
			buf.append(paramName).append("=").append(servletRequest.getParameter(paramName)).append("  ");
		}
		return buf.toString();
	}

	public static Cookie createOrUpdateCookie(final HttpServletRequest request, final String cookieDomain,
			final String name, final String value, final int maxAgeInSeconds, final SqrlConfig config) {
		Cookie cookie = findCookie(request, name);
		if (cookie == null) {
			cookie = new Cookie(name, value);
		}
		cookie.setValue(value);
		cookie.setMaxAge(maxAgeInSeconds);
		applySettingsToCookie(cookie, cookieDomain, request, config);
		return cookie;
	}

	private static void applySettingsToCookie(final Cookie cookie, final String cookieDomain,
			final HttpServletRequest request, final SqrlConfig config) {
		if (cookieDomain != null) {
			cookie.setDomain(cookieDomain);
		}
		cookie.setPath(config.getCookiePath());
		cookie.setHttpOnly(true);
		if (request.getScheme().equals(SqrlConstants.SCHEME_HTTPS)) {
			cookie.setSecure(true);
		}
	}

	private static Cookie findCookie(final HttpServletRequest request, final String toFind) {
		if (request.getCookies() != null) {
			for (final Cookie cookie : request.getCookies()) {
				if (toFind.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}

	public static String findCookieValue(final HttpServletRequest request, final String toFind) {
		final Cookie cookie = findCookie(request, toFind);
		if (cookie == null) {
			return null;
		}
		return cookie.getValue();
	}

	public static void deleteCookies(final HttpServletRequest request, final HttpServletResponse response,
			final SqrlConfig sqrlConfig, final String... cookiesToDelete) {
		final List<String> cookieToDeleteList = Arrays.asList(cookiesToDelete);
		if (request.getCookies() == null) {
			return;
		}
		for (final Cookie cookie : request.getCookies()) {
			if (cookieToDeleteList.contains(cookie.getName())) {
				cookie.setMaxAge(0);
				cookie.setValue("");
				final String cookieDomain = SqrlUtil.computeCookieDomain(request, sqrlConfig);
				applySettingsToCookie(cookie, cookieDomain, request, sqrlConfig);
				response.addCookie(cookie);
			}
		}
	}

	public static final String cookiesToString(final Cookie[] cookieArray) {
		final StringBuilder buf = new StringBuilder(300);
		buf.append("[ ");
		if (cookieArray != null) {
			for (final Cookie cookie : cookieArray) {
				buf.append(cookie.getName()).append("=").append(cookie.getValue()).append(", ");
			}
		}
		return buf.substring(0, buf.length() - 2) + " ]";
	}

	public static String computeCookieDomain(final HttpServletRequest request, final SqrlConfig config) {
		String domain = config.getCookieDomain();
		if (domain == null) {
			final String requestUrl = request.getRequestURL().toString();
			domain = cookieDomainCache.get(requestUrl);
			if (domain == null) {
				// compute the value and store in the cache
				domain = requestUrl.substring(requestUrl.indexOf("//") + 2);
				final int index = domain.indexOf('/');
				if (index > 0) {
					domain = domain.substring(0, index);
				}
				final int portIndex = domain.indexOf(':');
				if (portIndex > -1) {
					domain = domain.substring(0, portIndex);
				}
				cookieDomainCache.put(requestUrl, domain);
			}
			if ("localhost".equals(domain)) {
				return null;
			}
		}
		return domain;
	}

	public static String logEnterServlet(final HttpServletRequest request) {
		if (!logger.isInfoEnabled()) {
			return "";
		}
		final StringBuilder buf = new StringBuilder(300);
		buf.append("In ");
		buf.append(request.getRequestURI()).append(" with params: ");
		buf.append(parameterMapToString(request.getParameterMap())).append(" and  cookies: ");
		buf.append(cookiesToString(request.getCookies()));
		return buf.toString();
	}

	public static String parameterMapToString(final Map<String, String[]> parameterMap) {
		final StringBuilder buf = new StringBuilder(400);
		buf.append("{");
		// {c=12850, 38.6=386540,
		for (final Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			buf.append(entry.getKey()).append(":");
			for (final String aValue : entry.getValue()) {
				buf.append(aValue).append(",");
			}
			buf.append(" ");
		}
		// remove the trailing comma
		if (buf.toString().endsWith(", ")) {
			buf.delete(buf.length() - 2, buf.length());
		}
		buf.append("}");
		return buf.toString();
	}

	public static String buildLogMessageForSqrlClientRequest(final HttpServletRequest request) {
		final StringBuilder buf = new StringBuilder(500);
		buf.append(SqrlClientRequestLoggingUtil.getLogHeader()).append("full params from  client: ");
		for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			buf.append(entry.getKey()).append("=").append(Arrays.toString(entry.getValue())).append("   ");
		}
		return buf.toString();
	}

	/**
	 * Convenience method which concatenates the given strings in an efficient manner using a {@link StringBuilder} or
	 * similar object
	 * 
	 */
	public static String buildString(final String... stringArray) {
		int length = 0;
		for(final String aString : stringArray) {
			length += aString.length();
		}
		final StringBuilder buf = new StringBuilder(length + 1);
		for(final String aString : stringArray) {
			buf.append(aString);
		}
		return buf.toString();
	}
}