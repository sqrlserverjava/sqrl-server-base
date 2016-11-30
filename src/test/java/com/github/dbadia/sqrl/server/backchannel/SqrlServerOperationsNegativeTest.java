package com.github.dbadia.sqrl.server.backchannel;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.TCUtil;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;
import com.github.dbadia.sqrl.server.util.SqrlConstants;

import junitx.framework.StringAssert;

public class SqrlServerOperationsNegativeTest {
	private static final String CLIENT_DATA_1_CORRELATOR = "jUJVUIpFWCP2PEMgivCIEme3d32GVH3UTafvAmL1Uqg";

	private static final SqrlTif.SqrlTifBuilder	TIF_BUILDER	= new SqrlTif.SqrlTifBuilder(false);
	// TODO: refactor error handling then enable this
	// private static final String EXPECTED_TIF_BAD_CLIENT_REQUEST = TIF_BUILDER.clearAllFlags()
	// .addFlag(SqrlTif.TIF_COMMAND_FAILED).addFlag(SqrlTif.TIF_CLIENT_FAILURE).createTif().toHexString();
	private static final String					EXPECTED_TIF_BAD_CLIENT_REQUEST	= TIF_BUILDER.clearAllFlags()
			.addFlag(SqrlTif.TIF_COMMAND_FAILED).createTif().toHexString();

	@Test
	public void testNutReplayed() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";
		final String serverValue = "cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";
		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=" + serverValue
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";

		TCUtil.setupSqrlPersistence(CLIENT_DATA_1_CORRELATOR, serverValue);

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		// config.setBackchannelServletPath(configBackchannelPath);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.handleSqrlClientRequest(queryRequest, servletResponse);
		servletResponse = new MockHttpServletResponse();
		sqrlServerOps.handleSqrlClientRequest(queryRequest, servletResponse);
		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse.getStatus());
		final Map<String, String> responseDataTable = SqrlServerOperationsRealClientDataTest
				.parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals(EXPECTED_TIF_BAD_CLIENT_REQUEST, responseDataTable.get("tif"));
	}

	@Test
	public void testSignatureValidationFailed() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";

		final SqrlPersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence();

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		final String serverValue = "ZXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";
		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=" + serverValue
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1cqvBg";

		final SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(CLIENT_DATA_1_CORRELATOR,
				TCUtil.AWHILE_FROM_NOW);
		sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverValue);
		sqrlPersistence.closeCommit();

		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.handleSqrlClientRequest(queryRequest, servletResponse);
		final String content = servletResponse.getContentAsString();
		assertNotNull(content);
		// Check the response generated by our code
		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse.getStatus());
		final Map<String, String> responseDataTable = SqrlServerOperationsRealClientDataTest
				.parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		// TODO AT chagne to EXPECTED_TIF_BAD_CLIENT_REQUEST
		assertEquals("C0", responseDataTable.get("tif"));
	}

	@Test
	public void testServerParamTampered() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";

		SqrlPersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence();
		// Store the lastServerParam but with a bad value
		sqrlPersistence = TCUtil.createSqrlPersistence();
		final SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(CLIENT_DATA_1_CORRELATOR,
				TCUtil.AWHILE_FROM_NOW);
		sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT,
				// Change the first letter of server so it won't match
				"ZXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw");
		sqrlPersistence.closeCommit();

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw"
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.handleSqrlClientRequest(queryRequest, servletResponse);
		final String content = servletResponse.getContentAsString();
		assertNotNull(content);

		// Check the response generated by our code - since the server parameter was tampered with
		// "The web server will reply with the “Command failed” and “SQRL failure” set in the “tif” value"
		// per https://www.grc.com/sqrl/semantics.htm Client -to- Server Semantics
		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse.getStatus());
		final Map<String, String> responseDataTable = SqrlServerOperationsRealClientDataTest
				.parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals(EXPECTED_TIF_BAD_CLIENT_REQUEST, responseDataTable.get("tif"));
	}
}
