package com.github.dbadia.sqrl.server.backchannel;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import com.github.dbadia.sqrl.server.backchannel.SqrlTif;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.SqrlTifBuilder;

public class SqrlTifConstructorTest {

	@Test
	public void testTif_IpsMatched() throws Exception {
		final SqrlTif tif = new SqrlTifBuilder(true).createTif();
		assertTrue(isTifPresent(tif, SqrlTif.TIF_IPS_MATCHED));
		assertEquals(4, tif.toHexInt());
	}

	@Test
	public void testTif_IpsNotMatched() throws Exception {
		final SqrlTif tif = new SqrlTifBuilder(false).createTif();
		assertTrue(isTifAbsent(tif, SqrlTif.TIF_IPS_MATCHED));
		assertEquals(0, tif.toHexInt());
	}

	static final boolean isTifPresent(final SqrlTif tif, final int tifToLookFor) {
		return (tif.toHexInt() & tifToLookFor) == tifToLookFor;
	}

	static final boolean isTifAbsent(final SqrlTif tif, final int tifToLookFor) {
		return !isTifPresent(tif, tifToLookFor);
	}
}
