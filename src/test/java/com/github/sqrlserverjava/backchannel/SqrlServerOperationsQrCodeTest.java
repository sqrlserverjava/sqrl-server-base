package com.github.sqrlserverjava.backchannel;

import static junit.framework.TestCase.assertNotNull;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.github.sqrlserverjava.SqrlAuthPageData;
import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.enums.SqrlQrCodeImageFormat;

/**
 * Tests QR code generation for each image format we support
 *
 * @author Dave Badia
 *
 */
@RunWith(Parameterized.class)
public class SqrlServerOperationsQrCodeTest {

	@Parameters(name = "{index}: imageFormat=({0})")
	public static Collection<Object[]> data() {
		// @formatter:off
		final List<Object[]> data = new ArrayList<>();
		for(final SqrlQrCodeImageFormat imageFormat : SqrlQrCodeImageFormat.values()) {
			data.add(new Object[]{imageFormat});
		}
		return data;
	}
	// @formatter:on

	@Test
	public void testQuery() throws Throwable {
		final String configBackchannelPath = "/sqrlbc";
		final String loginRequestUrl = "https://sqrljava.com:20000/sqrlexample/login";

		// Data from a real transaction with a long expiry
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		config.setBackchannelServletPath(configBackchannelPath);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);

		// Emulate the login page generation
		final MockHttpServletRequest loginPageRequest = TestCaseUtil.buildMockRequest(loginRequestUrl);
		final MockHttpServletResponse loginPageResponse = new MockHttpServletResponse();
		final SqrlAuthPageData authPageData = sqrlServerOps.browserFacingOperations()
				.prepareSqrlAuthPageData(loginPageRequest, loginPageResponse,
				InetAddress.getByName("localhost"), 250);
		assertNotNull(authPageData);
		assertNotNull(authPageData.getQrCodeOutputStream());
	}

	private final SqrlConfig config;

	public SqrlServerOperationsQrCodeTest(final SqrlQrCodeImageFormat imageFormat) {
		super();
		config = TestCaseUtil.buildTestSqrlConfig();
		config.setQrCodeImageFormat(imageFormat);
	}
}