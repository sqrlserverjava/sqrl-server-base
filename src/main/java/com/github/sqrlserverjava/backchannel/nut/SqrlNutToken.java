package com.github.sqrlserverjava.backchannel.nut;

import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.util.SqrlUtil;

public abstract class SqrlNutToken {

	/**
	 * @return the encrypted, base64 representation of this token
	 */
	public abstract String asEncryptedBase64();

	abstract long getIssuedTimestampMillis();

	/**
	 * @return the InetAddress of device which originated the browser request. This can return null <b>if</b>
	 *         <code>compareSqrlClientInetAddress<code> is overridden
	 */
	abstract InetAddress getBrowserIPAddress();

	/**
	 * Default implementation to compute when this SQRL "nut" token will expire
	 *
	 * @param config
	 *            the SQRL config
	 * @return the time, in millis, when the token will expire
	 */
	public long computeExpiresAt(SqrlConfig config) {
		final long nutValidityMillis = TimeUnit.SECONDS.toMillis(config.getNutValidityInSeconds());
		return getIssuedTimestampMillis() + nutValidityMillis;
	}

	/**
	 * Default implementation to perform a comparision of the IP address between the browser and the SQRL client
	 * 
	 * @param sqrlClientIpAddress
	 *            the IP address SQRL client which sent the backchannel request
	 * @return Optional.empty if the IP addresses matched, Optional.string if they did not including detail of the
	 *         mismatch for debugging
	 */
	public Optional<String> compareSqrlClientInetAddress(InetAddress sqrlClientIpAddress, SqrlConfig config)
			throws SqrlException {
		InetAddress browserIpAddress = getBrowserIPAddress();
		if (sqrlClientIpAddress.equals(browserIpAddress)) {
			return Optional.empty();
		} else {
			return Optional.of(SqrlUtil.buildString("IP address mismatch, browserIP=", browserIpAddress.getHostAddress(),
					" sqrlClientIP=", sqrlClientIpAddress.getHostAddress()));
		}
	}

	/**
	 * Helper method to convert translate from a byte to an unsigned byte represented by an int
	 */
	static int buildFormatId(byte unsignedByte) {
		return Byte.toUnsignedInt(unsignedByte);
	}
}
