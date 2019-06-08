package com.github.sqrlserverjava;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class SqrlServerOperationsTest {

	@Test
	public void testUrlsMatch() throws Throwable {
		final String configBackchannelPath = "/sqrlbc";
		final String loginRequestUrl = "https://sqrljava.com:20000/sqrlexample/sqrllogin";

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		config.setBackchannelServletPath(configBackchannelPath);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);

		// Generate the login page data
		final MockHttpServletRequest loginPageRequest = TestCaseUtil.buildMockRequest(loginRequestUrl);
		final MockHttpServletResponse loginPageResponse = new MockHttpServletResponse();
		final AuthPageData authPageData = sqrlServerOps.browserFacingOperations()
				.prepareSqrlAuthPageData(loginPageRequest, loginPageResponse,
				250);
		assertNotNull(authPageData);
		assertNotNull(authPageData.getUrl());
		final String clickUrl = authPageData.getUrl();

		assertNotNull(authPageData.getQrCodeOutputStream());
		final ByteArrayInputStream bais = new ByteArrayInputStream(authPageData.getQrCodeOutputStream().toByteArray());
		final BinaryBitmap binaryBitmap = new BinaryBitmap(
				new HybridBinarizer(new BufferedImageLuminanceSource(ImageIO.read(bais))));
		final Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap);
		final String qrCodeUrl = qrCodeResult.getText();

		assertEquals(clickUrl, qrCodeUrl);
	}
}
