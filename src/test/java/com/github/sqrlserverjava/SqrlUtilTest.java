package com.github.sqrlserverjava;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.github.sqrlserverjava.util.SqrlUtil;

public class SqrlUtilTest {

	/**
	 * SQRL spec uses a modified base64 which removes the "=" padding chars from the encoded value
	 */
	@Test
	public void testDecode_HaveToAddPadding() throws Exception {
		final String needsPadding = "dmVyPTENCmNtZD1pZGVudA0KaWRrPUNXNkVYRU1kY2xaYzNKRUpreV9Ld01GX0RoTWJrVjE1RTZRMTRweXFNTlkNCm9wdD1zdWsNCg";
		SqrlUtil.base64UrlDecode(needsPadding);
		assertTrue("No error during decode", true);
	}

	/**
	 * Even though the SQRL spec says to remove padding, make sure we can still decode it if it's present
	 */
	@Test
	public void testDecode_NoNeedToAddPadding() throws Exception {
		final String paddingAlreadyThere = "YmxhaA==";
		SqrlUtil.base64UrlDecode(paddingAlreadyThere);
		assertTrue("No error during decode", true);
	}

	@Test
	public void testEncode_HaveToRemovePadding() throws Exception {
		final String encoded = SqrlUtil.sqrlBase64UrlEncode("blah");
		assertEquals("YmxhaA", encoded);
	}

	@Test
	public void testEncode_NoNeedToRemovePadding() throws Exception {
		final String encoded = SqrlUtil.sqrlBase64UrlEncode("blahaa");
		assertEquals("YmxhaGFh", encoded);
	}

	@Test
	public void testExtractCookieDomain() throws Exception {
		final String loginRequestUrl = "http://sqrljava.tech/sqrlexample/app";
		final MockHttpServletRequest request = TCUtil.buildMockRequest(loginRequestUrl);
		final String result = SqrlUtil.computeCookieDomain(request, TCUtil.buildTestSqrlConfig());
		assertEquals("sqrljava.tech", result);
	}

	@Test
	public void testExtractCookieDomainFromConfig() throws Exception {
		final String loginRequestUrl = "http://sqrljava.tech/sqrlexample/app";
		final MockHttpServletRequest request = TCUtil.buildMockRequest(loginRequestUrl);
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		final String expected = "sqrl.sqrljava.tech";
		config.setCookieDomain(expected);
		final String result = SqrlUtil.computeCookieDomain(request, config);
		assertEquals(expected, result);
	}
}
