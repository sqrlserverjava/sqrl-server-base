package com.github.dbadia.sqrl.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.dbadia.sqrl.server.util.SqrlVersionUtil;

import junitx.framework.ArrayAssert;

@RunWith(Parameterized.class)
public class SqrlVersionParseParameterizedTest {
	@Parameters(name = "{index}: versionString=({0})") // no point in printing the array
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			{ "1",  	new int[]{ 1 }},
			{ "1-1",  	new int[]{ 1 }},
			{ "1-2",  	new int[]{ 1,2 }},
			{ "1,2",  	new int[]{ 1,2 }},
			{ "1,2,3",  new int[]{ 1,2,3 }},
			{ "1,2-3",  new int[]{ 1,2,3 }},

			{ "2-3,1",  new int[]{ 2,3,1 }},

			// Ignore non numeric data and parse the rest
			{ "1,b,2",  		new int[]{ 1,2 }},
			{ "abc,1,b,2",  	new int[]{ 1,2 }},
			{ "a-c,1,b,2",  	new int[]{ 1,2 }},
		});
	}
	// @formatter:on

	@Test
	public void testIt() throws Exception {
		final Collection<Integer> versionParsedSet = SqrlVersionUtil.parseClientVersionString(versionStringToParse);
		final int[] versionParsedArray = versionParsedSet.stream().mapToInt(i -> i).toArray();
		ArrayAssert.assertEquals(expectedResult, versionParsedArray);
	}


	@Parameter(value = 0)
	public /* not private */ String	versionStringToParse;
	@Parameter(value = 1)
	public /* not private */ int[]	expectedResult;

}
