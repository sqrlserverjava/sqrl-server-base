package com.github.dbadia.sqrl.server.backchannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlFlag;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.TCUtil;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.SqrlTifBuilder;

public class SqrlCommandProcessorClientOptsTest {
	final String					correlator	= "abc";
	private SqrlConfig				config;
	private SqrlPersistence			sqrlPersistence;
	private SqrlTifBuilder				tifBuilder;
	private SqrlNutToken	nutToken;

	@Before
	public void setUp() throws Exception {
		sqrlPersistence = TCUtil.createEmptySqrlPersistence();
		config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		tifBuilder = new SqrlTifBuilder();
		nutToken = TCUtil.buildValidSqrlNut(config, LocalDateTime.now());
	}

	@After
	public void tearDown() throws Exception {
		sqrlPersistence.closeCommit();
	}

	@Test
	public void testOptCps_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TCUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "ident", correlator, false,
				SqrlClientOpt.cps);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence, tifBuilder);
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testOptHardlock_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TCUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "ident", correlator, false,
				SqrlClientOpt.hardlock);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence, tifBuilder);
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@Test
	public void testOptSqrlOnly_NotSupported() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TCUtil.setupIdk(idk, correlator, "123");

		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "ident", correlator, false,
				SqrlClientOpt.sqrlonly);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence, tifBuilder);
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate
		assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		// Ensure nothing got disabled
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}
}
