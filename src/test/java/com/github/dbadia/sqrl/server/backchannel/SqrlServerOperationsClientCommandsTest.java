package com.github.dbadia.sqrl.server.backchannel;

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
import com.github.dbadia.sqrl.server.TCUtil;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.TifBuilder;

import junitx.framework.ObjectAssert;
import junitx.framework.StringAssert;

public class SqrlServerOperationsClientCommandsTest {

	final String correlator = "abc";
	private SqrlConfig config;
	private SqrlPersistence sqrlPersistence;
	private SqrlServerOperations sqrlServerOps;
	private TifBuilder tifBuilder;
	private SqrlNutToken nutToken;

	@Before
	public void setUp() throws Exception {
		config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		sqrlPersistence = TCUtil.buildEmptySqrlPersistence();
		sqrlServerOps = new SqrlServerOperations(sqrlPersistence, config);
		tifBuilder = new TifBuilder();
		nutToken = TCUtil.buildValidSqrlNut(config, LocalDateTime.now());
	}

	@Test
	public void testCmdEnable_SqrlIdentityExists() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.startTransaction();
		sqrlPersistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		sqrlPersistence.commitTransaction();
		final SqrlRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "enable", true);

		// Execute - call start/commit since it is usually done by the caller
		sqrlPersistence.startTransaction();
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
		sqrlPersistence.commitTransaction();

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

		sqrlPersistence.startTransaction();
		sqrlPersistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		sqrlPersistence.commitTransaction();

		sqrlPersistence.startTransaction();
		sqrlPersistence.setSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED, false);
		sqrlPersistence.commitTransaction();

		final SqrlRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "enable", false); // No urs

		// Execute
		try {
			// Execute - call start/commit since it is usually done by the caller
			sqrlPersistence.startTransaction();
			sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
			sqrlPersistence.commitTransaction();
			fail("Exception expected");
		} catch (final Exception e) {
			sqrlPersistence.rollbackTransaction();
			e.printStackTrace();
			ObjectAssert.assertInstanceOf(SqrlInvalidRequestException.class, e);
			StringAssert.assertContains("urs", e.getMessage());
		}
		// Verify that it's still disabled
		assertFalse(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testCmdRemove_SqrlIdentityExists() throws Throwable {
		// Setup
		sqrlPersistence.startTransaction();
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		final SqrlRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "remove", true);
		sqrlPersistence.commitTransaction();

		// Execute all start/commit manually since it is usually done by the caller
		sqrlPersistence.startTransaction();
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
		sqrlPersistence.commitTransaction();

		// Validate
		assertTrue(idkExists);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		assertFalse(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	public void testCmdRemove_UrsMissing() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		sqrlPersistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		final SqrlRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "remove", false); // No urs

		// Execute
		try {
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
		TCUtil.setupIdk(idk, correlator, "123");
		final SqrlRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "disable", true);
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));

		// Execute - call start/commit since it is usually done by the caller
		sqrlPersistence.startTransaction();
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);
		sqrlPersistence.commitTransaction();

		// Validate
		assertTrue(idkExists);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		assertFalse(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}
}
