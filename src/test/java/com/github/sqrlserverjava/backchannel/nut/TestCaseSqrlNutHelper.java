package com.github.sqrlserverjava.backchannel.nut;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.exception.SqrlException;

public class TestCaseSqrlNutHelper {
	public static SqrlNutToken buildValidSqrlNut(long timestamp, SqrlConfig config)
			throws SqrlException, UnknownHostException {
		final SqrlNutTokenSingleBlockFormat nut = new SqrlNutTokenSingleBlockFormat(getLocalHost(),
				TestCaseUtil.buildSqrlConfigOperations(config), timestamp);
		return nut;
	}

	public static SqrlNutTokenSingleBlockFormat buildValidSqrlNutTokenLegacyFormat(long timestamp, SqrlConfig config)
			throws SqrlException, UnknownHostException {
		final SqrlNutTokenSingleBlockFormat nut = new SqrlNutTokenSingleBlockFormat(getLocalHost(),
				TestCaseUtil.buildSqrlConfigOperations(config), timestamp);
		return nut;
	}

	public static InetAddress getLocalHost() throws UnknownHostException {
		return InetAddress.getLocalHost();
	}
}
