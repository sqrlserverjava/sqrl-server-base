package com.github.sqrlserverjava.backchannel;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.backchannel.nut.SqrlNutToken0;
import com.github.sqrlserverjava.enums.SqrlIdentityFlag;
import com.github.sqrlserverjava.enums.SqrlInternalUserState;
import com.github.sqrlserverjava.enums.SqrlRequestCommand;
import com.github.sqrlserverjava.exception.SqrlInvalidRequestException;

import junitx.framework.ObjectAssert;
import junitx.framework.StringAssert;
public class SqrlCommandProcessorTest {

	final String					correlator	= "abc";
	private SqrlConfig				config;
	private SqrlPersistence			sqrlPersistence;
	private SqrlClientRequestProcessor	processor;
	private SqrlNutToken0				nutToken;

	@Before
	public void setUp() throws Exception {
		config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		sqrlPersistence = TestCaseUtil.createEmptySqrlPersistence();
		nutToken = TestCaseUtil.buildValidSqrlNut(config, LocalDateTime.now());
	}

	@After
	public void tearDown() throws Exception {
		sqrlPersistence.closeCommit();
	}

	@Test
	public void testCmdEnable_SqrlIdentityExists() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.createAndEnableSqrlIdentity(idk);
		sqrlPersistence.closeCommit();
		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.ENABLE,
				correlator, true);

		// Execute - call start/commit since it is usually done by the caller
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();

		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
				TestCaseUtil.buildTestSqrlConfig());
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();
		sqrlPersistence.closeCommit();
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testCmdEnable_UrsMissing() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";

		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		sqrlPersistence.createAndEnableSqrlIdentity(idk);
		sqrlPersistence.closeCommit();

		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		sqrlPersistence.setSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED, false);
		sqrlPersistence.closeCommit();

		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.ENABLE,
				correlator, false); // No
		// urs

		// Execute
		SqrlInternalUserState sqrlInternalUserState = null;
		try {
			// Execute - call start/commit since it is usually done by the caller
			sqrlPersistence = TestCaseUtil.createSqrlPersistence();

			final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
					TestCaseUtil.buildTestSqrlConfig());
			sqrlInternalUserState = processor.processClientCommand();
			sqrlPersistence.closeCommit();
			fail("Exception expected");
		} catch (final Exception e) {
			e.printStackTrace();
			sqrlPersistence.closeRollback();
			ObjectAssert.assertInstanceOf(SqrlInvalidRequestException.class, e);
			StringAssert.assertContains("urs", e.getMessage());
		}
		assertNull(sqrlInternalUserState);
		// Verify that it's still disabled
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		assertFalse(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));
	}

	@Test
	public void testCmdRemove_SqrlIdentityExists() throws Throwable {
		// Setup
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.createAndEnableSqrlIdentity(idk);
		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.REMOVE,
				correlator, true);
		sqrlPersistence.closeCommit();

		// Execute all start/commit manually since it is usually done by the caller
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();

		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
				TestCaseUtil.buildTestSqrlConfig());
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();
		sqrlPersistence.closeCommit();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		assertFalse(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	public void testCmdRemove_UrsMissing() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.createAndEnableSqrlIdentity(idk);
		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.REMOVE,
				correlator, false); // No
		// urs

		// Execute
		try {
			sqrlPersistence = TestCaseUtil.createSqrlPersistence();

			final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
					TestCaseUtil.buildTestSqrlConfig());
			processor.processClientCommand();
			fail("Exception expected");
		} catch (final Exception e) {
			ObjectAssert.assertInstanceOf(SqrlInvalidRequestException.class, e);
			StringAssert.assertContains("urs", e.getMessage());
		}
	}

	@Test
	public void testCmdDisable_SqrlIdentityExists() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		;
		TestCaseUtil.setupIdk(idk, correlator, "123");
		final SqrlClientRequest sqrlRequest = TestCaseUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.DISABLE,
				correlator, true);
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));

		// Execute - call start/commit since it is usually done by the caller
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();

		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
				TestCaseUtil.buildTestSqrlConfig());
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();
		sqrlPersistence.closeCommit();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		sqrlPersistence = TestCaseUtil.createSqrlPersistence();
		;
		assertFalse(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}
}
