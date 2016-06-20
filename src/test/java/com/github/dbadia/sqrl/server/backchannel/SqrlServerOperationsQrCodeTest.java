package com.github.dbadia.sqrl.server.backchannel;

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

import com.github.dbadia.sqrl.server.SqrlAuthPageData;
import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlConfig.ImageFormat;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.TCUtil;

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
		final SqrlPersistence sqrlPersistence = TCUtil.buildEmptySqrlPersistence();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		config.setBackchannelServletPath(configBackchannelPath);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(sqrlPersistence, config);

		// Emulate the login page generation
		final MockHttpServletRequest loginPageRequest = TCUtil.buildMockRequest(loginRequestUrl);
		final SqrlAuthPageData authPageData = sqrlServerOps.buildQrCodeForAuthPage(loginPageRequest,
				InetAddress.getByName("localhost"), 250);
		assertNotNull(authPageData);
		assertNotNull(authPageData.getQrCodeOutputStream());
	}

	private final SqrlConfig config;

	public SqrlServerOperationsQrCodeTest(final ImageFormat imageFormat) {
		super();
		config = TCUtil.buildValidSqrlConfig();
		config.setQrCodeFileType(imageFormat);
	}
}