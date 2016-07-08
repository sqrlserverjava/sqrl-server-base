package com.github.dbadia.sqrl.server.backchannel;

import static junit.framework.TestCase.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletRequest;

import com.github.dbadia.sqrl.server.SqrlConfig;

@RunWith(Parameterized.class)
public class SqrlServerOperationsDetermineIpTest {
	@Parameters(name = "{index}: url=({0}) escheme=({1}) eurl=({2}) eport=({3}) euri=({4})")
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			// final String expectedIp, final String[] headerConfigSetting,
			// final String ipToSetOnRequest, final String[] headersToSetOnRequest
			{ "127.0.0.1", null, "127.0.0.1", null},
			{ "127.0.0.1", new String[]{"X-Forwarded-For"}, "127.0.0.1", new String[]{"abc", "123"}},
			{ "4.52.84.1", new String[]{"X-Forwarded-For"}, "192.168.1.1", new String[]{"X-Forwarded-For", "4.52.84.1"}},
			{ "4.52.84.1", new String[]{"X-Forwarded-For"}, "192.168.1.1", new String[]{"Some-Other-Header", "192.168.1.", "X-Forwarded-For", "4.52.84.1"}},
			{ "4.52.84.1", new String[]{"Some-Other-header", "X-Forwarded-For"}, "192.168.1.1", new String[]{"X-Forwarded-For", "4.52.84.1"}},
		});
	}
	// @formatter:on


	@Test
	public void testIt() throws Throwable {
		final InetAddress inetAddress = SqrlServerOperations.determineClientIpAddress(request, config);
		assertEquals(expectedIp, inetAddress);
	}

	// Instance variables and constructor are all boilerplate for Parameterized test, so put them at the bottom
	private final MockHttpServletRequest request = new MockHttpServletRequest();
	private final SqrlConfig config = new SqrlConfig();
	private final InetAddress expectedIp;

	public SqrlServerOperationsDetermineIpTest(final String expectedIp, final String[] headerConfigSetting,
			final String ipToSetOnRequest, final String[] headersToSetOnRequest) throws UnknownHostException {
		super();
		this.expectedIp = InetAddress.getByName(expectedIp);
		config.setIpForwardedForHeaders(headerConfigSetting);
		request.setRemoteHost(ipToSetOnRequest);
		if (headersToSetOnRequest != null) {
			for (int i = 0; i < headersToSetOnRequest.length; i += 2) {
				request.addHeader(headersToSetOnRequest[i], headersToSetOnRequest[i + 1]);
			}
		}
	}


}
