package com.github.sqrlserverjava.backchannel;

import static com.github.sqrlserverjava.backchannel.SqrlServerOperationsRealClientDataTest.parseSqrlResponse;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.github.sqrlserverjava.MockSqrlHttpRequestBuilder;
import com.github.sqrlserverjava.SqrlClientFacingOperations;
import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.backchannel.SqrlTif.SqrlTifBuilder;
import com.github.sqrlserverjava.enums.SqrlServerSideKey;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;
import com.github.sqrlserverjava.util.SqrlConstants;

import junitx.framework.StringAssert;

public class SqrlServerOperationsNegativeTest {
	private static final String CLIENT_DATA_1_CORRELATOR = "jUJVUIpFWCP2PEMgivCIEme3d32GVH3UTafvAmL1Uqg";

	private static final SqrlTifBuilder	BUILDER	= new SqrlTifBuilder(false);
	private static final String EXPECTED_BAD_CLIENT_REQUEST = BUILDER.clearAllFlags()
			.addFlag(SqrlTifFlag.COMMAND_FAILED).addFlag(SqrlTifFlag.CLIENT_FAILURE).createTif().toHexString();

	@Before
	public void setUp() throws NoSuchFieldException {
		TestCaseUtil.createEmptySqrlPersistence();
		TestCaseUtil.clearStaticFields();
	}

	@Test
	public void testNutReplayed() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";
		final String serverValue = "cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";
		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=" + serverValue
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";

		TestCaseUtil.setupSqrlPersistence(CLIENT_DATA_1_CORRELATOR, serverValue);

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		// config.setBackchannelServletPath(configBackchannelPath);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.clientFacingOperations().handleSqrlClientRequest(queryRequest, servletResponse);
		servletResponse = new MockHttpServletResponse();
		sqrlServerOps.clientFacingOperations().handleSqrlClientRequest(queryRequest, servletResponse);
		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse.getStatus());
		final Map<String, String> responseDataTable = SqrlServerOperationsRealClientDataTest
				.parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals(EXPECTED_BAD_CLIENT_REQUEST, responseDataTable.get("tif"));
	}

	@Test
	public void testSignatureValidationFailed() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";

		final SqrlPersistence sqrlPersistence = TestCaseUtil.createEmptySqrlPersistence();

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		final String serverValue = "ZXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";
		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=" + serverValue
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1cqvBg";

		final SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(CLIENT_DATA_1_CORRELATOR,
				TestCaseUtil.AWHILE_FROM_NOW);
		sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverValue);
		sqrlPersistence.closeCommit();

		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.clientFacingOperations().handleSqrlClientRequest(queryRequest, servletResponse);
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
		assertEquals(EXPECTED_BAD_CLIENT_REQUEST, responseDataTable.get("tif"));
	}

	@Test
	public void testServerParamTampered() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";

		SqrlPersistence sqrlPersistence = TestCaseUtil.createEmptySqrlPersistence();
		// Store the lastServerParam but with a bad value
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		final SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(CLIENT_DATA_1_CORRELATOR,
				TestCaseUtil.AWHILE_FROM_NOW);
		sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT,
				// Change the first letter of server so it won't match
				"ZXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw");
		sqrlPersistence.closeCommit();

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw"
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.clientFacingOperations().handleSqrlClientRequest(queryRequest, servletResponse);
		final String content = servletResponse.getContentAsString();
		assertNotNull(content);

		// per https://www.grc.com/sqrl/semantics.htm Client -to- Server Semantics
		// Check the response generated by our code - since the server parameter was
		// tampered with "The web server will reply with the “Command failed” and “SQRL
		// failure” set in the “tif” value"
		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse.getStatus());
		final Map<String, String> responseDataTable = SqrlServerOperationsRealClientDataTest
				.parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals(EXPECTED_BAD_CLIENT_REQUEST, responseDataTable.get("tif"));
	}


	@Test
	public void testNoiptestNotSentAndIpChanged() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		final String correlatorFromServerParam = "jUJVUIpFWCP2PEMgivCIEme3d32GVH3UTafvAmL1Uqg";
		final String suk = "xyz";

		String serverParam = "cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";

		SqrlPersistence sqrlPersistence = TestCaseUtil.setupIdk(idk, correlatorFromServerParam, serverParam);
		sqrlPersistence.storeSqrlDataForSqrlIdentity(idk,
				Collections.singletonMap(SqrlServerSideKey.suk.toString(), suk));
		sqrlPersistence.closeCommit();

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		final SqrlClientFacingOperations clientFacingOperations = sqrlServerOps.clientFacingOperations();

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=" + serverParam
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";
		// Emulate the login page generation
		String firstIp = "11.11.11.11";
		final MockHttpServletRequest queryRequest = new MockSqrlHttpRequestBuilder(sqrlRequestUrl)
				.withQueryParams(rawQueryParams).fromIP(firstIp).build();
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		clientFacingOperations.handleSqrlClientRequest(queryRequest, servletResponse);
		String content = servletResponse.getContentAsString();
		assertNotNull(content);
		// Check the response generated by our code
		assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		Map<String, String> responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(5, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals("1", responseDataTable.get("tif"));

		// Now the ident call
		serverParam = "dmVyPTENCm51dD1UZjBoVWZXenpocG1zeEdyNS1kaDdRDQp0aWY9MA0KcXJ5PS9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PVRmMGhVZld6emhwbXN4R3I1LWRoN1EmY29yPWpVSlZVSXBGV0NQMlBFTWdpdkNJRW1lM2QzMkdWSDNVVGFmdkFtTDFVcWcNCg";
		String differentIp = "22.22.22.22";
		final String rawIdentParams = "client=dmVyPTENCmNtZD1pZGVudA0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCnN1az1jeTlYX2U1SGhoS3c1OGktNzdlNlFOX3A2NTFObjNHWWczQk1aUGU3ajBNDQp2dWs9NjlXM2hJYjhQZWtRVU03UmhrWkNHaHVEaHNBUVJpREpDRUQ3Q2VSMXgwOA0K"
				+ "&server=" + serverParam
				+ "&ids=SFEHcCzTb_cnaMaInR3nFt-L_fguMGEEXHVRATq3naTlCJ6TCTfarjjYRH8HR-tua-k4HLiSVtvdLRKqM6KFDg";
		// Store the server parrot so request validation will pass
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		sqrlPersistence.fetchSqrlCorrelatorRequired(correlatorFromServerParam).getTransientAuthDataTable()
				.put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		sqrlPersistence.closeCommit();

		final MockHttpServletRequest identRequest = new MockSqrlHttpRequestBuilder(sqrlRequestUrl)
				.withQueryParams(rawIdentParams).fromIP(differentIp).build();
		servletResponse = new MockHttpServletResponse();

		clientFacingOperations.handleSqrlClientRequest(identRequest, servletResponse);
		// To be clear: if the server does not receive the "noiptest"
		// option and the IP do not match, the operation will be hard
		// failed with the server's TIF flags having "0x40 Command failed"
		// =SET= and "0x04 IPs matched" =RESET=. Since this is not a
		// client failure, the "0x80 Client failure" bit will not be set.
		SqrlTif expectedSqrlTif = new SqrlTifBuilder(false).addFlag(SqrlTifFlag.COMMAND_FAILED).createTif();

		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse.getStatus());
		responseDataTable = SqrlServerOperationsRealClientDataTest
				.parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals(expectedSqrlTif, responseDataTable.get("tif"));
	}
}
