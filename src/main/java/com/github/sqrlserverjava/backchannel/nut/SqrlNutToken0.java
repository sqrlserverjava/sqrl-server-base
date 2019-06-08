package com.github.sqrlserverjava.backchannel.nut;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.util.SqrlUtil;

public abstract class SqrlNutToken0 {
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
	public long computeExpiresAt(final SqrlConfig config) {
		final long nutValidityMillis = TimeUnit.SECONDS.toMillis(config.getNutValidityInSeconds());
		return getIssuedTimestampMillis() + nutValidityMillis;
	}

	/**
	 * Default implementation to perform a comparison of the IP address between the browser and the SQRL client
	 * 
	 * @param sqrlClientIpAddress
	 *            the IP address SQRL client which sent the backchannel request
	 * @return Optional.empty if the IP addresses matched, Optional.string if they did not including detail of the
	 *         mismatch for debugging
	 */
	public Optional<String> compareSqrlClientInetAddress(final InetAddress sqrlClientIpAddress, final SqrlConfig config)
			throws SqrlException {
		final InetAddress browserIpAddress = getBrowserIPAddress();
		if (sqrlClientIpAddress.equals(browserIpAddress)) {
			return Optional.empty();
		} else if ((sqrlClientIpAddress instanceof Inet4Address && browserIpAddress instanceof Inet6Address)
				|| (sqrlClientIpAddress instanceof Inet6Address && browserIpAddress instanceof Inet4Address)) {
			// One side saw an IPv4 while the other side saw IPv6, these will never match
			if (sqrlClientIpAddress.isLoopbackAddress() && browserIpAddress.isLoopbackAddress()) {
				return Optional.empty();
			}
			return Optional.of(SqrlUtil.buildString("IP address format mismatch, browserIP=",
					browserIpAddress.getHostAddress(), " sqrlClientIP=", sqrlClientIpAddress.getHostAddress()));
		} else {
			return Optional.of(SqrlUtil.buildString("IP address mismatch, browserIP=",
					browserIpAddress.getHostAddress(), " sqrlClientIP=", sqrlClientIpAddress.getHostAddress()));
		}
	}

	/**
	 * Helper method to convert translate from a byte to an unsigned byte represented by an int
	 */
	static int buildFormatId(final byte unsignedByte) {
		return Byte.toUnsignedInt(unsignedByte);
	}
}
