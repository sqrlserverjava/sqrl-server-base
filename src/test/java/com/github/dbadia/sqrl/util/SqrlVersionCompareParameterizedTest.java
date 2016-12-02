package com.github.dbadia.sqrl.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.dbadia.sqrl.server.util.SqrlVersionUtil;

@RunWith(Parameterized.class)
public class SqrlVersionCompareParameterizedTest {
	@Parameters(name = "{index} expected=({0}) serverVersionArray=({1}) clientVersionArray=({2})")

	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			{ 1,  new int[]{1}, new int[]{1} },
			{ 2,  new int[]{1,2}, new int[]{1,2,3}},

			// Test that client list can be unordered
			{ 3,  new int[]{1,2,3}, new int[]{3,2,1} },
			{ 3,  new int[]{1,2,3}, new int[]{2,3,1} },
			{ 3,  new int[]{1,2,3}, new int[]{1,2,3} },

			// None match
			{ null,  new int[]{2}, new int[]{1} },
			{ null,  new int[]{1}, new int[]{2} },
			{ null,  new int[]{1, 2}, new int[]{3} },
			{ null,  new int[]{1, 4}, new int[]{2, 3} },
		});
	}
	// @formatter:on

	@Test
	public void testIt() throws Exception {
		final SortedSet<Integer> serverVersionSet = convertToSortedSet(serverVersionArray);
		final Collection<Integer> clientVersionSet = Arrays.stream(clientVersionArray).boxed()
				.collect(Collectors.toList());
		final Integer result = SqrlVersionUtil.findHighestCommonVersion(serverVersionSet, clientVersionSet);
		assertEquals(expectedVersionMatch, result);
	}


	@Parameter(value = 0)
	public /* not private */ Integer	expectedVersionMatch;
	@Parameter(value = 1)
	public /* not private */ int[]	serverVersionArray;
	@Parameter(value = 2)
	public /* not private */ int[]	clientVersionArray;

	/**
	 * The API we are testing takes Set<Integer> but in the test data we use array notation for convenience; so we have
	 * this util method to convert it for us
	 *
	 * @param intArray
	 * @return
	 */
	private static final SortedSet<Integer> convertToSortedSet(final int[] intArray) {
		// TreeSet in descending order
		final SortedSet<Integer> versionSet = new TreeSet<>((final Integer o1, final Integer o2) -> o2.compareTo(o1));
		final Collection<Integer> tempCollection = Arrays.stream(intArray).boxed().collect(Collectors.toList());
		versionSet.addAll(tempCollection);
		return versionSet;
	}
}
