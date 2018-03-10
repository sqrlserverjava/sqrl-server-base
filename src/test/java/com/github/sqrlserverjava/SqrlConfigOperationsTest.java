package com.github.sqrlserverjava;

import java.util.Base64;

import org.junit.Test;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlConfigOperations;
import com.github.sqrlserverjava.util.SqrlConstants;

import junit.framework.TestCase;
import junitx.framework.StringAssert;

public class SqrlConfigOperationsTest {

	@Test
	public void testValidateSqrlConfig_ConfigGivesFullInvalidUrl() {
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
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
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setAesKeyBase64("oYqoDiWZiODUWDFSD2eJ5y8dNA==");
		try {
			new SqrlConfigOperations(config);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			StringAssert.assertContains("AES", e.getMessage());
			StringAssert.assertContains("must be", e.getMessage());
			StringAssert.assertContains(Integer.toString(SqrlConstants.AES_KEY_LENGTH), e.getMessage());
		}
	}

	@Test
	public void testValidateSqrlConfig_ConfigAesKeyNotBase64() {
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		config.setAesKeyBase64("oYqoDiW8dNA==");
		try {
			new SqrlConfigOperations(config);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			StringAssert.assertContains("Error base64 decoding", e.getMessage());
		}
	}
	
	@Test
	public void testValidateSqrlConfig_ConfigAesKeyTooSmall() {
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
		String base64 = Base64.getEncoder().encodeToString(new byte[14]);
		config.setAesKeyBase64("AAAAAAAAAAAAAAAAAAA=");
		try {
			new SqrlConfigOperations(config);
			TestCase.fail("Exception expected");
		} catch (final Exception e) {
			StringAssert.assertContains("AES", e.getMessage());
			StringAssert.assertContains("must be", e.getMessage());
			StringAssert.assertContains(Integer.toString(SqrlConstants.AES_KEY_LENGTH), e.getMessage());
		}
	}

	@Test
	public void testValidateSqrlConfig_ConfigGivesUrlWithoutSceme() {
		final SqrlConfig config = TestCaseUtil.buildTestSqrlConfig();
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
