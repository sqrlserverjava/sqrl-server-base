package com.github.sqrlserverjava;

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

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.util.SqrlUtil;

@RunWith(Parameterized.class)
public class SqrlServerOperationsComputeCookieDomainTest {

	// @formatter:off
	@Parameters(name = "{index}: SqrlClientOpt=({0})")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ "sqrljava.tech", "https://sqrljava.tech/blah" },
			{ "sqrljava.tech", "https://sqrljava.tech/blah/more" },
			{ "sqrljava.tech", "https://sqrljava.tech" },
			{ "sqrljava.tech", "https://sqrljava.tech/" },
			{ "sqrljava.tech", "https://sqrljava.tech:20165/blah" },
			{ "sqrl.sqrljava.tech", "https://sqrl.sqrljava.tech:20165/blah" },
			{ "sqrl.sqrljava.tech", "https://sqrl.sqrljava.tech" },
			{ "sqrl.sqrljava.tech", "https://sqrl.sqrljava.tech/" },
			{ null, "https://localhost:8081/sqrlexample",  },
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
	public /* NOT private */ String expected;

	@Parameter(value = 1)
	public /* NOT private */ String input;


}
