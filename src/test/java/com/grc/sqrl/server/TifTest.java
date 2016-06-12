package com.grc.sqrl.server;

import com.grc.sqrl.server.backchannel.SqrlTif;
import com.grc.sqrl.server.backchannel.SqrlTif.TifBuilder;

import junit.framework.TestCase;

public class TifTest extends TestCase {

	public void testTif_IpsMatched() throws Exception {
		final SqrlTif tif = new TifBuilder(true).createTif();
		final boolean result = (tif.getTifInt() & SqrlTif.TIF_IPS_MATCHED) == SqrlTif.TIF_IPS_MATCHED;
		assertTrue(result);
	}

	public void testTif_IpsNotMatched() throws Exception {
		final SqrlTif tif = new TifBuilder(false).createTif();
		final boolean result = (tif.getTifInt() & SqrlTif.TIF_IPS_MATCHED) == SqrlTif.TIF_IPS_MATCHED;
		assertFalse(result);
	}
}
