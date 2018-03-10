package com.github.sqrlserverjava.backchannel;

import static junit.framework.TestCase.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.enums.SqrlIdentityFlag;
import com.github.sqrlserverjava.enums.SqrlInternalUserState;
import com.github.sqrlserverjava.enums.SqrlRequestCommand;
import com.github.sqrlserverjava.enums.SqrlRequestOpt;

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
		for(final SqrlRequestOpt opt : SqrlRequestOpt.values()) {
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
		TestCaseUtil.createEmptySqrlPersistence();
		TestCaseUtil.setupIdk(idk, "abc", "123");
		final SqrlClientRequest sqrlRequest = TCBackchannelUtil.buildMockSqrlRequest(idk, SqrlRequestCommand.QUERY,
				correlator, false,
				opt);

		// Execute
		final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlRequest, sqrlPersistence,
				TestCaseUtil.buildTestSqrlConfig());
		final SqrlInternalUserState sqrlInternalUserState = processor.processClientCommand();

		// Validate - everything should be normal since these flags are ignored on a query command
		TestCase.assertEquals(SqrlInternalUserState.IDK_EXISTS, sqrlInternalUserState);
		assertTrue(sqrlPersistence.fetchSqrlFlagForIdentity(idk, SqrlIdentityFlag.SQRL_AUTH_ENABLED));
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
	private final SqrlRequestOpt			opt;

	public SqrlCommandProcessorOptsIgnoredForQueryTest(final SqrlRequestOpt opt) throws Exception {
		super();
		this.opt = opt;
		sqrlPersistence = TestCaseUtil.createEmptySqrlPersistence();
		config = TestCaseUtil.buildTestSqrlConfig();
		config.setNutValidityInSeconds(Integer.MAX_VALUE);
	}
}
