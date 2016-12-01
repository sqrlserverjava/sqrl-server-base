package com.github.dbadia.sqrl.server.backchannel;

import static junit.framework.TestCase.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlFlag;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.TCUtil;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.SqrlTifBuilder;

import junit.framework.TestCase;

/**
 * Some opts are to be <b>ignored</b> on a query command, test it
 *
 * @author Dave Badia
 *
 */
@RunWith(Parameterized.class)
public class SqrlCommandProcessorOptsIgnoredForQueryTest {

	@Parameters(name = "{index}: SqrlClientOpt=({0})")
	public static Collection<Object[]> data() {
		// @formatter:off
		// return all SqrlClientOpt which are nonQuery only
		final List<Object[]> data = new ArrayList<>();
		for(final SqrlClientOpt opt : SqrlClientOpt.values()) {
			if(opt.isNonQueryOnly()) {
				data.add(new Object[]{opt});
			}
		}
		return data;
	}
	// @formatter:on

	@Test
	public void testIt() throws Throwable {
		// Setup
		final String idk = "m470Fb8O3XY8xAqlN2pCL0SokqPYNazwdc5sT6SLnUM";
		TCUtil.createEmptySqrlPersistence();
		TCUtil.setupIdk(idk, "abc", "123");
		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, "query", correlator, false,
				opt);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence, tifBuilder);
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate - everything should be normal since these flags are ignored on a query command
		TestCase.assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		final SqrlTif tif = tifBuilder.createTif();
		SqrlTifTest.assertTif(tif, SqrlTif.TIF_CURRENT_ID_MATCH);
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlFlag.SQRL_AUTH_ENABLED));
		assertTrue(sqrlPersistence.doesSqrlIdentityExistByIdk(idk));
	}

	@After
	public void tearDown() throws Exception {
		sqrlPersistence.closeCommit();
	}

	// In parameterized tests, the instance variables and constructor are boilerplate so keep them out of the way
	private final String				correlator	= "abc";
	private final SqrlConfig			config;
	private final SqrlPersistence		sqrlPersistence;
	private final SqrlServerOperations	sqrlServerOps;
	private final SqrlTifBuilder			tifBuilder;
	private final SqrlNutToken			nutToken;
	private final SqrlClientOpt			opt;

	public SqrlCommandProcessorOptsIgnoredForQueryTest(final SqrlClientOpt opt) throws Exception {
		super();
		this.opt = opt;
		sqrlPersistence = TCUtil.createEmptySqrlPersistence();
		config = TCUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
		sqrlServerOps = new SqrlServerOperations(config);
		tifBuilder = new SqrlTifBuilder();
		nutToken = TCUtil.buildValidSqrlNut(config, LocalDateTime.now());
	}
}
