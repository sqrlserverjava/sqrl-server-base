package com.github.dbadia.sqrl.server;

import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlFlag;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.backchannel.SqrlClientOpt;
import com.github.dbadia.sqrl.server.backchannel.SqrlNutToken;
import com.github.dbadia.sqrl.server.backchannel.SqrlClientRequest;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif;
import com.github.dbadia.sqrl.server.backchannel.SqrlTifTest;
import com.github.dbadia.sqrl.server.backchannel.TCBackchannelUtil;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.TifBuilder;

public class SqrlServerOperationsClientOptsTest {
	final String					correlator	= "abc";
	private SqrlConfig				config;
	private SqrlPersistence			sqrlPersistence;
	private SqrlServerOperations	sqrlServerOps;
	private TifBuilder				tifBuilder;
	private SqrlNutToken			nutToken;

	@Before
	public void setUp() throws Exception {
		sqrlPersistence = TCUtil.createEmptySqrlPersistence();
		config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		sqrlServerOps = new SqrlServerOperations(config);
		tifBuilder = new TifBuilder();
		nutToken = TCUtil.buildValidSqrlNut(config, LocalDateTime.now());
	}

	@Test
	public void testOptCps_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TCUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "ident", false, SqrlClientOpt.cps);

		// Execute
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);

		// Validate
		assertTrue(idkExists);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH, SqrlTif.TIF_FUNCTIONS_NOT_SUPPORTED);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testOptHardlock_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TCUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "ident", false,
				SqrlClientOpt.hardlock);

		// Execute
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);

		// Validate
		assertTrue(idkExists);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH, SqrlTif.TIF_FUNCTIONS_NOT_SUPPORTED);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testOptSqrlOnly_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TCUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "ident", false,
				SqrlClientOpt.sqrlonly);

		// Execute
		final boolean idkExists = sqrlServerOps.processClientCommand(sqrlRequest, nutToken, tifBuilder, correlator);

		// Validate
		assertTrue(idkExists);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH, SqrlTif.TIF_FUNCTIONS_NOT_SUPPORTED);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}
}
