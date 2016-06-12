package com.grc.sqrl.server;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletRequest;

import junit.framework.TestCase;

@RunWith(Parameterized.class)
public class TCUtilTest {

	@Parameters(name = "{index}: url=({0}) escheme=({1}) eurl=({2}) eport=({3}) euri=({4})")
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] { 
			{ "http://127.0.0.1:8080/sqrlexample/sqrlbc", "http", "127.0.0.1", 8080, "/sqrlexample/sqrlbc" },
			{ "https://127.0.0.1:8080/sqrlexample/sqrlbc", "https", "127.0.0.1", 8080, "/sqrlexample/sqrlbc" },
			{ "http://127.0.0.1/sqrlexample/sqrlbc", "http", "127.0.0.1", -1, "/sqrlexample/sqrlbc" },
			{ "https://127.0.0.1/sqrlexample/sqrlbc", "https", "127.0.0.1", -1, "/sqrlexample/sqrlbc" },
			{ "http://eff.org:8080/sqrlexample/sqrlbc", "http", "eff.org", 8080, "/sqrlexample/sqrlbc" },
			{ "https://eff.org:8080/sqrlexample/sqrlbc", "https", "eff.org", 8080, "/sqrlexample/sqrlbc" },
			{ "http://eff.org/sqrlexample/sqrlbc", "http", "eff.org", -1, "/sqrlexample/sqrlbc" },
			{ "https://eff.org/sqrlexample/sqrlbc", "https", "eff.org", -1, "/sqrlexample/sqrlbc" },
		});
	}
	// @formatter:on

	@Test
	public void testBuildMockHttpRequest() throws URISyntaxException {
		final MockHttpServletRequest request = TCUtil.buildMockRequest(urlString);
		TestCase.assertNotNull(request);
		TestCase.assertEquals(expectedProtocol, request.getScheme());
		TestCase.assertEquals(expectedHostname, request.getServerName());
		TestCase.assertEquals(expectedPort, request.getServerPort());
		TestCase.assertEquals(expectedQueryString, request.getRequestURI());
	}


	// Instance variables and constructor are all boilerplate for Parameterized test, so put them at the bottom
	private final String urlString;
	private final String expectedProtocol;
	private final String expectedHostname;
	private final int expectedPort;
	private final String expectedQueryString;

	public TCUtilTest(final String urlString, final String expectedProtocol, final String expectedHostname,
			final int expectedPort, final String expectedQueryString) {
		super();
		this.urlString = urlString;
		this.expectedProtocol = expectedProtocol;
		this.expectedHostname = expectedHostname;
		this.expectedPort = expectedPort;
		this.expectedQueryString = expectedQueryString;
	}


}
