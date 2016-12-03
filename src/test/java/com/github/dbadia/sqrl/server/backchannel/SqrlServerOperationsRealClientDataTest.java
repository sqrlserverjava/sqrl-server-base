package com.github.dbadia.sqrl.server.backchannel;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
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

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.TCUtil;
import com.github.dbadia.sqrl.server.enums.SqrlServerSideKey;
import com.github.dbadia.sqrl.server.persistence.SqrlCorrelator;
import com.github.dbadia.sqrl.server.util.SqrlConstants;
import com.github.dbadia.sqrl.server.util.SqrlUtil;

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
		TCUtil.createEmptySqrlPersistence();
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
		final SqrlConfig config = TCUtil.buildTestSqrlConfig("GiXid26ALy2THQ7GT0a8sg");
		config.setNutValidityInSeconds(Integer.MAX_VALUE);

		final SqrlServerOperations sqrlServerOps = new SqrlServerOperations(config);

		// Store the server parrot
		SqrlPersistence sqrlPersistence = TCUtil.createSqrlPersistence();
		SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(correlatorFromServerParam, expiryTime);
		sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		sqrlPersistence.closeCommit();

		final String rawQueryParams = "client=dmVyPTENCmNtZD1xdWVyeQ0KaWRrPUNXNkVYRU1kY2xaYzNKRUpreV9Ld01GX0RoTWJrVjE1RTZRMTRweXFNTlkNCm9wdD1zdWsNCg"
				+ "&server=" + serverParam
				+ "&ids=aFZSlUvZFwiqCN2ycjui1ZdSQwtjVRVGqPy6IB-GUHJeDsF03LatdAdJ5XFYNB_R85a0s_v6UHXVtIV4yMX-AA";
		// Emulate the login page generation
		final MockHttpServletRequest queryRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawQueryParams);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		sqrlServerOps.handleSqrlClientRequest(queryRequest, servletResponse);
		String content = servletResponse.getContentAsString();
		assertNotNull(content);
		String result = new String(SqrlUtil.base64UrlDecode(content));
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

		final MockHttpServletRequest identRequest = TCUtil.buildMockRequest(sqrlRequestUrl, rawIdentParams);
		servletResponse = new MockHttpServletResponse();

		// Store the server parrot in the DB
		sqrlPersistence = TCUtil.createSqrlPersistence();
		sqrlCorrelator = sqrlPersistence.fetchSqrlCorrelator(sqrlCorrelator.getCorrelatorString());
		sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		sqrlPersistence.closeCommit();

		sqrlServerOps.handleSqrlClientRequest(identRequest, servletResponse);
		content = servletResponse.getContentAsString();
		assertNotNull(content);
		result = new String(SqrlUtil.base64UrlDecode(content));
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
		sqrlPersistence = TCUtil.createSqrlPersistence();
		final String idk = "CW6EXEMdclZc3JEJky_KwMF_DhMbkV15E6Q14pyqMNY";
		final String sukValue = sqrlPersistence.fetchSqrlIdentityDataItem(idk, SqrlServerSideKey.suk.toString());
		assertEquals("Ly47hSvzYMN2dAY1oOTAx0ouNk5ieNmDUqUEwoLKVQs", sukValue);

		// check for vuk
		final String vukValue = sqrlPersistence.fetchSqrlIdentityDataItem(idk, SqrlServerSideKey.vuk.toString());
		assertEquals("l1gKRo6taFh-19qN9D4Of24L1sfbLKjjZ4OzqUx16_8", vukValue);
		sqrlPersistence.closeCommit();
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
		assertEquals(null, responseDataTable.get(SqrlServerSideKey.suk));
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
		sqrlPersistence.storeSqrlDataForSqrlIdentity(idk,
				Collections.singletonMap(SqrlServerSideKey.suk.toString(), suk));
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

		// Now the ident call
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
		assertEquals("1", responseDataTable.get("ver"));
		StringAssert.assertStartsWith(expectedPath + "?nut=", responseDataTable.get("qry"));
		StringAssert.assertContains("cor=", responseDataTable.get("qry"));
		assertEquals("1", responseDataTable.get("tif"));
		assertNull("suk is only returned on query command", responseDataTable.get("suk"));
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
