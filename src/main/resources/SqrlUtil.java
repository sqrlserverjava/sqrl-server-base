package com.dbadia.sqrl.server;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

public class SqrlUtil {
	private static final String UTF8 = "UTF-8";

	private SqrlUtil() {
	}

	/**
	 * Performs the SQRL version of base64 encoding (remove = as needed)
	 */
	public static String sqBase64Encode(final byte[] bytes) {
		try {
			String base64Encoded = new String(Base64.getEncoder().encode(bytes), UTF8);
			// remove the == (not sure what the == is about but we'll do it)
			while (base64Encoded.endsWith("=")) {
				base64Encoded = base64Encoded.substring(0, base64Encoded.length() - 1);
			}
			return base64Encoded;
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException during base64 encode", e);
		}
	}

	/**
	 * Performs the SQRL version of base64 encoding (remove = as needed)
	 */
	public static String sqBase64Encode(final String toEncode) {
		try {
			return sqBase64Encode(toEncode.getBytes(UTF8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException for " + UTF8, e);
		}
	}


	/**
	 * Performs the SQRL version of base64 decoding (adds = as needed)
	 * 
	 * @param toDecode
	 * @return
	 * @throws SqrlException
	 */
	public static byte[] sqBase64Decode(final String toDecodeParam) throws SqrlException {
		String toDecode = toDecodeParam;
		while (toDecode.length() % 4 != 0) {
			toDecode += "=";
		}
		try {
			return Base64.getDecoder().decode(toDecode.getBytes());
		} catch (final IllegalArgumentException e) {
			throw new SqrlException("Error base64 decoding: " + toDecode, e);
		}
	}

	/**
	 * Performs the SQRL version of base64 decoding (adds = as needed)
	 * 
	 * @param toDecode
	 * @return
	 * @throws SqrlException
	 */
	public static String sqBase64DecodeToString(final String toDecode) throws SqrlException {
		return new String(sqBase64Decode(toDecode));
	}

	/**
	 * @deprecated use {@link #sqBase64DecodeToString(String)}
	 */
	@Deprecated
	public static String base64DecodeToString(final String toDecode) {
		final byte[] bytes = base64Decode(toDecode);
		return new String(bytes);
	}

	/**
	 * @deprecated use {@link #sqBase64Decode(String)}
	 */
	@Deprecated
	public static byte[] base64Decode(final String toDecode) {
		return Base64.getDecoder().decode(toDecode.getBytes());
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
}
