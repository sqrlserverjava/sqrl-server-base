package com.github.dbadia.sqrl.server;

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

import com.github.dbadia.sqrl.server.SqrlConfig.ImageFormat;

@RunWith(Parameterized.class)
public class SqrlServerOperationsQrCodeTest {

	@Parameters(name = "{index}: imageFormat=({0})")
	public static Collection<Object[]> data() {
		// @formatter:off
		final List<Object[]> data = new ArrayList<>();
		for(final ImageFormat imageFormat : ImageFormat.values()) {
			data.add(new Object[]{imageFormat});
		}
		return data;
	}
	// @formatter:on

	@Test
	public void testQuery() throws Throwable {
		// TODO: need valid test data once SQRL client base64 idk bug is fixed
		final String configBackchannelPath = "/sqrlbc";
		final String loginRequestUrl = "http://127.0.0.1:8080/sqrlexample/login";

		// Data from a real transaction with a long expiry
		final SqrlIdentityPersistance sqrlPersistance = TCUtil.buildValidSqrlPersistance();
		sqrlConfig.setNutValidityInSeconds(Integer.MAX_VALUE);
		sqrlConfig.setBackchannelServletPath(configBackchannelPath);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(sqrlPersistance, sqrlConfig);

		// Emulate the login page generation
		final MockHttpServletRequest loginPageRequest = TCUtil.buildMockRequest(loginRequestUrl);
		final SqrlAuthPageData authPageData = sqrlServerOps.buildQrCodeForAuthPage(loginPageRequest,
				InetAddress.getByName("localhost"), 250);
		assertNotNull(authPageData);
		assertNotNull(authPageData.getQrCodeOutputStream());
	}

	private final SqrlConfig sqrlConfig;

	public SqrlServerOperationsQrCodeTest(final ImageFormat imageFormat) {
		super();
		sqrlConfig = TCUtil.buildValidSqrlConfig();
		sqrlConfig.setQrCodeFileType(imageFormat);
	}
}