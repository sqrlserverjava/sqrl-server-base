package com.github.dbadia.sqrl.server.backchannel;

import static com.github.dbadia.sqrl.server.backchannel.SqrlTifTest.isTifAbsent;
import static com.github.dbadia.sqrl.server.backchannel.SqrlTifTest.isTifPresent;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.dbadia.sqrl.server.backchannel.SqrlTif.SqrlTifBuilder;

@RunWith(Parameterized.class)
public class SqrlTifParameterizedTest {
	@Parameters(name = "{index}: ipmatch=({0}) expected=({1}) tifArray=({2}) )")
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			{ false, "1", new int[]{SqrlTif.TIF_CURRENT_ID_MATCH }},
			{ true, "4", new int[]{}},
			{ true, "5", new int[]{SqrlTif.TIF_CURRENT_ID_MATCH }},
			{ false, "C0", new int[]{SqrlTif.TIF_COMMAND_FAILED, SqrlTif.TIF_CLIENT_FAILURE} },
			{ true, "C4", new int[]{SqrlTif.TIF_COMMAND_FAILED, SqrlTif.TIF_CLIENT_FAILURE} },
		});
	}
	// @formatter:on

	@Test
	public void testIt() throws Exception {
		final SqrlTifBuilder builder = new SqrlTifBuilder(ipsMatched);
		final List<Integer> absentTifList = SqrlTif.getAllTifs();
		if (ipsMatched) {
			absentTifList.remove(Integer.valueOf(SqrlTif.TIF_IPS_MATCHED));
		}
		for (final int tif : tifList) {
			builder.addFlag(tif);
			absentTifList.remove(Integer.valueOf(tif));
		}

		final SqrlTif tif = builder.createTif();

		if (ipsMatched) {
			assertTrue(isTifPresent(tif, SqrlTif.TIF_IPS_MATCHED));
		}
		assertEquals(expectedValue, tif.toHexString());

		// Check present
		for (final int expectedTif : tifList) {
			assertTrue(expectedTif + " not found in int" + tif, isTifPresent(tif, expectedTif));
		}

		// Check absent
		for (final int shouldBeAbsent : absentTifList) {
			assertTrue("Found " + shouldBeAbsent + " in tif " + tif + " but shouldn't have",
					isTifAbsent(tif, shouldBeAbsent));
		}
	}


	// Instance variables and constructor are all boilerplate for Parameterized test, so put them at the bottom
	private final boolean	ipsMatched;
	// hex string without 0x
	private final String	expectedValue;
	private final int[]		tifList;

	public SqrlTifParameterizedTest(final boolean ipsMatched, final String expectedValue, final int... tifList) {
		super();
		this.ipsMatched = ipsMatched;
		this.expectedValue = expectedValue;
		this.tifList = tifList;
	}

}
