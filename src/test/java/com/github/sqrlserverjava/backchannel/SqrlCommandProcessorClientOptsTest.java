package com.github.sqrlserverjava.backchannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.enums.SqrlIdentityFlag;
import com.github.sqrlserverjava.enums.SqrlInternalUserState;
import com.github.sqrlserverjava.enums.SqrlRequestCommand;
import com.github.sqrlserverjava.enums.SqrlRequestOpt;

public class SqrlCommandProcessorClientOptsTest {
	final String					correlator	= "abc";
	private SqrlConfig				config;
	private SqrlPersistence			sqrlPersistence;

	@Before
	public void setUp() throws Exception {
		sqrlPersistence = TestCaseUtil.createEmptySqrlPersistence();
		config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
	}

	@After
	public void tearDown() throws Exception {
		sqrlPersistence.closeCommit();
	}

	@Test
	public void testOptCps_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TestCaseUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.IDENT,
				correlator, false,
				SqrlRequestOpt.cps);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
				TestCaseUtil.buildTestSqrlConfig());
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testOptHardlock_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TestCaseUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.IDENT,
				correlator, false, SqrlRequestOpt.hardlock);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
				TestCaseUtil.buildTestSqrlConfig());
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testOptSqrlOnly_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TestCaseUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.IDENT,
				correlator, false,
				SqrlRequestOpt.sqrlonly);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
				TestCaseUtil.buildTestSqrlConfig());
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}
	

	@Test
	public void testOptNoIpTest() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TestCaseUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.IDENT,
				correlator, false, SqrlRequestOpt.noiptest);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
				TestCaseUtil.buildTestSqrlConfig());
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}
}
