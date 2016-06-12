package com.grc.sqrl.server;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;

import org.junit.Test;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;

public class ED25519Test {
	static final String HEXES = "0123456789abcdef";

	private final byte[] privateKey = new byte[32];
	private final byte[] publicKey = hexStringToByteArray(
			"3b6a27bcceb6a42d62a3a8d02a6f0d73653215771de243a63ac048a18b59da29");

	@Test
	public void testSign() throws Exception {
		final byte[] message = "This is a secret message".getBytes(Charset.forName("UTF-8"));
		final String expectedSignatureHex = "94825896c7075c31bcb81f06dba2bdcd9dcf16e79288d4b9f87c248215c8468d475f429f3de3b4a2cf67fe17077ae19686020364d6d4fa7a0174bab4a123ba0f";

		// Signature sgr = Signature.getInstance("EdDSA", "I2P");
		final Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
		final EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.CURVE_ED25519_SHA512);

		final PrivateKey sKey = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(privateKey, spec));
		sgr.initSign(sKey);
		sgr.update(message);
		final byte[] signature = sgr.sign();

		assertEquals(expectedSignatureHex, getHex(signature));
	}

	@Test
	public void testVerifySignature() throws Exception {
		final byte[] message = "This is a secret message".getBytes(Charset.forName("UTF-8"));
		final byte[] signatureToCompare = hexStringToByteArray(
				"94825896c7075c31bcb81f06dba2bdcd9dcf16e79288d4b9f87c248215c8468d475f429f3de3b4a2cf67fe17077ae19686020364d6d4fa7a0174bab4a123ba0f");

		assertTrue(SqrlUtil.verifyED25519(signatureToCompare, message, publicKey));
	}

	public static String getHex( final byte [] raw ) {
		if ( raw == null ) {
			return null;
		}
		final StringBuilder hex = new StringBuilder( 2 * raw.length );
		for ( final byte b : raw ) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4))
			.append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}

	public static byte[] hexStringToByteArray(final String s) {
		final int len = s.length();
		final byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}

