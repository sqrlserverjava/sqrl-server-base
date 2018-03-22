package com.github.sqrlserverjava.backchannel.nut;

import java.net.InetAddress;

import org.junit.Test;

import com.github.sqrlserverjava.SqrlConfigOperations;
import com.github.sqrlserverjava.TestCaseUtil;

public class SqrlNutTokenEmbeddedTest {

	@Test
	public void testIt() throws Exception {
		InetAddress inetAddress = InetAddress.getLocalHost();
		String correlator = "cQCw_mQLJlVOC74y83JtNMinfZ8r3wEUQWfy7JhuRWk";
		String browserLoginUrl = "https://sqrljava.com:20000/sqrlexample/app";
		SqrlConfigOperations configOps = TestCaseUtil.buildSqrlConfigOperations(TestCaseUtil.buildTestSqrlConfig());
		SqrlNutTokenEmbedded nutToken = new SqrlNutTokenEmbedded(inetAddress, configOps, System.currentTimeMillis(),
				correlator, browserLoginUrl);
		String encoded = nutToken.asEncryptedBase64();
		System.out.println(nutToken.asEncryptedBase64());

		nutToken = new SqrlNutTokenEmbedded(configOps, encoded);
		System.out.println("issued=" + nutToken.getIssuedTimestampMillis());

	}
}
