package com.grc.sqrl.server.backchannel;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.grc.sqrl.server.SqrlConfig;
import com.grc.sqrl.server.SqrlConfigOperations;
import com.grc.sqrl.server.SqrlUtil;
import com.grc.sqrl.server.TCUtil;
import com.grc.sqrl.server.backchannel.SqrlNutToken;


public class NutTest {
	private SqrlConfig config = TCUtil.buildValidSqrlConfig();

	@Before
	public void setup() throws Exception {
		config = TCUtil.buildValidSqrlConfig();
	}

	@Test
	public void testNut_getters() throws Exception {
		config.setBackchannelServletPath("http://davetest.com/sqrl");
		final SqrlConfigOperations configOps = new SqrlConfigOperations(config);
		final long timestamp = 1461244576746L;
		final int inetInt = 4;
		final int counter = 234;
		final int random = 6;
		final SqrlNutToken nut = new SqrlNutToken(inetInt, configOps, counter, timestamp, random);
		assertEquals(inetInt, nut.getInetInt());
		assertEquals(counter, nut.getCounter());
		// Note that getTimestamp will remove millis since the Nut only has second granularity
		assertEquals(1461244576000L, nut.getIssuedTimestamp());
		assertEquals(random, nut.getRandomInt());
		assertEquals(counter, nut.getCounter());
		assertEquals(22, nut.asSqBase64EncryptedNut().length());
		assertEquals("QwJJFrvH1jBXakjOh_vVqg", nut.asSqBase64EncryptedNut());
	}

	@Test
	public void testNut_parse() throws Exception {
		config.setBackchannelServletPath("http://davetest.com/sqrl");
		final SqrlConfigOperations configOps = new SqrlConfigOperations(config);
		final SqrlNutToken nut = new SqrlNutToken(configOps, "QwJJFrvH1jBXakjOh_vVqg");
		assertEquals(4, nut.getInetInt());
		assertEquals(234, nut.getCounter());
		assertEquals(1461244576000L, nut.getIssuedTimestamp());
		assertEquals(6, nut.getRandomInt());
	}

	@Test
	public void testNut_parse2() throws Exception {
		final byte[] nutBytes = SqrlUtil.base64Decode("mHrG30UxMg6iAGcLILSXOg==");

		// TODO: need encrypted
		// final Nut nut = new Nut(nutBytes);
		// InetAddress fromNut = InetAddress.getByAddress(Nut.unpack(nut.getInetInt()));
		// System.out.println(fromNut);
		//
		// String addressStr = InetAddresses.fromInteger(nut.getInetInt()).toString();
		// System.out.println(addressStr);
		//
		// System.out.println(new Date(nut.getTimestamp() * 1000));
	}
}
