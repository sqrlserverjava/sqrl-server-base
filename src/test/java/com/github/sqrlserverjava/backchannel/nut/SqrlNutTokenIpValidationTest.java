package com.github.sqrlserverjava.backchannel.nut;

import org.junit.Before;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.TestCaseUtil;

public class SqrlNutTokenIpValidationTest {
	private SqrlConfig		config		= TestCaseUtil.buildTestSqrlConfig();
	private SqrlPersistence persistence = null;

	@Before
	public void setup() throws Exception {
		config = TestCaseUtil.buildTestSqrlConfig();
		persistence = TestCaseUtil.createEmptySqrlPersistence();
	}

	// TODO: convert to parameterized that does genric validation
	//
	//
	// @Test
	// public void testValidateInetAddress_ipv4_pass() throws Throwable {
	// final InetAddress address = InetAddress.getByName("69.50.232.54");
	// final boolean actual = !SqrlNutTokenLegacyFormat.validateInetAddress(address, 1160964150, config).isPresent();
	// assertTrue(actual);
	// }
	//
	// @Test
	// public void testValidateInetAddress_ipv4_fail_1() throws Throwable {
	// final InetAddress address = InetAddress.getByName("198.105.254.130");
	// final boolean actual = !SqrlNutTokenLegacyFormat.validateInetAddress(address, 1160964150, config).isPresent();
	// assertFalse(actual);
	// }
	//
	// public void testValidateInetAddress_ipv4_fail_2() throws Throwable {
	// final InetAddress address = InetAddress.getByName("69.50.232.54");
	// final boolean actual = !SqrlNutTokenLegacyFormat.validateInetAddress(address, 1511609640, config).isPresent();
	// assertFalse(actual);
	// }
	//
	// @Test
	// public void testValidateInetAddress_ipv6_pass() throws Throwable {
	// final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
	// final boolean actual = !SqrlNutTokenLegacyFormat.validateInetAddress(address, -461733409, config).isPresent();
	// assertTrue(actual);
	// }
	//
	// @Test
	// public void testValidateInetAddress_ipv6_fail_1() throws Throwable {
	// final InetAddress address = InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
	// final boolean actual = !SqrlNutTokenLegacyFormat.validateInetAddress(address, 1160964150, config).isPresent();
	// assertFalse(actual);
	// }
	//
	// public void testValidateInetAddress_ipv6_fail_2() throws Throwable {
	// final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
	// final boolean actual = !SqrlNutTokenLegacyFormat.validateInetAddress(address, 1511609640, config).isPresent();
	// assertFalse(actual);
	// }


}
