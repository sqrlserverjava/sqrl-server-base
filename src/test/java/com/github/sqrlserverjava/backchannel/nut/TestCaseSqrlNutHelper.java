package com.github.sqrlserverjava.backchannel.nut;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.exception.SqrlException;

public class TestCaseSqrlNutHelper {
	public static SqrlNutToken0 buildValidSqrlNut(long timestamp, SqrlConfig config)
			throws SqrlException, UnknownHostException {
		final SqrlNutToken1SingleBlockFormat nut = new SqrlNutToken1SingleBlockFormat(getLocalHost(),
				TestCaseUtil.buildSqrlConfigOperations(config), timestamp);
		return nut;
	}

	public static SqrlNutToken1SingleBlockFormat buildValidSqrlNutTokenLegacyFormat(long timestamp, SqrlConfig config)
			throws SqrlException, UnknownHostException {
		final SqrlNutToken1SingleBlockFormat nut = new SqrlNutToken1SingleBlockFormat(getLocalHost(),
				TestCaseUtil.buildSqrlConfigOperations(config), timestamp);
		return nut;
	}

	public static InetAddress getLocalHost() throws UnknownHostException {
		return InetAddress.getLocalHost();
	}
}
