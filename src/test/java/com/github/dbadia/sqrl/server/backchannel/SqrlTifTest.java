package com.github.dbadia.sqrl.server.backchannel;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.github.dbadia.sqrl.server.backchannel.SqrlTif.SqrlTifBuilder;

public class SqrlTifTest {

	@Test
	public void testAddSameMultiple() {
		final SqrlTifBuilder builder = new SqrlTifBuilder(false);
		builder.addFlag(SqrlTif.TIF_CURRENT_ID_MATCH);
		// 2nd add should have no effect
		builder.addFlag(SqrlTif.TIF_CURRENT_ID_MATCH);
		assertEquals("1", builder.createTif().toHexString());
	}

	/* **************** Util methods ***********************/

	static final List<Integer> buildAbsentTifList(final int[] expectedTifArray) {
		final List<Integer> absentTifList = SqrlTif.getAllTifs();
		for (final int tif : expectedTifArray) {
			absentTifList.remove(Integer.valueOf(tif));
		}
		return absentTifList;
	}

	static final void assertTifPresent(final SqrlTif tif, final int tifToLookFor) {
		assertTrue("Expected " + tifToLookFor + " in tif " + tif, isTifPresent(tif, tifToLookFor));
	}

	static final boolean isTifPresent(final SqrlTif tif, final int tifToLookFor) {
		final int tifInt = Integer.parseInt(tif.toHexString(), 16);
		return (tifInt & tifToLookFor) == tifToLookFor;
	}

	static final boolean isTifAbsent(final SqrlTif tif, final int tifToLookFor) {
		return !isTifPresent(tif, tifToLookFor);
	}

	public static final void assertTif(final SqrlTif tif, final int... expectedTifArray) {
		for (final int expectedTifInt : expectedTifArray) {
			assertTifPresent(tif, expectedTifInt);
		}
		final List<Integer> absentTifList = buildAbsentTifList(expectedTifArray);
		for (final int absentTifInt : absentTifList) {
			assertTifAbsent(tif, absentTifInt);
		}
	}

	static void assertTifAbsent(final SqrlTif tif, final int absentTifInt) {
		assertTrue("Found expected absent " + absentTifInt + " in tif " + tif, isTifAbsent(tif, absentTifInt));
	}
}
