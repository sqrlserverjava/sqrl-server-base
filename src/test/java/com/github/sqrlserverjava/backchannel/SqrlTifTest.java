package com.github.sqrlserverjava.backchannel;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.sqrlserverjava.backchannel.SqrlTif.SqrlTifBuilder;

public class SqrlTifTest {

	@Test
	public void testAddSameMultiple() {
		final SqrlTifBuilder builder = new SqrlTifBuilder(false);
		builder.addFlag(SqrlTifFlag.CURRENT_ID_MATCH);
		// 2nd add should have no effect
		builder.addFlag(SqrlTifFlag.CURRENT_ID_MATCH);
		assertEquals("1", builder.createTif().toHexString());
	}

	/* **************** Util methods ***********************/

	static final List<SqrlTifFlag> buildAbsentTifList(final SqrlTifFlag[] expectedTifList) {
		final List<SqrlTifFlag> absentTifList = Arrays.asList(SqrlTifFlag.values());
		for (final SqrlTifFlag tif : expectedTifList) {
			absentTifList.remove(tif);
		}
		return absentTifList;
	}

	static final void assertTifPresent(final SqrlTif tif, final SqrlTifFlag tifToLookFor) {
		assertTrue("Expected " + tifToLookFor + " in tif " + tif, isTifPresent(tif, tifToLookFor));
	}

	static final boolean isTifPresent(final SqrlTif tif, final SqrlTifFlag tifToLookFor) {
		final int tifInt = Integer.parseInt(tif.toHexString(), 16);
		return (tifInt & tifToLookFor.getMask()) == tifToLookFor.getMask();
	}

	static final boolean isTifAbsent(final SqrlTif tif, final SqrlTifFlag tifToLookFor) {
		return !isTifPresent(tif, tifToLookFor);
	}

	public static final void assertTif(final SqrlTif tif, final SqrlTifFlag... expectedTifArray) {
		for (final SqrlTifFlag expectedTifInt : expectedTifArray) {
			assertTifPresent(tif, expectedTifInt);
		}
		final List<SqrlTifFlag> absentTifList = buildAbsentTifList(expectedTifArray);
		for (final SqrlTifFlag absentTifInt : absentTifList) {
			assertTifAbsent(tif, absentTifInt);
		}
	}

	static void assertTifAbsent(final SqrlTif tif, final SqrlTifFlag absentTifInt) {
		assertTrue("Found expected absent " + absentTifInt + " in tif " + tif, isTifAbsent(tif, absentTifInt));
	}
}
