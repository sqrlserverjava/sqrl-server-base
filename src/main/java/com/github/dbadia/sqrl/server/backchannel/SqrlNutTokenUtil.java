package com.github.dbadia.sqrl.server.backchannel;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlConstants;
import com.github.dbadia.sqrl.server.SqrlException;
import com.github.dbadia.sqrl.server.SqrlNutTokenReplayedException;
import com.github.dbadia.sqrl.server.SqrlPersistence;

/**
 * Various util methods for the {@link SqrlNutToken}
 * 
 * @author Dave Badia
 */
public class SqrlNutTokenUtil {
	private static final Logger logger = LoggerFactory.getLogger(SqrlNutTokenUtil.class);

	private SqrlNutTokenUtil() {
		// Util class, all static methods
	}

	static int inetAddressToInt(final URI serverUrl, final InetAddress requesterIpAddress) throws SqrlException {
		// From https://www.grc.com/sqrl/server.htm
		// Although this 128-bit total nut size only provides 32 bits for an IPv4 IP address, our purpose is only to
		// perform a match/no-match comparison to detect same-device phishing attacks. Therefore, any 128-bit IPv6
		// addresses can be safely �compressed� to 32 bits by hashing the full IPv6 IP with a secret salt and
		// retaining the least significant 32 bits of the hash result. The hash's salt can be the same AES key being
		// used to encrypt and decrypt the nut.
		final String serverUrlScheme = serverUrl.getScheme();
		if (serverUrlScheme.equals(SqrlConstants.SCHEME_HTTPS) || serverUrlScheme.equals(SqrlConstants.SCHEME_SQRL)) {
			if (requesterIpAddress instanceof Inet4Address) {
				return SqrlNutTokenUtil.pack(requesterIpAddress.getAddress());
			} else if (requesterIpAddress instanceof Inet6Address) {
				// TODO: IPv6 support
				throw new SqrlException("Inet6Address not implemented");
			} else {
				throw new SqrlException("Unknown InetAddress type of " + requesterIpAddress.getClass());
			}
		} else if (serverUrlScheme.equals(SqrlConstants.SCHEME_HTTP) || serverUrlScheme.equals(SqrlConstants.SCHEME_QRL)) {
			return 0;
		} else {
			throw new SqrlException("Unsupported scheme " + serverUrlScheme);
		}
	}

	static boolean validateInetAddress(final InetAddress requesterIpAddress, final int inetInt)
			throws SqrlException {
		// From https://www.grc.com/sqrl/server.htm
		// Although this 128-bit total nut size only provides 32 bits for an IPv4 IP address, our purpose is only to
		// perform a match/no-match comparison to detect same-device phishing attacks. Therefore, any 128-bit IPv6
		// addresses can be safely �compressed� to 32 bits by hashing the full IPv6 IP with a secret salt and
		// retaining the least significant 32 bits of the hash result. The hash's salt can be the same AES key being
		// used to encrypt and decrypt the nut.
		if (inetInt == 0) {
			return false;
		}
		if (requesterIpAddress instanceof Inet4Address) {

			final byte[] bytes = SqrlNutTokenUtil.unpack(inetInt);
			try {
				final InetAddress fromNut = InetAddress.getByAddress(bytes);
				return requesterIpAddress.equals(fromNut);
			} catch (final UnknownHostException e) {
				throw new SqrlException("Got UnknownHostException for inet " + inetInt, e);
			}
		} else if (requesterIpAddress instanceof Inet6Address) {
			// TODO: IPv6 support
			throw new SqrlException("Inet6Address not implemented");
		} else {
			throw new SqrlException("Unknown InetAddress type of " + requesterIpAddress.getClass());
		}

	}

	// From https://stackoverflow.com/questions/2241229/going-from-127-0-0-1-to-2130706433-and-back-again
	static int pack(final byte[] bytes) {
		int theInt = 0;
		for (int i = 0; i < bytes.length; i++) {
			theInt <<= 8;
			theInt |= bytes[i] & 0xff;
		}
		return theInt;
	}

	// From https://stackoverflow.com/questions/2241229/going-from-127-0-0-1-to-2130706433-and-back-again
	static byte[] unpack(final int theInt) {
		return new byte[] { (byte) ((theInt >>> 24) & 0xff), (byte) ((theInt >>> 16) & 0xff),
				(byte) ((theInt >>> 8) & 0xff), (byte) ((theInt) & 0xff) };
	}

	/**
	 * Validates the {@link SqrlNutToken} from the {@link SqrlRequest} by:<br/>
	 * <li>1. check the timestamp embedded in the Nut has expired
	 * <li>2. call {@link SqrlPersistence} to see if the Nut has been replayed
	 * 
	 * @param nutToken
	 *            the Nut to be validated
	 * @throws SqrlException
	 *             if any validation fails or if persistence fails
	 */
	static void validateNut(final SqrlNutToken nutToken, final SqrlConfig config, final SqrlPersistence sqrlPersistence)
			throws SqrlException {
		final long nutExpiryMs = computeNutExpiresAt(nutToken, config);
		final long now = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			final Date nutExpiry = new Date(nutExpiryMs);
			logger.debug("{} Now={}, nutExpiry={}", SqrlLoggingUtil.getLogHeader(), new Date(now), nutExpiry);
		}
		if (now > nutExpiryMs) {
			// TODO: set a TIF
			throw new SqrlInvalidRequestException(
					SqrlLoggingUtil.getLogHeader() + "Nut expired by " + (nutExpiryMs - now)
					+ "ms, nut timetamp ms=" + nutToken.getIssuedTimestamp() + ", expiry is set to "
					+ config.getNutValidityInSeconds() + " seconds");
		}
		// Mark the token as used since we will process this request
		final String nutTokenString = nutToken.asSqBase64EncryptedNut();
		if (sqrlPersistence.hasTokenBeenUsed(nutTokenString)) {
			throw new SqrlNutTokenReplayedException(
					SqrlLoggingUtil.getLogHeader() + "Nut token was replayed " + nutToken);
		}
		final Date nutExpiry = new Date(nutExpiryMs);
		sqrlPersistence.markTokenAsUsed(nutTokenString, nutExpiry);
	}

	/**
	 * Computes when a given SQRL "nut" nonce token will expire
	 * 
	 * @param nutToken
	 *            the token for which expiration time will be computed
	 * @param config
	 *            the SQRL config
	 * @return the time, in millis, when the token will expire
	 */
	public static long computeNutExpiresAt(final SqrlNutToken nutToken, final SqrlConfig config) {
		final long nutValidityMillis = config.getNutValidityInSeconds() * 1000L;
		return nutToken.getIssuedTimestamp() + nutValidityMillis;
	}
}
