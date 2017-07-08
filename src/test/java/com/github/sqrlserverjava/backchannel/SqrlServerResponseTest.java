package com.github.sqrlserverjava.backchannel;

import java.util.Collections;

import org.junit.Test;

import com.github.sqrlserverjava.backchannel.SqrlTif.SqrlTifBuilder;

import junit.framework.TestCase;

public class SqrlServerResponseTest extends TestCase {
	// @formatter:off
	/**
	 * from decode page at grc
	 * dmVyPTENCm51dD1SX0lzc1FReWwtTEItc0IxUGlJRkd3DQp0aWY9NQ0KcXJ5PS9zcXJsP251dD1SX0lzc1FReWwtTEItc0IxUGlJRkd3DQo
		server reply decode: 
		ver=1
		nut=R_IssQQyl-LB-sB1PiIFGw
		tif=5
		qry=/sqrl?nut=R_IssQQyl-LB-sB1PiIFGw
	 */
	// @formatter:on
	@Test
	public void testBuildReply() throws Exception {
		final SqrlTif tif = new SqrlTifBuilder(true).addFlag(SqrlTifFlag.CURRENT_ID_MATCH).createTif();
		final String nut = "R_IssQQyl-LB-sB1PiIFGw";
		final String query = "/sqrl?nut=R_IssQQyl-LB-sB1PiIFGw";
		final String correlator = "alkfjaliejilsf";
		final SqrlClientReply reply = new SqrlClientReply(nut, tif, query, correlator, Collections.emptyMap());
		System.out.println(reply.toBase64());
		final String expected = "dmVyPTENCm51dD1SX0lzc1FReWwtTEItc0IxUGlJRkd3DQp0aWY9NQ0KcXJ5PS9zcXJsP251dD1SX0lzc1FReWwtTEItc0IxUGlJRkd3P251dD1SX0lzc1FReWwtTEItc0IxUGlJRkd3JmNvcj1hbGtmamFsaWVqaWxzZg0K";
		assertEquals(expected, reply.toBase64());
	}
}
