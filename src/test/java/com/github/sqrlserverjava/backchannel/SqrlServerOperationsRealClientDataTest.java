package com.github.sqrlserverjava.backchannel;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.github.sqrlserverjava.SqrlClientFacingOperations;
import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.enums.SqrlServerSideKey;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;
import com.github.sqrlserverjava.util.SqrlConstants;
import com.github.sqrlserverjava.util.SqrlUtil;

import junit.framework.TestCase;
import junitx.framework.StringAssert;

/**
 * Uses real data captured from the SQRL reference client
 *
 * @author Dave Badia
 *
 */
public class SqrlServerOperationsRealClientDataTest {
	private final Date expiryTime = new Date(System.currentTimeMillis() + 1_000_000);

	@Before
	public void setUp() throws NoSuchFieldException {
		TestCaseUtil.createEmptySqrlPersistence();
		TestCaseUtil.clearStaticFields();
	}

	@Test
	public void testFirstTime_SqrlIdentity() throws Throwable {
		// qrl://127.0.0.1:8081/sqrlexample/sqrlbc?nut=GiXid26ALy2THQ7GT0a8sg&sfn=bG9jYWxob3N0&cor=3Q7N5WBs50uYn__VjG4iv4Pkpkv2Ne1skQgZ48A6DSA
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";

		final String correlatorFromServerParam = "3Q7N5WBs50uYn__VjG4iv4Pkpkv2Ne1skQgZ48A6DSA";
		String serverParam = "cXJsOi8vMTI3LjAuMC4xOjgwODEvc3FybGV4YW1wbGUvc3FybGJjP251dD1HaVhpZDI2QUx5MlRIUTdHVDBhOHNnJnNmbj1iRzlqWVd4b2IzTjAmY29yPTNRN041V0JzNTB1WW5fX1ZqRzRpdjRQa3BrdjJOZTFza1FnWjQ4QTZEU0E";
		// (correlatorFromServerParam, serverParam);

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig("GiXid26ALy2THQ7GT0a8sg");
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		final SqrlClientFacingOperations clientFacingOperations = sqrlServerOps.clientFacingOperations();

		// Store the server parrot
		SqrlPersistence sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(correlatorFromServerParam, expiryTime);
		sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		sqrlPersistence.closeCommit();

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPUNXNkVYRU1kY2xaYzNKRUpreV9Ld01GX0RoTWJrVjE1RTZRMTRweXFNTlkNCm9wdD1zdWsNCg"
				+ "&server=" + serverParam
				+ "&ids=aFZSlUvZFwiqCN2ycjui1ZdSQwtjVRVGqPy6IB-GUHJeDsF03LatdAdJ5XFYNB_R85a0s_v6UHXVtIV4yMX-AA";
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		clientFacingOperations.handleSqrlClientRequest(queryRequest, servletResponse);
		String content = servletResponse.getContentAsString();
		assertNotNull(content);
		final String result = new String(SqrlUtil.base64UrlDecode(content));
		// Check the response generated by our code
		assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		Map<String, String> responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals("0", responseDataTable.get("tif"));

		// Now simulate ident call
		// 'server=' value :
		serverParam = "dmVyPTENCm51dD1iZVFIOHU4VlZHMG9od1hLUUJhOWNBDQp0aWY9MA0KcXJ5PS9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWJlUUg4dThWVkcwb2h3WEtRQmE5Y0EmY29yPTNRN041V0JzNTB1WW5fX1ZqRzRpdjRQa3BrdjJOZTFza1FnWjQ4QTZEU0ENCg";

		// POST Data String:
		final String rawIdentParams = "client=dmVyPTENCmNtZD1pZGVudA0KaWRrPUNXNkVYRU1kY2xaYzNKRUpreV9Ld01GX0RoTWJrVjE1RTZRMTRweXFNTlkNCnN1az1MeTQ3aFN2ellNTjJkQVkxb09UQXgwb3VOazVpZU5tRFVxVUV3b0xLVlFzDQp2dWs9bDFnS1JvNnRhRmgtMTlxTjlENE9mMjRMMXNmYkxLampaNE96cVV4MTZfOA0Kb3B0PXN1aw0K"
				+ "&server=" + serverParam
				+ "&ids=P94csUjLIrSJTx21axMdEnR7GFJJ78lTIvJ9oGU1KIDu46ATteZFiK1up-RHLcIcZxA2V7MW9LGNUod7j2jmCg";

		final MockHttpServletRequest identRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawIdentParams);
		servletResponse = new MockHttpServletResponse();

		// Store the server parrot in the DB
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		sqrlCorrelator = sqrlPersistence.fetchSqrlCorrelator(sqrlCorrelator.getCorrelatorString());
		sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		sqrlPersistence.closeCommit();

		clientFacingOperations.handleSqrlClientRequest(identRequest, servletResponse);
		content = servletResponse.getContentAsString();
		assertNotNull(content);
		// Check the response generated by our code

		assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals("0", responseDataTable.get("tif"));
		// suk should not be returned as transactions are atomic and it did not exist before
		assertNull(responseDataTable.get(SqrlServerSideKey.suk.toString()));

