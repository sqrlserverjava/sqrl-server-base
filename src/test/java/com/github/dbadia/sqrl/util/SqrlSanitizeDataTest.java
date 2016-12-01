package com.github.dbadia.sqrl.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.dbadia.sqrl.server.exception.SqrlInvalidRequestException;
import com.github.dbadia.sqrl.server.util.SqrlConstants;
import com.github.dbadia.sqrl.server.util.SqrlSanitize;

import junit.framework.TestCase;
import junitx.framework.ObjectAssert;

@RunWith(Parameterized.class)
public class SqrlSanitizeDataTest {

	@Parameters(name = "{index}: exceptionexpected={0}, data={1}")
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			{ true, tooBig },
			{ true, "<--Aal81H916zeXbbMTyUeA"},
			{ true, ">--Aal81H916zeXbbMTyUeA"},
			{ true, "{--Aal81H916zeXbbMTyUeA"},
			{ true, "}--Aal81H916zeXbbMTyUeA"},
			{ false, null },
			{ false, "" },
			// nut
			{ false, "--Aal81H916zeXbbMTyUeA" },
			// correlator
			{ false, "wAkAPBqy76cV7G0DSSR_6ML8R3gY3LlCZ7aTChE2k88" },
			// server param
			{ false, "c3FybDovL3NxcmxqYXZhLnRlY2gvc3FybGV4YW1wbGUvc3FybGJjP251dD1lVkVSZGpSeHVHNGd2aVB5ME5BQ3ZnJnNmbj1jM0Z5YkdwaGRtRXVkR1ZqYUEmY29yPWo0Q19yazZvUVpybjhPQW82U1BZZXFBMVVmQ0dWa1NhU2p1RDBNeE1xb1U" },
		});
		// @formatter:on
	}

	@Test
	public void testIt() throws Exception {
		try {
			SqrlSanitize.inspectIncomingSqrlData(data);
			TestCase.assertFalse("Exception was expected, but none was thrown", exceptionExpected);
		} catch (final Exception e) {
			if (exceptionExpected) {
				ObjectAssert.assertInstanceOf(SqrlInvalidRequestException.class, e);
			} else {
				// Wasn't expected, just throw it
				throw e;
			}
		}
	}

	@Parameter(value = 0)
	public /* NOT private */ boolean exceptionExpected;

	@Parameter(value = 1)
	public /* NOT private */ String data;

	private static String tooBig = null;

	static {
		final char[] chars = new char[SqrlConstants.MAX_SQRL_TOKEN_SIZE + 1];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = 'a';
		}
		tooBig = new String(chars);
	}
}
