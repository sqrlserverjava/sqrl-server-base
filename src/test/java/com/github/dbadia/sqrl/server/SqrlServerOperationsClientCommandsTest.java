package com.github.dbadia.sqrl.server;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlFlag;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.backchannel.SqrlNutToken;
import com.github.dbadia.sqrl.server.backchannel.SqrlClientRequest;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif;
import com.github.dbadia.sqrl.server.backchannel.SqrlTifTest;
import com.github.dbadia.sqrl.server.backchannel.TCBackchannelUtil;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.TifBuilder;
import com.github.dbadia.sqrl.server.exception.SqrlInvalidRequestException;

import junitx.framework.ObjectAssert;
import junitx.framework.StringAssert;

public class SqrlServerOperationsClientCommandsTest {

	final String					correlator	= "abc";
	private SqrlConfig				config;
	private SqrlPersistence			sqrlPersistence;
	private SqrlServerOperations	sqrlServerOps;
	private TifBuilder				tifBuilder;
	private SqrlNutToken			nutToken;

	@Before
	public void setUp() throws Exception {
		config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		sqrlPersistence = TCUtil.createEmptySqrlPersistence();
		sqrlServerOps = new SqrlServerOperations(config);
		tifBuilder = new TifBuilder();
		nutToken = TCUtil.buildValidSqrlNut(config, LocalDateTime.now());
	}

	@Test
	public void testCmdEnable_SqrlIdentityExists() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		sqrlPersistence.closeCommit();
		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "enable", true);

		// Execute - call start/commit since it is usually done by the caller
		sqrlPersistence = TCUtil.createSqrlPersistence();
		;
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
		sqrlPersistence.closeCommit();
		sqrlPersistence = TCUtil.createSqrlPersistence();
		;

		// Validate
		assertTrue(idkExists);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testCmdEnable_UrsMissing() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";

		sqrlPersistence = TCUtil.createSqrlPersistence();
		;
		sqrlPersistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		sqrlPersistence.closeCommit();

		sqrlPersistence = TCUtil.createSqrlPersistence();
		;
		sqrlPersistence.setSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED, false);
		sqrlPersistence.closeCommit();

		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "enable", false); // No urs

		// Execute
		try {
			// Execute - call start/commit since it is usually done by the caller
			sqrlPersistence = TCUtil.createSqrlPersistence();
			;
			sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
			sqrlPersistence.closeCommit();
			fail("Exception expected");
		} catch (final Exception e) {
			sqrlPersistence.closeRollback();
			e.printStackTrace();
			ObjectAssert.assertInstanceOf(SqrlInvalidRequestException.class, e);
			StringAssert.assertContains("urs", e.getMessage());
		}
		// Verify that it's still disabled
		sqrlPersistence = TCUtil.createSqrlPersistence();
		;
		assertFalse(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testCmdRemove_SqrlIdentityExists() throws Throwable {
		// Setup
		sqrlPersistence = TCUtil.createSqrlPersistence();
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "remove", true);
		sqrlPersistence.closeCommit();

		// Execute all start/commit manually since it is usually done by the caller
		sqrlPersistence = TCUtil.createSqrlPersistence();
		;
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
		sqrlPersistence.closeCommit();

		// Validate
		assertTrue(idkExists);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		sqrlPersistence = TCUtil.createSqrlPersistence();
		assertFalse(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	public void testCmdRemove_UrsMissing() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "remove", false); // No urs

		// Execute
		try {
			sqrlPersistence = TCUtil.createSqrlPersistence();
			;
			sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
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
		sqrlPersistence = TCUtil.createSqrlPersistence();
		;
		TCUtil.setupIdk(idk, correlator, "123");
		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "disable", true);
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));

		// Execute - call start/commit since it is usually done by the caller
		sqrlPersistence = TCUtil.createSqrlPersistence();
		;
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
		sqrlPersistence.closeCommit();

		// Validate
		assertTrue(idkExists);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		sqrlPersistence = TCUtil.createSqrlPersistence();
		;
		assertFalse(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}
}
