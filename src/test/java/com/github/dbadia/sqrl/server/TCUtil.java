package com.github.dbadia.sqrl.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.springframework.mock.web.MockHttpServletRequest;

import com.github.dbadia.sqrl.server.backchannel.SqrlNutToken;

public class TCUtil {
	static final String DEFAULT_CONFIG_SQRL_BACKCHANNEL_PATH = "http://127.0.0.1:8080/sqrlbc";
	static final byte[] AES_TEST_KEY = new byte[16];

	public static final SqrlConfig buildValidSqrlConfig() {
		final SqrlConfig sqrlConfig = new SqrlConfig();
		sqrlConfig.setServerFriendlyName("Dave Test");
		sqrlConfig.setBackchannelServletPath("http://127.0.0.1:8080/sqrlbc");
		// set AES key to all zeros for test cases
		sqrlConfig.setAESKeyBytes(AES_TEST_KEY);
		// TestSecureRandom isn't random at all which is very fast
		// If we didn't set a secure random, SecureRandom.getInstance will be called
		// which would slow down most of our test cases for no good reason
		sqrlConfig.setSecureRandom(new TestSecureRandom());

		return sqrlConfig;
	}

	public static final SqrlPersistence buildValidsqrlPersistence() {
		return new TestOnlysqrlPersistence();
	}

	public static MockHttpServletRequest buildMockRequest(final String uriString) throws URISyntaxException {
		return buildMockRequest(new URI(uriString));
	}

	/**
	 * 
	 * @param loginRequestUrl
	 * @param mockDataParams
	 *            a URI string from the GRC client log such as "client=123&server=456&ids=789"
	 * @return
	 * @throws URISyntaxException
	 */
	public static MockHttpServletRequest buildMockRequest(final String requestUrl, final String mockDataParams)
			throws URISyntaxException {
		final MockHttpServletRequest mockRequest = buildMockRequest(requestUrl);
		for (final String nameValuePair : mockDataParams.split("&")) {
			final String[] parts = nameValuePair.split("=");
			mockRequest.addParameter(parts[0], parts[1]);
		}
		return mockRequest;
	}

	public static MockHttpServletRequest buildMockRequest(final URI uri) {
		final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setScheme(uri.getScheme());
		mockRequest.setServerName(uri.getHost());
		mockRequest.setServerPort(uri.getPort());
		mockRequest.setRequestURI(uri.getPath());
		return mockRequest;
	}

	public static MockHttpServletRequest buildMockRequest(final URL url) {
		final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setScheme(url.getProtocol());
		mockRequest.setServerName(url.getHost());
		mockRequest.setServerPort(url.getPort());
		mockRequest.setRequestURI(url.getPath());
		return mockRequest;
	}

	public static SqrlNutToken buildValidSqrlNut(final SqrlConfig sqrlConfig) throws SqrlException {
		final long timestamp = System.currentTimeMillis();
		final int inetInt = 4;
		final int counter = 234;
		final int random = 6;
		final SqrlNutToken nut = new SqrlNutToken(inetInt, new SqrlConfigOperations(sqrlConfig), counter, timestamp, random);
		return nut;
	}


}
