package com.github.dbadia.sqrl.server.backchannel;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.TifBuilder;
import com.github.dbadia.sqrl.server.exception.SqrlInvalidRequestException;
import com.github.dbadia.sqrl.server.exception.SqrlNutTokenReplayedException;
import com.github.dbadia.sqrl.server.util.SqrlConstants;
import com.github.dbadia.sqrl.server.util.SqrlException;

/**
 * Various util methods for the {@link SqrlNutToken}
 *
 * @author Dave Badia
 */
public class SqrlNutTokenUtil {
	private static final Logger	logger				= LoggerFactory.getLogger(SqrlNutTokenUtil.class);
	private static final int	IPV6_TO_PACK_BYTES	= 4;

	private SqrlNutTokenUtil() {
		// Util class, all static methods
	}

	public static int inetAddressToInt(final URI serverUrl, final InetAddress requesterIpAddress,
			final SqrlConfig config)
					throws SqrlException {
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
				return packInet6Address((Inet6Address) requesterIpAddress, config);
			} else {
				throw new SqrlException("Unknown InetAddress type of " + requesterIpAddress.getClass());
			}
		} else if (serverUrlScheme.equals(SqrlConstants.SCHEME_HTTP)
				|| serverUrlScheme.equals(SqrlConstants.SCHEME_QRL)) {
			return 0;
		} else {
			throw new SqrlException("Unsupported scheme " + serverUrlScheme);
		}
	}

	public static boolean validateInetAddress(final InetAddress requesterIpAddress, final int inetInt,
			final SqrlConfig config)
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
			final int currentIpPacked = packInet6Address((Inet6Address) requesterIpAddress, config);
			return currentIpPacked == inetInt;
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
	 * Validates the {@link SqrlNutToken} from the {@link SqrlClientRequest} by:<br/>
	 * <li>1. check the timestamp embedded in the Nut has expired
	 * <li>2. call {@link SqrlPersistence} to see if the Nut has been replayed
	 *
	 * @param correlator
	 *
	 * @param nutToken
	 *            the Nut to be validated
	 * @param tifBuilder
	 * @throws SqrlException
	 *             if any validation fails or if persistence fails
	 */
	public static void validateNut(final String correlator, final SqrlNutToken nutToken, final SqrlConfig config,
			final SqrlPersistence sqrlPersistence, final TifBuilder tifBuilder) throws SqrlException {
		final long nutExpiryMs = computeNutExpiresAt(nutToken, config);
		final long now = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			final Date nutExpiry = new Date(nutExpiryMs);
			logger.debug("{} Now={}, nutExpiry={}", SqrlLoggingUtil.getLogHeader(), new Date(now), nutExpiry);
		}
		if (now > nutExpiryMs) {
			tifBuilder.addFlag(SqrlTif.TIF_TRANSIENT_ERROR);
			throw new SqrlInvalidRequestException(SqrlLoggingUtil.getLogHeader() + "Nut expired by "
					+ (nutExpiryMs - now) + "ms, nut timetamp ms=" + nutToken.getIssuedTimestampMillis()
					+ ", expiry is set to " + config.getNutValidityInSeconds() + " seconds");
		}
		// Mark the token as used since we will process this request
		final String nutTokenString = nutToken.asSqrlBase64EncryptedNut();
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
		return nutToken.getIssuedTimestampMillis() + nutValidityMillis;
	}

	private static int packInet6Address(final Inet6Address requesterIpAddress, final SqrlConfig config)
			throws SqrlException {
		// Compress per https://www.grc.com/sqrl/server.htm
		// IPv6 addresses can be safely compressed to 32 bits by hashing the full IPv6 IP with a secret salt and
		// retaining the least significant 32 bits of the hash result. The hash's salt can be the AES key
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			// salt with aes key bytes
			messageDigest.update(config.getAESKeyBytes());
			final byte[] result = messageDigest.digest(requesterIpAddress.getAddress());
			// Get the least significant 32 bits of the hash result
			final byte[] toPack = new byte[IPV6_TO_PACK_BYTES];
			final int start = result.length - IPV6_TO_PACK_BYTES;
			System.arraycopy(result, start, toPack, 0, IPV6_TO_PACK_BYTES);
			final int packed = SqrlNutTokenUtil.pack(toPack);
			logger.debug("IPV6 {} compressed and packed to {}", requesterIpAddress, packed);
			return packed;
		} catch (final NoSuchAlgorithmException e) {
			throw new SqrlException("Error occured while hashing IPV6 address", e);
		}
	}
}
