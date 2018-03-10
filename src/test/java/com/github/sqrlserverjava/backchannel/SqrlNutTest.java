package com.github.sqrlserverjava.backchannel;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlConfigOperations;
import com.github.sqrlserverjava.SqrlConfigOperationsFactory;
import com.github.sqrlserverjava.TestCaseUtil;

public class SqrlNutTest {
	private SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();

	@Before
	public void setup() throws Exception {
		config = TestCaseUtil.buildTestSqrlConfig();
	}

	@Test
	public void testNut_getters() throws Exception {
		config.setBackchannelServletPath("http://davetest.com/sqrl");
		final SqrlConfigOperations configOps = SqrlConfigOperationsFactory.get(config);
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
		assertEquals(22, nut.asBase64UrlEncryptedNut().length());
		assertEquals("QwJJFrvH1jBXakjOh_vVqg", nut.asBase64UrlEncryptedNut());
	}

	@Test
	public void testIt() {
		final byte[] bytes = SqrlNutTokenUtil.unpack(1431904222);
		System.out.println(Arrays.toString(bytes));
	}

	@Test
	public void testNut_parse() throws Exception {
		config.setBackchannelServletPath("http://davetest.com/sqrl");
		final SqrlConfigOperations configOps = SqrlConfigOperationsFactory.get(config);
		final SqrlNutToken nut = new SqrlNutToken(configOps, "QwJJFrvH1jBXakjOh_vVqg");
		assertEquals(4, nut.getInetInt());
		assertEquals(234, nut.getCounter());
		assertEquals(1461244576000L, nut.getIssuedTimestampMillis());
		assertEquals(6, nut.getRandomInt());
	}
}
