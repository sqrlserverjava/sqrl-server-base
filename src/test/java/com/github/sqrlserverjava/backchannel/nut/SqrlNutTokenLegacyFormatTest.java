package com.github.sqrlserverjava.backchannel.nut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlConfigOperations;
import com.github.sqrlserverjava.SqrlConfigOperationsFactory;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.exception.SqrlException;

import junitx.framework.ArrayAssert;

public class SqrlNutTokenLegacyFormatTest {
	private SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();

	@Before
	public void setup() throws Exception {
		config = TestCaseUtil.buildTestSqrlConfig();
	}

	@Test
	@Ignore
	public void testNut_parse() throws Exception {
		config.setBackchannelServletPath("http://davetest.com/sqrl");
		final SqrlConfigOperations configOps = SqrlConfigOperationsFactory.get(config);
		final SqrlNutTokenSingleBlockFormat nut = new SqrlNutTokenSingleBlockFormat(configOps, "Jxf-R1rq5bjAnbnHrYszww");
		assertEquals(-1408237112, nut.getInetInt());
		assertEquals(1521722339000L, nut.getIssuedTimestampMillis());
		assertEquals(6, nut.getRandomInt());
	}


	@Test
	public void testPack() throws Throwable {
		final byte[] bytes = { 27, -78, -123, 54 };
		final int actual = SqrlNutTokenSingleBlockFormat.pack(bytes);
		assertEquals(464684342, actual);
	}

	@Test
	public void testUnpack() throws Throwable {
		final byte[] actual = SqrlNutTokenSingleBlockFormat.unpack(464684342);
		ArrayAssert.assertEquals(new byte[] { 27, -78, -123, 54 }, actual);
	}

	/* ************ Nut expiry tests *********************/
	@Test
	@Ignore
	public void testNutExpiry() throws Exception {
		final int nutValidityInSeconds = 1000;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final long nutIssuedTime = System.currentTimeMillis();
		final SqrlNutTokenSingleBlockFormat nut = TestCaseSqrlNutHelper.buildValidSqrlNutTokenLegacyFormat(nutIssuedTime,
				config);
		final long expiresAt = nut.computeExpiresAt(config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
	}

	@Test
	@Ignore
	public void testNutExpiry2() throws Exception {
		final int nutValidityInSeconds = 180;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final long nutIssuedTime = System.currentTimeMillis();
		final SqrlNutTokenSingleBlockFormat nut = TestCaseSqrlNutHelper.buildValidSqrlNutTokenLegacyFormat(nutIssuedTime,
				config);
		final long expiresAt = nut.computeExpiresAt(config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
	}

	/**
	 * timestamp is unsigned int with second precision which means that the timestamp will go up to
	 * 2106-02-07T02:28:15-0400
	 *
	 * @throws SqrlException
	 */
	@Test
	public void testComputeNutExpiresInJan2016() throws Exception {
		final int nutValidityInSeconds = 1000;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final LocalDateTime nutIssuedAt = LocalDateTime.parse("2016-01-03T10:15:30");
		final long tokenIssuedAt = nutIssuedAt.toEpochSecond(ZoneOffset.UTC);
		final SqrlNutTokenSingleBlockFormat nut = TestCaseSqrlNutHelper.buildValidSqrlNutTokenLegacyFormat(tokenIssuedAt,
				config);
		final long nutIssuedTime = nut.getIssuedTimestampMillis();
		final long expiresAt = nut.computeExpiresAt(config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
		assertEquals(1452816000L, expiresAt);
	}
}
