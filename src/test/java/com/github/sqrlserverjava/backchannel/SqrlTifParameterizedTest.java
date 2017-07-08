package com.github.sqrlserverjava.backchannel;

import static com.github.sqrlserverjava.backchannel.SqrlTifTest.*;
import com.github.sqrlserverjava.backchannel.SqrlTif.SqrlTifBuilder;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class SqrlTifParameterizedTest {
	@Parameters(name = "{index}: ipmatch=({0}) expected=({1}) tifArray=({2}) )")
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			{ false, "1", new SqrlTifFlag[]{SqrlTifFlag.CURRENT_ID_MATCH }},
			{ true, "4", new SqrlTifFlag[]{}},
			{ true, "5", new SqrlTifFlag[]{SqrlTifFlag.CURRENT_ID_MATCH }},
			{ false, "C0", new SqrlTifFlag[]{SqrlTifFlag.COMMAND_FAILED, SqrlTifFlag.CLIENT_FAILURE} },
			{ true, "C4", new SqrlTifFlag[]{SqrlTifFlag.COMMAND_FAILED, SqrlTifFlag.CLIENT_FAILURE} },
		});
	}
	// @formatter:on

	@Test
	public void testIt() throws Exception {
		final SqrlTifBuilder builder = new SqrlTifBuilder(ipsMatched);
		final List<SqrlTifFlag> absentTifList = new ArrayList<>(Arrays.asList(SqrlTifFlag.values()));
		if (ipsMatched) {
			absentTifList.remove(SqrlTifFlag.IPS_MATCHED);
		}
		for (final SqrlTifFlag tif : tifList) {
			builder.addFlag(tif);
			absentTifList.remove(tif);
		}

		final SqrlTif tif = builder.createTif();

		if (ipsMatched) {
			assertTrue(isTifPresent(tif, SqrlTifFlag.IPS_MATCHED));
		}
		assertEquals(expectedValue, tif.toHexString());

		// Check present
		for (final SqrlTifFlag expectedTif : tifList) {
			assertTrue(expectedTif + " not found in int" + tif, isTifPresent(tif, expectedTif));
		}

		// Check absent
		for (final SqrlTifFlag shouldBeAbsent : absentTifList) {
			assertTrue("Found " + shouldBeAbsent + " in tif " + tif + " but shouldn't have",
					isTifAbsent(tif, shouldBeAbsent));
		}
	}


	// Instance variables and constructor are all boilerplate for Parameterized test, so put them at the bottom
	private final boolean	ipsMatched;
	// hex string without 0x
	private final String	expectedValue;
	private final SqrlTifFlag[]		tifList;

	public SqrlTifParameterizedTest(final boolean ipsMatched, final String expectedValue, final SqrlTifFlag... tifList) {
		super();
		this.ipsMatched = ipsMatched;
		this.expectedValue = expectedValue;
		this.tifList = tifList;
	}

}
