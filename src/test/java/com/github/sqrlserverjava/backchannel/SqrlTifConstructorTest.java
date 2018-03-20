package com.github.sqrlserverjava.backchannel;

import static com.github.sqrlserverjava.backchannel.SqrlTifTest.isTifAbsent;
import static com.github.sqrlserverjava.backchannel.SqrlTifTest.isTifPresent;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import com.github.sqrlserverjava.backchannel.SqrlTifResponse.SqrlTifResponseBuilder;

public class SqrlTifConstructorTest {

	@Test
	public void testIpsMatched() throws Exception {
		final SqrlTifResponse tif = new SqrlTifResponseBuilder(true).createTif();
		assertTrue(isTifPresent(tif, SqrlTifFlag.IPS_MATCHED));
		assertEquals("4", tif.toHexString());
	}

	@Test
	public void testIpsNotMatched() throws Exception {
		final SqrlTifResponse tif = new SqrlTifResponseBuilder(false).createTif();
		assertTrue(isTifAbsent(tif, SqrlTifFlag.IPS_MATCHED));
		assertEquals("0", tif.toHexString());
	}

}
