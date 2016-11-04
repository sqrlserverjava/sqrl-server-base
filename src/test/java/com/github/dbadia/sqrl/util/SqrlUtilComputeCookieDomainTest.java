package com.github.dbadia.sqrl.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.util.SqrlUtil;

@RunWith(Parameterized.class)
public class SqrlUtilComputeCookieDomainTest {

	// @formatter:off
	@Parameters(name = "{index}: SqrlClientOpt=({0})")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ "https://sqrljava.tech/blah", "sqrljava.tech" },
			{ "https://sqrljava.tech:20165/blah", "sqrljava.tech" },
			{ "https://sqrl.sqrljava.tech:20165/blah", "sqrl.sqrljava.tech" },
		});
	}

	// @formatter:on

	@Test
	public void testIt() {
		final SqrlConfig config = new SqrlConfig();
		final HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(input));
		final String result = SqrlUtil.computeCookieDomain(mockRequest, config);
		assertEquals(expected, result);
	}

	@Parameter(value = 0)
	public /* NOT private */ String input;

	@Parameter(value = 1)
	public /* NOT private */ String expected;

}
