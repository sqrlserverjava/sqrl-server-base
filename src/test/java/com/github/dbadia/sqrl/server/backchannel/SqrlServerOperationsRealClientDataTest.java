package com.github.dbadia.sqrl.server.backchannel;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.TCUtil;
import com.github.dbadia.sqrl.server.util.SqrlConstants;

import junit.framework.TestCase;
import junitx.framework.StringAssert;

/**
 * Uses real data captured from the SQRL reference client
 *
 * @author Dave Badia
 *
 */
public class SqrlServerOperationsRealClientDataTest {

	@Before
	public void setUp() throws NoSuchFieldException {
		TCUtil.createEmptySqrlPersistence();
	}

	@Test
	public void testFirstTime_SqrlIdentity() throws Throwable {
		// final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		// final String expectedPath = "/sqrlexample/sqrlbc";
		//
		// final String correlatorFromServerParam = "jUJVUIpFWCP2PEMgivCIEme3d32GVH3UTafvAmL1Uqg";
		// String serverParam =
		// "cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";
		// final SqrlPersistence sqrlPersistence = TCUtil.buildSqrlPersistence(correlatorFromServerParam, serverParam);
		//
		// // Data from a real transaction with a long expiry
		// final SqrlConfig config = TCUtil.buildTestSqrlConfig("Tf0hUfWzzhpmsxGr5-dh7Q");
		// config.setNutValidityInSeconds(Integer.MAX_VALUE);
		//
		// final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		// // Store the server parrot
		// TCUtil.buildSqrlPersistence(correlatorFromServerParam, serverParam);
		//
		// final String rawQueryParams =
		// "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
		// + "&server=" + serverParam
		// + "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";
		// // Emulate the login page generation
		// final MockHttpServletRequest queryRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		// MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		//
		// sqrlServerOps.handleSqrlClientRequest(queryRequest, servletResponse);
		// String content = servletResponse.getContentAsString();
		// assertNotNull(content);
		// String result = new String(SqrlUtil.base64UrlDecode(content));
		// // Check the response generated by our code
		// assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		// Map<String, String> responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		// assertEquals(4, responseDataTable.size());
		// assertEquals("1", responseDataTable.get("ver"));
		// StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		// StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		// assertEquals("0", responseDataTable.get("tif"));
		//
		// // Now sent ident
		// serverParam =
		// "dmVyPTENCm51dD1UZjBoVWZXenpocG1zeEdyNS1kaDdRDQp0aWY9MA0KcXJ5PS9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PVRmMGhVZld6emhwbXN4R3I1LWRoN1EmY29yPWpVSlZVSXBGV0NQMlBFTWdpdkNJRW1lM2QzMkdWSDNVVGFmdkFtTDFVcWcNCg";
		//
		// final String rawIdentParams =
		// "client=dmVyPTENCmNtZD1pZGVudA0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCnN1az1jeTlYX2U1SGhoS3c1OGktNzdlNlFOX3A2NTFObjNHWWczQk1aUGU3ajBNDQp2dWs9NjlXM2hJYjhQZWtRVU03UmhrWkNHaHVEaHNBUVJpREpDRUQ3Q2VSMXgwOA0K"
		// + "&server=" + serverParam
		// + "&ids=SFEHcCzTb_cnaMaInR3nFt-L_fguMGEEXHVRATq3naTlCJ6TCTfarjjYRH8HR-tua-k4HLiSVtvdLRKqM6KFDg";
		//
		// final MockHttpServletRequest identRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawIdentParams);
		// servletResponse = new MockHttpServletResponse();
		//
		// sqrlServerOps.handleSqrlClientRequest(identRequest, servletResponse);
		// content = servletResponse.getContentAsString();
		// assertNotNull(content);
		// result = new String(SqrlUtil.base64UrlDecode(content));
		// // Check the response generated by our code
		//
		// assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		// responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		// assertEquals(5, responseDataTable.size());
		// assertEquals("1", responseDataTable.get("ver"));
		// StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		// StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		// assertEquals("0", responseDataTable.get("tif"));
		// // suk should not be present as transactions are atomic and it did not exist before
		// assertEquals("cy9X_e5HhhKw58i-77e6QN_p651Nn3GYg3BMZPe7j0M",
		// responseDataTable.get(SqrlConstants.KEY_TYPE_SUK));
	}

	@Test
	public void testFirstTime_SqrlIdentity2ndRequestInvalid() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";

