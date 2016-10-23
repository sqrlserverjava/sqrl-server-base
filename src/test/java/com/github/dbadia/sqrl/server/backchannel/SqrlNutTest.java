package com.github.dbadia.sqrl.server.backchannel;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlConfigOperations;
import com.github.dbadia.sqrl.server.TCUtil;

public class SqrlNutTest {
	private SqrlConfig config = TCUtil.buildTestSqrlConfig();

	@Before
	public void setup() throws Exception {
		config = TCUtil.buildTestSqrlConfig();
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
		assertEquals(1461244576000L, nut.getIssuedTimestampMillis());
		assertEquals(random, nut.getRandomInt());
		assertEquals(counter, nut.getCounter());
		assertEquals(22, nut.asSqrlBase64EncryptedNut().length());
		assertEquals("QwJJFrvH1jBXakjOh_vVqg", nut.asSqrlBase64EncryptedNut());
	}

	@Test
	public void testIt() {
		final byte[] bytes = SqrlNutTokenUtil.unpack(1431904222);
		System.out.println(Arrays.toString(bytes));
	}

	@Test
	public void testNut_parse() throws Exception {
		config.setBackchannelServletPath("http://davetest.com/sqrl");
		final SqrlConfigOperations configOps = new SqrlConfigOperations(config);
		final SqrlNutToken nut = new SqrlNutToken(configOps, "QwJJFrvH1jBXakjOh_vVqg");
		assertEquals(4, nut.getInetInt());
		assertEquals(234, nut.getCounter());
		assertEquals(1461244576000L, nut.getIssuedTimestampMillis());
		assertEquals(6, nut.getRandomInt());
	}
}
