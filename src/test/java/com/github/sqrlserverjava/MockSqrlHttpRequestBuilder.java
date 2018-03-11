package com.github.sqrlserverjava;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.mock.web.MockHttpServletRequest;

public class MockSqrlHttpRequestBuilder {
	private final MockHttpServletRequest mockRequest = new MockHttpServletRequest();

	public MockSqrlHttpRequestBuilder(URI uri) {
		mockRequest.setScheme(uri.getScheme());
		mockRequest.setServerName(uri.getHost());
		mockRequest.setServerPort(uri.getPort());
		mockRequest.setRequestURI(uri.getPath());
	}

	public MockSqrlHttpRequestBuilder(String sqrlRequestUrl) throws URISyntaxException {
		this(new URI(sqrlRequestUrl));
	}

	public MockSqrlHttpRequestBuilder fromIP(String ip) {
		mockRequest.setRemoteAddr(ip);
		return this;
	}

	/**
	 * @param mockDataParams
	 *            a URI string from the GRC client log such as
	 *            "client=123&server=456&ids=789"
	 */
	public MockSqrlHttpRequestBuilder withQueryParams(String mockDataParams) {
		for (final String nameValuePair : mockDataParams.split("&")) {
			final String[] parts = nameValuePair.split("=");
			mockRequest.addParameter(parts[0], parts[1]);
		}
		return this;
	}

	public MockHttpServletRequest build() {
		return mockRequest;
   }
}