		// but... the suk must exist in the DB
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		final String idk = "CW6EXEMdclZc3JEJky_KwMF_DhMbkV15E6Q14pyqMNY";
		final String sukValue = sqrlPersistence.fetchSqrlIdentityDataItem(idk, SqrlServerSideKey.suk.toString());
		assertEquals("Ly47hSvzYMN2dAY1oOTAx0ouNk5ieNmDUqUEwoLKVQs", sukValue);

		// check for vuk
		final String vukValue = sqrlPersistence.fetchSqrlIdentityDataItem(idk, SqrlServerSideKey.vuk.toString());
		assertEquals("l1gKRo6taFh-19qN9D4Of24L1sfbLKjjZ4OzqUx16_8", vukValue);

		assertNull("url is only returned when cps param is sent", responseDataTable.get("url"));
		sqrlPersistence.closeCommit();
	}

	@Test
	public void testFirstTime_SqrlIdentity2ndRequestInvalid() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";

		final String correlatorFromServerParam = "jUJVUIpFWCP2PEMgivCIEme3d32GVH3UTafvAmL1Uqg";
		String serverParam = "cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";
		TestCaseUtil.setupSqrlPersistence(correlatorFromServerParam, serverParam);

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		final SqrlClientFacingOperations clientFacingOperations = sqrlServerOps.clientFacingOperations();
		// Store the server parrot
		TestCaseUtil.setupSqrlPersistence(correlatorFromServerParam, serverParam);

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=" + serverParam
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		clientFacingOperations.handleSqrlClientRequest(queryRequest, servletResponse);
		String content = servletResponse.getContentAsString();
		assertNotNull(content);
		// Check the response generated by our code
		assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		Map<String, String> responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals("0", responseDataTable.get("tif"));

		// Now sent ident
		serverParam = "dmVyPTENCm51dD1UZjBoVWZXenpocG1zeEdyNS1kaDdRDQp0aWY9MA0KcXJ5PS9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PVRmMGhVZld6emhwbXN4R3I1LWRoN1EmY29yPWpVSlZVSXBGV0NQMlBFTWdpdkNJRW1lM2QzMkdWSDNVVGFmdkFtTDFVcWcNCg";

		final String rawIdentParams = "client=dmVyPTENCmNtZD1pZGVudA0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCnN1az1jeTlYX2U1SGhoS3c1OGktNzdlNlFOX3A2NTFObjNHWWczQk1aUGU3ajBNDQp2dWs9NjlXM2hJYjhQZWtRVU03UmhrWkNHaHVEaHNBUVJpREpDRUQ3Q2VSMXgwOA0K"
				+ "&server=" + serverParam
				+ "&ids=SFEHcCzTb_cnaMaInR3nFt-L_fguMGEEXHVRATq3naTlCJ6TCTfarjjYRH8HR-tua-k4HLiSVtvdLRKqM6KFDg";

		final MockHttpServletRequest identRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawIdentParams);
		servletResponse = new MockHttpServletResponse();

		clientFacingOperations.handleSqrlClientRequest(identRequest, servletResponse);
		content = servletResponse.getContentAsString();
		assertNotNull(content);

		// Check the response generated by our code
		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletResponse.getStatus());
		responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(4, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals("C0", responseDataTable.get("tif"));
		// suk should not be present as transactions are atomic and it did not exist before
		assertNull(responseDataTable.get(SqrlServerSideKey.suk.toString()));
		assertNull("url is only returned when cps param is sent", responseDataTable.get("url"));
	}

	@Test
	public void testQueryIdent_SqrlIdentityExists_BrowserAuth() throws Throwable {
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
		final MockHttpServletRequest queryRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
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

		final String rawIdentParams = "client=dmVyPTENCmNtZD1pZGVudA0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCnN1az1jeTlYX2U1SGhoS3c1OGktNzdlNlFOX3A2NTFObjNHWWczQk1aUGU3ajBNDQp2dWs9NjlXM2hJYjhQZWtRVU03UmhrWkNHaHVEaHNBUVJpREpDRUQ3Q2VSMXgwOA0K"
				+ "&server=" + serverParam
				+ "&ids=SFEHcCzTb_cnaMaInR3nFt-L_fguMGEEXHVRATq3naTlCJ6TCTfarjjYRH8HR-tua-k4HLiSVtvdLRKqM6KFDg";
		// Store the server parrot so request validation will pass
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		sqrlPersistence.fetchSqrlCorrelatorRequired(correlatorFromServerParam).getTransientAuthDataTable()
		.put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		sqrlPersistence.closeCommit();

		final MockHttpServletRequest identRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawIdentParams);
		servletResponse = new MockHttpServletResponse();

		clientFacingOperations.handleSqrlClientRequest(identRequest, servletResponse);
		content = servletResponse.getContentAsString();
		assertNotNull(content);

		// Check the response generated by our code
		assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertNull("url is only returned when cps param is sent", responseDataTable.get("url"));
		assertEquals("1", responseDataTable.get("tif"));
		assertNull("suk is only returned on query command", responseDataTable.get("suk"));
	}

	@Test
	public void testQueryIdent_SqrlIdentityExists_CpsAuth() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";
		final String idk = "9OX0unQDwcBM6zYvm7X2gWF5hvRETuRqTgdw0FWmcNM";
		final String correlatorFromServerParam = "AEgmrOWDhQk1amN9j8sjRwCXfnm7FjB5Wh6jrrTBd6k";
		final String suk = "xyz";

		String serverParam = "c3FybDovLzEyNy4wLjAuMTo4MDgyL3NxcmxleGFtcGxlL3NxcmxiYz9udXQ9ZGJSQTZ2ZUVuQjdPMkN1V1hoNmRKUSZzZm49Ykc5allXeG9iM04wTG1OdmJRJmNvcj1BRWdtck9XRGhRazFhbU45ajhzalJ3Q1hmbm03RmpCNVdoNmpyclRCZDZr";

		SqrlPersistence sqrlPersistence = TestCaseUtil.setupIdk(idk, correlatorFromServerParam, serverParam);
		sqrlPersistence.storeSqrlDataForSqrlIdentity(idk,
				Collections.singletonMap(SqrlServerSideKey.suk.toString(), suk));
		sqrlPersistence.closeCommit();

		// Since there is no front side facing call from this test case, we need to set this manually
		final String browserFacingSqrlLoginUrl = "https://sqrljava.com:20000/sqrlexample/sqrllogin";
		TestCaseUtil.setSqrlServerOpsBrowserFacingUrl(new URL(browserFacingSqrlLoginUrl));

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		final SqrlClientFacingOperations clientFacingOperations = sqrlServerOps.clientFacingOperations();

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPTlPWDB1blFEd2NCTTZ6WXZtN1gyZ1dGNWh2UkVUdVJxVGdkdzBGV21jTk0NCnBpZGs9Q1NodHhOMGdRaEQ0Sm9sVUlaekh1cHVXYXhJTXJtRFl0VUJNNW9xb3E2WQ0Kb3B0PWNwc35zdWsNCg"
				+ "&server=" + serverParam
				+ "&ids=xKpxHhhpviglCEnKgzVR8V75KIFhZjG93ulLO89TP1mkZNRLAoeQTh446YRkZv8zcgBOsqgm5wmLmMesDQ8dDQ";
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
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
		serverParam = "dmVyPTENCm51dD1fWTJNMkJBQkI2MkkxV1JRaGE1MGhBDQp0aWY9NQ0KcXJ5PS9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PV9ZMk0yQkFCQjYySTFXUlFoYTUwaEEmY29yPUFFZ21yT1dEaFFrMWFtTjlqOHNqUndDWGZubTdGakI1V2g2anJyVEJkNmsNCnN1az1CT1hKZ2xMSEVOQUFaTU1KMWtWcjRmZ25vXzBueWR6cDhpSDJVZjh6NUdJDQo";

		final String rawIdentParams = "client=dmVyPTENCmNtZD1pZGVudA0KaWRrPTlPWDB1blFEd2NCTTZ6WXZtN1gyZ1dGNWh2UkVUdVJxVGdkdzBGV21jTk0NCnBpZGs9Q1NodHhOMGdRaEQ0Sm9sVUlaekh1cHVXYXhJTXJtRFl0VUJNNW9xb3E2WQ0Kb3B0PWNwc35zdWsNCg"
				+ "&server=" + serverParam
				+ "&ids=n4jzxR3kHScltt__wiZkkHnCaZwOxiY2HnA6E-zHyWNgZZRxj07Os-9PLNPO5j_mGPMnro2B3xFPAtH22aP2Dg";
		// Store the server parrot so request validation will pass
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		sqrlPersistence.fetchSqrlCorrelatorRequired(correlatorFromServerParam).getTransientAuthDataTable()
		.put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		sqrlPersistence.closeCommit();

		final MockHttpServletRequest identRequest = TestCaseUtil.buildMockRequest(sqrlRequestUrl, rawIdentParams);
		servletResponse = new MockHttpServletResponse();

		clientFacingOperations.handleSqrlClientRequest(identRequest, servletResponse);
		content = servletResponse.getContentAsString();
		assertNotNull(content);

		// Check the response generated by our code
		assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		System.out.println(servletResponse.getContentAsString());
		responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals("1", responseDataTable.get("ver"));
		// TODO: finish this
		// StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		// StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		// assertNull("url is only returned when cps param is sent", responseDataTable.get("url"));
		// assertEquals("1", responseDataTable.get("tif"));
		// assertNull("suk is only returned on query command", responseDataTable.get("suk"));
	}

	/* ************* Util methods **************************/
	static Map<String, String> parseSqrlResponse(final String contentAsString) throws IOException {
		final Map<String, String> dataTable = new ConcurrentHashMap<String, String>();
		final String data = new String(Base64.getUrlDecoder().decode(contentAsString));
		try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
			String line = reader.readLine();
			while (line != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				final int index = line.indexOf('=');
				TestCase.assertTrue("Could not find = in line: " + line, index != -1);
				final String name = line.substring(0, index);
				final String value = line.substring(index + 1);
				dataTable.put(name, value);
				line = reader.readLine();
			}
		}
		return dataTable;
	}

}
