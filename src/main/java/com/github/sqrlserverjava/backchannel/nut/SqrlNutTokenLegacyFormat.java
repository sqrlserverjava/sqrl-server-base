package com.github.sqrlserverjava.backchannel.nut;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlConfigOperations;
import com.github.sqrlserverjava.backchannel.SqrlTifFlag;
import com.github.sqrlserverjava.exception.SqrlClientRequestProcessingException;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.exception.SqrlInvalidRequestException;
import com.github.sqrlserverjava.util.SqrlConfigHelper;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * "Nut" token format matching the size and data layout as suggested by the SQRL spec. This is not the preferred format
 * but is retained until test case data is converted to use a newer format
 * 
 * This token format is deprecated for the following reasons:
 * <li/>Use of ECB padding which is inherently insecure (CBC with a static IV is just as bad and this format does not
 * provide space for an IV
 * <li/>Requires server side state (that is, database entries) when displaying the login page
 * <li/>This token is encrypted but is <b>not</b> signed and therefore subject to manipulation
 * <li/>Can only store part of an IPv6 address, which makes IP mismatch debugging difficult
 *
 * @author Dave Badia
 * @deprecated
 */
@Deprecated
public class SqrlNutTokenLegacyFormat extends SqrlNutToken {
	private static final Logger			logger				= LoggerFactory.getLogger(SqrlNutTokenLegacyFormat.class);
	private static final int			IPV6_TO_PACK_BYTES	= 4;
	private static final AtomicInteger	COUNTER				= new AtomicInteger(0);

	/**
	 * This format does not store the ID anywhere, it is identified by size
	 */
	static final int		FORMAT_ID	= buildFormatId((byte) 0);

	private final int		inetInt;
	private final int		counter;
	private final long		issuedTimestamp;
	private final int		randomInt;
	/**
	 * The encrypted nut in base64url format as it appeared in the query string
	 */
	private final String	base64UrlEncryptedNut;


	/**
	 *
	 * @param inetInt
	 * @param config
	 * @param counter
	 * @param timestamp
	 *            The time at which the Nut was created, typically {@link System#currentTimeMillis()}. Note that the
	 *            data in the Nut is only stored with second granularity
	 * @param randomInt
	 * @throws SqrlException
	 */
	public SqrlNutTokenLegacyFormat(InetAddress browserIPAddress, SqrlConfigOperations configOperations,
			long timestamp) throws SqrlException {
		SqrlConfig config = configOperations.getSqrlConfig();
		this.inetInt = inetAddressToInt(browserIPAddress, config);
		this.counter = COUNTER.incrementAndGet();
		// Convert the timestamp param from millis precision to second precision
		this.issuedTimestamp = (timestamp / 1000) * 1000;
		this.randomInt = config.getSecureRandom().nextInt();
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final DataOutputStream nutOs = new DataOutputStream(baos)) {
			// Build the nut
			// A) 32 bits: user's connection IP address if secured, 0.0.0.0 if non-secured.
			nutOs.writeInt(inetInt);
			// B) 32 bits: UNIX-time timestamp incrementing once per second.
			// Note this is a 32-bit unsigned int, not a long. We have second granularity
			final int unixTimeInSeconds = (int) (this.issuedTimestamp / 1000);
			nutOs.writeInt(unixTimeInSeconds);
			// C) 32 bits: up-counter incremented once for every SQRL link generated.
			nutOs.writeInt(counter);
			// D) 31 bits: pseudo-random noise from system source.
			nutOs.writeInt(randomInt);
			// D2) FUTURE: 1 bit: flag bit to indicate source: QRcode or URL click

			final byte[] nutBytes = baos.toByteArray();
			// Encrypt and encode the nut
			final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, configOperations.getAESKey());
			final byte[] encrypted = cipher.doFinal(nutBytes);
			this.base64UrlEncryptedNut = SqrlUtil.sqrlBase64UrlEncode(encrypted);
		} catch (final GeneralSecurityException e) {
			throw new SqrlException(e, "Error during nut encryption");
		} catch (final IOException e) {
			throw new SqrlException(e, "IO exception during write");
		}
	}

	public SqrlNutTokenLegacyFormat(final SqrlConfigOperations configOps, final String sqBase64EncryptedNut)
			throws SqrlClientRequestProcessingException {
		this.base64UrlEncryptedNut = sqBase64EncryptedNut;
		// Decrypt the nut
		byte[] cleartextBytes = null;
		try {
			final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, configOps.getAESKey());
			final byte[] cipherbytes = SqrlUtil.base64UrlDecodeDataFromSqrlClient(sqBase64EncryptedNut);
			cleartextBytes = cipher.doFinal(cipherbytes);
		} catch (final GeneralSecurityException e) {
			throw new SqrlInvalidRequestException("Error during nut decryption for " + sqBase64EncryptedNut, e);
		}

		try (final ByteArrayInputStream bais = new ByteArrayInputStream(cleartextBytes);
				final DataInputStream nutIs = new DataInputStream(bais)) {
			// Nut format is taken from the spec except for D2 (see below)
			// A) 32 bits: user's connection IP address if secured, 0.0.0.0 if non-secured.
			this.inetInt = nutIs.readInt();
			// B) 32 bits: UNIX-time timestamp incrementing once per second.
			// This is a 32-bit UNSIGNED int timestamp with second granularity
			final int temp = nutIs.readInt();
			// convert the unsigned int to a signed long with to millis granularity
			this.issuedTimestamp = Integer.toUnsignedLong(temp) * 1000;
			// C) 32 bits: up-counter incremented once for every SQRL link generated.
			this.counter = nutIs.readInt();
			// D) 31 bits: pseudo-random noise from system source.
			this.randomInt = nutIs.readInt();

			// D2) SQRL spec says "1 bit: flag bit to indicate source: QRcode or URL click"
			// but there is no way we can know this when we issue the first nut and there are better
			// ways to track this, so we ignored it
		} catch (final IOException e) {
			throw new SqrlClientRequestProcessingException(SqrlTifFlag.COMMAND_FAILED, e, "IO exception during read");
		}
	}

	@Override
	public Optional<String> compareSqrlClientInetAddress(InetAddress requesterIpAddress, SqrlConfig config)
			throws SqrlException {
		// From https://www.grc.com/sqrl/server.htm
		// Although this 128-bit total nut size only provides 32 bits for an IPv4 IP address, our purpose is only to
		// perform a match/no-match comparison to detect same-device phishing attacks. Therefore, any 128-bit IPv6
		// addresses can be safely �compressed� to 32 bits by hashing the full IPv6 IP with a secret salt and
		// retaining the least significant 32 bits of the hash result. The hash's salt can be the same AES key being
		// used to encrypt and decrypt the nut.
		if (inetInt == 0) {
			return Optional.of("Invalid inetInt from nut");
		}
		if (requesterIpAddress instanceof Inet4Address) {
			final byte[] bytes = unpack(inetInt);
			try {
				final InetAddress fromNut = InetAddress.getByAddress(bytes);
				if(requesterIpAddress.equals(fromNut)) {
					return Optional.empty();
				} else {
					return Optional.of(SqrlUtil.buildString("IPv4 address mismatch, browser=", fromNut.getHostAddress(), " sqrlClient=",requesterIpAddress.getHostAddress()));
				}
			} catch (final UnknownHostException e) {
				throw new SqrlException(e, "Got UnknownHostException for inetInt ", Integer.toString(inetInt));
			}
		} else if (requesterIpAddress instanceof Inet6Address) {
			final int currentIpPacked = packInet6Address((Inet6Address) requesterIpAddress, config);
			if(currentIpPacked == inetInt) {
				return Optional.empty();
			} else {
				return Optional.of(SqrlUtil.buildString("IPv6 address mismatch, browser packaed=", currentIpPacked, " sqrlClient=",requesterIpAddress.getHostAddress()));
			}
		} else {
			throw new SqrlException("Unknown InetAddress type of " + requesterIpAddress.getClass());
		}
	}

	@Override
	public long computeExpiresAt(SqrlConfig config) {
		final long nutValidityMillis = config.getNutValidityInSeconds() * 1000L;
		return getIssuedTimestampMillis() + nutValidityMillis;
	}

	@Override
	public String asEncryptedBase64() {
		return base64UrlEncryptedNut;
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

	public static int inetAddressToInt(final InetAddress requesterIpAddress,
			final SqrlConfig config) throws SqrlException {
		// From https://www.grc.com/sqrl/server.htm
		// Although this 128-bit total nut size only provides 32 bits for an IPv4 IP address, our purpose is only to
		// perform a match/no-match comparison to detect same-device phishing attacks. Therefore, any 128-bit IPv6
		// addresses can be safely �compressed� to 32 bits by hashing the full IPv6 IP with a secret salt and
		// retaining the least significant 32 bits of the hash result. The hash's salt can be the same AES key being
		// used to encrypt and decrypt the nut.
		if (requesterIpAddress instanceof Inet4Address) {
			return pack(requesterIpAddress.getAddress());
		} else if (requesterIpAddress instanceof Inet6Address) {
			return packInet6Address((Inet6Address) requesterIpAddress, config);
		} else {
			throw new SqrlException("Unknown InetAddress type of " + requesterIpAddress.getClass());
		}
	}

	private static int packInet6Address(final Inet6Address requesterIpAddress, final SqrlConfig config)
			throws SqrlException {
		// Compress per https://www.grc.com/sqrl/server.htm
		// IPv6 addresses can be safely compressed to 32 bits by hashing the full IPv6 IP with a secret salt and
		// retaining the least significant 32 bits of the hash result. The hash's salt can be the AES key
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			// salt with aes key bytes
			messageDigest.update(SqrlConfigHelper.getAESKeyBytes(config));
			final byte[] result = messageDigest.digest(requesterIpAddress.getAddress());
			// Get the least significant 32 bits of the hash result
			final byte[] toPack = new byte[IPV6_TO_PACK_BYTES];
			final int start = result.length - IPV6_TO_PACK_BYTES;
			System.arraycopy(result, start, toPack, 0, IPV6_TO_PACK_BYTES);
			final int packed = pack(toPack);
			logger.debug("IPV6 {} compressed and packed to {}", requesterIpAddress, packed);
			return packed;
		} catch (final NoSuchAlgorithmException e) {
			throw new SqrlException(e, "Error occured while hashing IPV6 address");
		}
	}

	// From https://stackoverflow.com/questions/2241229/going-from-127-0-0-1-to-2130706433-and-back-again
	static byte[] unpack(final int theInt) {
		return new byte[] { (byte) ((theInt >>> 24) & 0xff), (byte) ((theInt >>> 16) & 0xff),
				(byte) ((theInt >>> 8) & 0xff), (byte) ((theInt) & 0xff) };
	}

	int getInetInt() {
		return inetInt;
	}

	int getCounter() {
		return counter;
	}

	/**
	 * Creation time of the Nut in standard java millis format, but with second granularity; the millis value will
	 * always be 0000.
	 *
	 * @return the millis time at which the Nut token was created with <b>second precision</b>. For example, if
	 *         {@link SqrlNutToken#Nut(int, SqrlConfig, int, long, int)} was called with a timestamp of
	 *         <code>1463948680679</code>, then this would return <code>1463948680000</code>
	 *
	 */
	@Override
	public long getIssuedTimestampMillis() {
		return issuedTimestamp;
	}

	int getRandomInt() {
		return randomInt;
	}

	@Override
	InetAddress getBrowserIPAddress() {
		// By contract, this will never be called since we override compareSqrlClientInetAddress
		return null;
	}
}
