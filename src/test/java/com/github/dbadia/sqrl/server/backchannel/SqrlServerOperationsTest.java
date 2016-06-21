package com.github.dbadia.sqrl.server.backchannel;

import org.junit.Test;

import com.github.dbadia.sqrl.server.TCUtil;

import junit.framework.TestCase;

public class SqrlServerOperationsTest {
	@Test
	public void testValidateSqrlConfig_ConfigIsNull() {
		try {
			new SqrlServerOperations(TCUtil.buildEmptySqrlPersistence(), null);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			TestCase.assertTrue(e.getMessage().contains("SqrlConfig"));
			TestCase.assertTrue(e.getMessage().contains("not be null"));
		}
	}

	@Test
	public void testValidateSqrlConfig_persistenceIsNull() {
		try {
			new SqrlServerOperations(null, TCUtil.buildValidSqrlConfig());
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			TestCase.assertTrue(e.getMessage().contains("sqrlPersistence"));
			TestCase.assertTrue(e.getMessage().contains("not be null"));
		}
	}
}
