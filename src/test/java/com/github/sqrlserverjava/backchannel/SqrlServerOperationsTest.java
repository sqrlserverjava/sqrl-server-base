package com.github.sqrlserverjava.backchannel;

import org.junit.Test;

import com.github.sqrlserverjava.SqrlServerOperations;

import junit.framework.TestCase;

public class SqrlServerOperationsTest {
	@Test
	public void testValidateSqrlConfig_ConfigIsNull() {
		try {
			new SqrlServerOperations(null);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			TestCase.assertTrue(e.getMessage().contains("SqrlConfig"));
			TestCase.assertTrue(e.getMessage().contains("not be null"));
		}
	}
}
