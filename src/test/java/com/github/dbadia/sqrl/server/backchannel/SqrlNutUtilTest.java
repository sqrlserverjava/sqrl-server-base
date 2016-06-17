package com.github.dbadia.sqrl.server.backchannel;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlException;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.TCUtil;

import junitx.framework.ArrayAssert;
import junitx.framework.ObjectAssert;

public class SqrlNutUtilTest {
	private SqrlConfig config = TCUtil.buildValidSqrlConfig();
	private SqrlPersistence persistence = TCUtil.buildValidsqrlPersistence();
	@Before
	public void setup() throws Exception {
		config = TCUtil.buildValidSqrlConfig();
		persistence = TCUtil.buildValidsqrlPersistence();
	}

	/* *************** URLs with http:// or https:// *****************/
	@Test
	public void testInetAddressToInt_ipv4_http() throws Throwable {
		final URI url = new URI("http://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		assertTrue(address instanceof Inet4Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals("http (insecure) should result in zero", 0, actual);
	}

	@Test
	public void testInetAddressToInt_ipv4_https() throws Throwable {
		final URI url = new URI("https://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		assertTrue(address instanceof Inet4Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals(1160964150, actual);
	}

	@Test
	public void testInetAddressToInt_ipv6_http() throws Throwable {
		final URI url = new URI("http://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
		assertTrue(address instanceof Inet6Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals("http (insecure) should result in zero", 0, actual);
	}

	@Test
	public void testInetAddressToInt_ipv6_https() throws Throwable {
		final URI url = new URI("https://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
		assertTrue(address instanceof Inet6Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals(-461733409, actual);
	}

	/* *************** URLs with qrl:// or sqrl:// *****************/
	@Test
	public void testInetAddressToInt_ipv4_qrl() throws Throwable {
		final URI url = new URI("qrl://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		assertTrue(address instanceof Inet4Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals("qrl (insecure) should result in zero", 0, actual);
	}

	@Test
	public void testInetAddressToInt_ipv4_sqrl() throws Throwable {
		final URI url = new URI("sqrl://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		assertTrue(address instanceof Inet4Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals(1160964150, actual);
	}

	@Test
	public void testInetAddressToInt_ipv6_qrl() throws Throwable {
		final URI url = new URI("qrl://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
		assertTrue(address instanceof Inet6Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals("qrl (insecure) should result in zero", 0, actual);
	}

	@Test
	public void testInetAddressToInt_ipv6_sqrl() throws Throwable {
		// final URI url = new URI("sqrl://davetest.com/sqrl");
		// final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
		// assertTrue(address instanceof Inet6Address);
		// final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address);
		// assertEquals(1160964150, actual); // TODO:
	}

	@Test
	public void testValidateInetAddress_ipv4_pass() throws Throwable {
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		final boolean actual = SqrlNutTokenUtil.validateInetAddress(address, 1160964150);
		assertTrue(actual);
	}

	@Test
	public void testValidateInetAddress_ipv4_fail_1() throws Throwable {
		final InetAddress address = InetAddress.getByName("198.105.254.130");
		final boolean actual = SqrlNutTokenUtil.validateInetAddress(address, 1160964150);
		assertFalse(actual);
	}

	public void testValidateInetAddress_ipv4_fail_2() throws Throwable {
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		final boolean actual = SqrlNutTokenUtil.validateInetAddress(address, 1511609640);
		assertFalse(actual);
	}

	@Test
	public void testPack() throws Throwable {
		final byte[] bytes = { 27, -78, -123, 54 };
		final int actual = SqrlNutTokenUtil.pack(bytes);
		assertEquals(464684342, actual);
	}
	@Test
	public void testUnpack_ipv4() throws Throwable {
		final byte[] actual = SqrlNutTokenUtil.unpack(464684342);
		ArrayAssert.assertEquals(new byte[] { 27, -78, -123, 54 }, actual);
	}

	@Test
	public void testInetAddressToInt_http() throws Exception {
		final URI uri = new URI("http://grc.com");
		final InetAddress requesterIpAddress = InetAddress.getByName("127.0.0.1");
		final int inetInt = SqrlNutTokenUtil.inetAddressToInt(uri, requesterIpAddress, config);
		assertEquals(0, inetInt);
	}

	@Test
	public void testInetAddressToInt_https() throws Exception {
		final InetAddress requesterIpAddress = InetAddress.getByName("127.0.0.1");
		final URI uri = new URI("https://grc.com");
		final int inetInt = SqrlNutTokenUtil.inetAddressToInt(uri, requesterIpAddress, config);
		assertEquals(2130706433, inetInt);
	}

	@Test
	public void testValidateInetAddress_http() throws Exception {
		final InetAddress requesterIpAddress = InetAddress.getByName("127.0.0.1");
		final int inetInt = 0;
		assertFalse(SqrlNutTokenUtil.validateInetAddress(requesterIpAddress, inetInt));
	}

	@Test
	public void testIntToInetAddress_httpsMatch() throws Exception {
		final InetAddress requesterIpAddress = InetAddress.getByName("127.0.0.1");
		final int inetInt = 2130706433;
		assertTrue(SqrlNutTokenUtil.validateInetAddress(requesterIpAddress, inetInt));
	}

	@Test
	public void testIntToInetAddress_httpsMismatch() throws Exception {
		final InetAddress requesterIpAddress = InetAddress.getByName("192.168.1.1");
		final int inetInt = 2130706433;
		assertFalse(SqrlNutTokenUtil.validateInetAddress(requesterIpAddress, inetInt));
	}

	/* ************ Nut expiry tests *********************/
	@Test
	public void testNutExpiry() throws SqrlException {
		final int nutValidityInSeconds = 1000;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final SqrlNutToken nut = TCUtil.buildValidSqrlNut(config);
		final long nutIssuedTime = nut.getIssuedTimestamp();
		final long expiresAt = SqrlNutTokenUtil.computeNutExpiresAt(nut, config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
	}

	@Test
	public void testNutExpiry2() throws SqrlException {
		final int nutValidityInSeconds = 180;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final SqrlNutToken nut = TCUtil.buildValidSqrlNut(config);
		final long nutIssuedTime = nut.getIssuedTimestamp();
		final long expiresAt = SqrlNutTokenUtil.computeNutExpiresAt(nut, config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
	}

	/**
	 * timetsamp is unsigned int with second precision which means that the timestamp will go up to
	 * 2106-02-07T02:28:15-0400
	 * 
	 * @throws SqrlException
	 */
	@Test
	public void testComputeNutExpiresInJan2016() throws SqrlException {
		final int nutValidityInSeconds = 1000;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final LocalDateTime tokenIssuedAt = LocalDateTime.parse("2016-01-03T10:15:30");
		final SqrlNutToken nut = TCUtil.buildValidSqrlNut(config, tokenIssuedAt);
		final long nutIssuedTime = nut.getIssuedTimestamp();
		final long expiresAt = SqrlNutTokenUtil.computeNutExpiresAt(nut, config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
		assertEquals(1452816000L, expiresAt);
	}

	@Test
	public void testValidateNutTimestamp_ExpiredInJan2016() throws SqrlException {
		final int nutValidityInSeconds = 1000;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final LocalDateTime tokenIssuedAt = LocalDateTime.parse("2016-01-03T10:15:30");
		final SqrlNutToken nutToken = TCUtil.buildValidSqrlNut(config, tokenIssuedAt);
		try {
			SqrlNutTokenUtil.validateNut(nutToken, config, persistence);
			fail("Exceptio expected");
		} catch (final Exception e) {
			ObjectAssert.assertInstanceOf(SqrlException.class, e);
		}
	}
}
