package com.github.dbadia.sqrl.server;
import org.junit.Test;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlConfigOperations;
import com.github.dbadia.sqrl.server.SqrlConstants;

import junit.framework.TestCase;
import junitx.framework.StringAssert;

public class SqrlConfigOperationsTest {

	@Test
	public void testValidateSqrlConfig_ConfigGivesFullInvalidUrl() {
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		config.setBackchannelServletPath("www.yahoo.com/sqrl");
		try {
			new SqrlConfigOperations(config);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			StringAssert.assertContains("backchannel", e.getMessage());
			StringAssert.assertContains("www.yahoo.com/sqrl", e.getMessage());
			// The path error message should NOT be present
			StringAssert.assertNotContains("Perhaps you", e.getMessage());
			StringAssert.assertNotContains("and forgot ", e.getMessage());
			StringAssert.assertNotContains("forward slash", e.getMessage());
		}
	}

	@Test
	public void testValidateSqrlConfig_ConfigAesKeyTooBig() {
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		final int badLength = SqrlConstants.AES_KEY_LENGTH + 5;
		config.setAESKeyBytes(new byte[badLength]);
		try {
			new SqrlConfigOperations(config);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			StringAssert.assertContains("AES", e.getMessage());
			StringAssert.assertContains("must be", e.getMessage());
			StringAssert.assertContains(Integer.toString(badLength), e.getMessage());
			StringAssert.assertContains(Integer.toString(SqrlConstants.AES_KEY_LENGTH), e.getMessage());
		}
	}

	@Test
	public void testValidateSqrlConfig_ConfigAesKeyTooSmall() {
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		final int badLength = SqrlConstants.AES_KEY_LENGTH - 5;
		config.setAESKeyBytes(new byte[badLength]);
		try {
			new SqrlConfigOperations(config);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			StringAssert.assertContains("AES", e.getMessage());
			StringAssert.assertContains("must be", e.getMessage());
			StringAssert.assertContains(Integer.toString(badLength), e.getMessage());
			StringAssert.assertContains(Integer.toString(SqrlConstants.AES_KEY_LENGTH), e.getMessage());
		}
	}

	@Test
	public void testValidateSqrlConfig_ConfigGivesUrlWithoutSceme() {
		final SqrlConfig config = TCUtil.buildTestSqrlConfig();
		config.setBackchannelServletPath("www.yahoo.com");
		try {
			new SqrlConfigOperations(config);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			StringAssert.assertContains("backchannel", e.getMessage());
			StringAssert.assertContains("SqrlConfig", e.getMessage());
		}
	}
}
