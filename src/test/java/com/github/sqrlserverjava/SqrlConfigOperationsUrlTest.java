package com.github.sqrlserverjava;

import static junit.framework.TestCase.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(Parameterized.class)
public class SqrlConfigOperationsUrlTest {

	@Parameters(name = "{index}: bcsetting=({0}) httpreq=({1}) expected=({2})")
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] { 
			// SqrlConfig.backchannel,  login url, expected sqrl url 
			// expected subsequent,
			{ "sqrlbc", "https://sqrljava.com:20000/sqrlexample/login", "sqrl://sqrljava.com:20000/sqrlexample/sqrlbc", "/sqrlexample/sqrlbc",},
			{ "/sqrlexample/sqrlbc", "https://sqrljava.com:20000/sqrlexample/login", "sqrl://sqrljava.com:20000/sqrlexample/sqrlbc", 
				"/sqrlexample/sqrlbc",},
			{ "https://sqrljava.com:20000/sqrlexample/sqrlbc", "https://sqrljava.com:20000/sqrlexample/login", 
					"sqrl://sqrljava.com:20000/sqrlexample/sqrlbc", "/sqrlexample/sqrlbc",},
			// Test subdomain
			{ "http://sqrl.javasqrl.tech/sqrlbc", "http://javasqrl.tech/sqrlexample/login", 
						"qrl://sqrl.javasqrl.tech/sqrlbc", "/sqrlbc",},
			{ "http://sqrl.javasqrl.tech/", "http://javasqrl.tech/sqrlexample/login", 
							"qrl://sqrl.javasqrl.tech/", "/",},
		});
	}
	// @formatter:on

	@Test
	public void testBuildReturnPaths() throws Throwable {
		// Data from a real transaction with a long expiry
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		config.setBackchannelServletPath(configBackchannelPath);

		final SqrlConfigOperations ops = new SqrlConfigOperations(config);

		// Execute
		final MockHttpServletRequest loginServletRequest = TCUtil.buildMockRequest(loginRequestUrl);
		assertEquals(expectedFullSqrlUrl, ops.getBackchannelRequestUrl(loginServletRequest).toString());

		final MockHttpServletRequest sqrlServletRequest = TCUtil.buildMockRequest(expectedFullSqrlUrl);
		assertEquals(expectedSubsequentPath, ops.getSubsequentRequestPath(sqrlServletRequest));
	}

	// Instance variables and constructor are all boilerplate for Parameterized test, so put them at the bottom

	private final String	configBackchannelPath;
	private final String	loginRequestUrl;
	private final String	expectedFullSqrlUrl;
	private final String	expectedSubsequentPath;

	public SqrlConfigOperationsUrlTest(final String configBackchannelPath, final String loginRequestUrl,
			final String expectedFullSqrlUrl, final String expectedSubsequentPath) {
		super();
		this.configBackchannelPath = configBackchannelPath;
		this.loginRequestUrl = loginRequestUrl;
		this.expectedFullSqrlUrl = expectedFullSqrlUrl;
		this.expectedSubsequentPath = expectedSubsequentPath;
	}
}