		final String correlatorFromServerParam = "jUJVUIpFWCP2PEMgivCIEme3d32GVH3UTafvAmL1Uqg";
		String serverParam = "cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";
		TCUtil.setupSqrlPersistence(correlatorFromServerParam, serverParam);

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);
		// Store the server parrot
		TCUtil.setupSqrlPersistence(correlatorFromServerParam, serverParam);

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=" + serverParam
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.handleSqrlClientRequest(queryRequest, servletResponse);
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

		final MockHttpServletRequest identRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawIdentParams);
		servletResponse = new MockHttpServletResponse();

		sqrlServerOps.handleSqrlClientRequest(identRequest, servletResponse);
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
		assertEquals(null, responseDataTable.get(SqrlConstants.KEY_TYPE_SUK));
	}

	@Test
	public void testQueryIdent_SqrlIdentityExists() throws Throwable {
		final String sqrlRequestUrl = "qrl://127.0.0.1:8080/sqrlexample/sqrlbc";
		final String expectedPath = "/sqrlexample/sqrlbc";
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		final String correlatorFromServerParam = "jUJVUIpFWCP2PEMgivCIEme3d32GVH3UTafvAmL1Uqg";
		final String suk = "xyz";

		String serverParam = "cXJsOi8vc3FybGphdmEudGVjaC9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PWVCbms4d3hyQ2RTX3VBMUwzX013Z3cmc2ZuPWMzRnliR3BoZG1FdWRHVmphQSZjb3I9alVKVlVJcEZXQ1AyUEVNZ2l2Q0lFbWUzZDMyR1ZIM1VUYWZ2QW1MMVVxZw";

		SqrlPersistence sqrlPersistence = TCUtil.setupIdk(idk, correlatorFromServerParam, serverParam);
		sqrlPersistence.storeSqrlDataForSqrlIdentity(idk, Collections.singletonMap(SqrlConstants.KEY_TYPE_SUK, suk));
		sqrlPersistence.closeCommit();

		// Data from a real transaction with a long expiry
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCg"
				+ "&server=" + serverParam
				+ "&ids=ROkIkpNyMrUsaD_Y6JIioE1shQ18ddM7b_PWQ5xmtkjdiZ1NtOTri-zOpSj1qptmNjCuKfG-Cpll3tgF1dqvBg";
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.handleSqrlClientRequest(queryRequest, servletResponse);
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

		// Now sent ident
		serverParam = "dmVyPTENCm51dD1UZjBoVWZXenpocG1zeEdyNS1kaDdRDQp0aWY9MA0KcXJ5PS9zcXJsZXhhbXBsZS9zcXJsYmM_bnV0PVRmMGhVZld6emhwbXN4R3I1LWRoN1EmY29yPWpVSlZVSXBGV0NQMlBFTWdpdkNJRW1lM2QzMkdWSDNVVGFmdkFtTDFVcWcNCg";

		final String rawIdentParams = "client=dmVyPTENCmNtZD1pZGVudA0KaWRrPW00NzBGYjhPM1hZOHhBcWxOMnBDTDBTb2txUFlOYXp3ZGM1c1Q2U0xuVU0NCm9wdD1zdWsNCnN1az1jeTlYX2U1SGhoS3c1OGktNzdlNlFOX3A2NTFObjNHWWczQk1aUGU3ajBNDQp2dWs9NjlXM2hJYjhQZWtRVU03UmhrWkNHaHVEaHNBUVJpREpDRUQ3Q2VSMXgwOA0K"
				+ "&server=" + serverParam
				+ "&ids=SFEHcCzTb_cnaMaInR3nFt-L_fguMGEEXHVRATq3naTlCJ6TCTfarjjYRH8HR-tua-k4HLiSVtvdLRKqM6KFDg";
		// Store the server parrot so request validation will pass
		sqrlPersistence = TCUtil.createSqrlPersistence();
		sqrlPersistence.fetchSqrlCorrelatorRequired(correlatorFromServerParam).getTransientAuthDataTable()
		.put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		sqrlPersistence.closeCommit();

		final MockHttpServletRequest identRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawIdentParams);
		servletResponse = new MockHttpServletResponse();

		sqrlServerOps.handleSqrlClientRequest(identRequest, servletResponse);
		content = servletResponse.getContentAsString();
		assertNotNull(content);

		// Check the response generated by our code
		assertEquals(HttpServletResponse.SC_OK, servletResponse.getStatus());
		responseDataTable = parseSqrlResponse(servletResponse.getContentAsString());
		assertEquals(5, responseDataTable.size());
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals("1", responseDataTable.get("tif"));
		assertEquals("cy9X_e5HhhKw58i-77e6QN_p651Nn3GYg3BMZPe7j0M", responseDataTable.get("suk"));
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
