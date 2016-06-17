package com.github.dbadia.sqrl.server;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import com.github.dbadia.sqrl.server.backchannel.SqrlTif;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.TifBuilder;

public class TifTest {

	@Test
	public void testTif_IpsMatched() throws Exception {
		final SqrlTif tif = new TifBuilder(true).createTif();
		assertTrue(isTifPresent(tif, SqrlTif.TIF_IPS_MATCHED));
		assertEquals(4, tif.getTifInt());
	}

	@Test
	public void testTif_IpsNotMatched() throws Exception {
		final SqrlTif tif = new TifBuilder(false).createTif();
		assertFalse(isTifPresent(tif, SqrlTif.TIF_IPS_MATCHED));
		assertEquals(0, tif.getTifInt());
	}

	@Test
	public void testTif_IpsMatched_And_Ident_Found() throws Exception {
		final SqrlTif tif = new TifBuilder(true).setFlag(SqrlTif.TIF_CURRENT_ID_MATCH).createTif();
		assertTrue(isTifPresent(tif, SqrlTif.TIF_IPS_MATCHED));
		assertTrue(isTifPresent(tif, SqrlTif.TIF_CURRENT_ID_MATCH));
		assertEquals(5, tif.getTifInt());
	}

	private static final boolean isTifPresent(final SqrlTif tif, final int tifToLookFor) {
		return (tif.getTifInt() & tifToLookFor) == tifToLookFor;
	}
}
