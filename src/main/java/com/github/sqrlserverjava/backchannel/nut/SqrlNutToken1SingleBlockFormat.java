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
 * "Nut" token format matching the size (but not the data layout) of the format suggested by the SQRL spec. This is not
 * the preferred format but is retained in case clients have issues processing our preferred format, which is larger in
 * size
 * 
 * This token format is deprecated for the following reasons:
 * <li/>1. Requires server side state (that is, database entries) when displaying the login page
 * <li/>2. This token is encrypted but is <b>not</b> signed
 * <li/>3. Can only store part of an IPv6 address, which makes IP mismatch debugging difficult
 *
 * @author Dave Badia
 * @deprecated SqrlNutTokenEmbedded is preferred
 */
@Deprecated
public class SqrlNutToken1SingleBlockFormat extends SqrlNutToken0 {
	private static final Logger	logger				= LoggerFactory.getLogger(SqrlNutToken1SingleBlockFormat.class);

	private static final int	IPV6_TO_PACK_BYTES	= 4;
	static final int			FORMAT_ID			= 1;

	private final int		inetInt;
	private final long		issuedTimestamp;
	private final int		randomInt;
	/**
	 * The encrypted nut in base64url format as it appeared in the query string
	 */
	private final String	base64UrlEncryptedNut;

	/**
	 *
	 * @param timestamp
	 *            The time at which the Nut was created, typically {@link System#currentTimeMillis()}. Note that the
	 *            data in the Nut is only stored with second granularity
	 * @param randomInt
	 * @throws SqrlException
	 * 
	 * @deprecated SqrlNutTokenEmbedded is preferred
	 */
	@Deprecated
	SqrlNutToken1SingleBlockFormat(final InetAddress browserIp, final SqrlConfigOperations configOps, 
			final long timestamp) throws SqrlException {
		this.inetInt = pack(browserIp.getAddress());
		// Convert the timestamp param from millis precision to second precision
		this.issuedTimestamp = (timestamp / 1000) * 1000;
		this.randomInt = configOps.getSqrlConfig().getSecureRandom().nextInt();
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final DataOutputStream nutOs = new DataOutputStream(baos)) {
			// A) 32 bits: format ID
			nutOs.writeInt(FORMAT_ID);
			// B) 32 bits: browser IP address
			nutOs.writeInt(inetInt);
			// C) 32 bits: UNIX-time timestamp incrementing once per second.
			// Note this is a 32-bit unsigned int, not a long. We have second granularity
			final int unixTimeInSeconds = (int) (this.issuedTimestamp / 1000);
			nutOs.writeInt(unixTimeInSeconds);
			// D) 32 bits: random data to help ensure uniqueness
			nutOs.writeInt(randomInt);
			final byte[] nutBytes = baos.toByteArray();
			// Encrypt and encode the nut
			final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, configOps.getAESKey());
			final byte[] encrypted = cipher.doFinal(nutBytes);
			// First byte of data must be our format ID
			final byte[] finalBytes = new byte[encrypted.length + 1];
			finalBytes[0] = (byte) FORMAT_ID;
			System.arraycopy(encrypted, 0, finalBytes, 1, encrypted.length);
			this.base64UrlEncryptedNut = SqrlUtil.sqrlBase64UrlEncode(finalBytes);
		} catch (final GeneralSecurityException e) {
			throw new SqrlException(e, "Error during nut encryption");
		} catch (final IOException e) {
			throw new SqrlException(e, "IO exception during write");
		}
	}

	SqrlNutToken1SingleBlockFormat(final SqrlConfigOperations configOps, final String sqBase64EncryptedNut)
			throws SqrlClientRequestProcessingException {
		this.base64UrlEncryptedNut = sqBase64EncryptedNut;
		// Verify the format ID which is the first byte of decoded data
		final byte[] nutBytes = SqrlUtil.base64UrlDecodeDataFromSqrlClient(sqBase64EncryptedNut);
		verifyFormatId(SqrlNutToken0.buildFormatId(nutBytes[0]));
		// Decrypt the nut
		byte[] cleartextBytes = null;
		try {
			final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, configOps.getAESKey());
			final byte[] cipherbytes = SqrlUtil.base64UrlDecodeDataFromSqrlClient(sqBase64EncryptedNut);
			cleartextBytes = cipher.doFinal(cipherbytes);
		} catch (final GeneralSecurityException e) {
			throw new SqrlInvalidRequestException(e, "Error during nut decryption for ", sqBase64EncryptedNut);
		}

		try (final ByteArrayInputStream bais = new ByteArrayInputStream(cleartextBytes);
				final DataInputStream nutIs = new DataInputStream(bais)) {
			// A) 32 bits: format ID is also the first byte of decrypted data
			verifyFormatId(nutIs.readInt());
			// B) 32 bits: browser IP address
			this.inetInt = nutIs.readInt();
			// B) 32 bits: UNIX-time timestamp incrementing once per second.
			// This is a 32-bit UNSIGNED int timestamp with second granularity
			final int temp = nutIs.readInt();
			// convert the unsigned int to a signed long with to millis granularity
			this.issuedTimestamp = Integer.toUnsignedLong(temp) * 1000;
			// D) 32 bits: random data to help ensure uniqueness
			this.randomInt = nutIs.readInt();
		} catch (final IOException e) {
			throw new SqrlClientRequestProcessingException(SqrlTifFlag.COMMAND_FAILED, e, "IO exception during nut unmarshalling");
		}
	}

	private void verifyFormatId(final int formatIdFromEncoded) throws SqrlClientRequestProcessingException {
		if (formatIdFromEncoded != FORMAT_ID) {
			throw new SqrlClientRequestProcessingException(SqrlTifFlag.COMMAND_FAILED.toString(),
					"Nut format ID mismatch, expected ", Integer.toString(FORMAT_ID), " but found ",
					Integer.toString(formatIdFromEncoded));
		}
	}

	@Override
	public Optional<String> compareSqrlClientInetAddress(final InetAddress requesterIpAddress, final SqrlConfig config)
			throws SqrlException {
		// From https://www.grc.com/sqrl/server.htm
		// Although this 128-bit total nut size only provides 32 bits for an IPv4 IP address, our purpose is only to
		// perform a match/no-match comparison to detect same-device phishing attacks. Therefore, any 128-bit IPv6
		// addresses can be safely �compressed� to 32 bits by hashing the full IPv6 IP with a secret salt and
		// retaining the least significant 32 bits of the hash result. The hash's salt can be the same AES key being
		// used to encrypt and decrypt the nut.
		if (inetInt == 0) {
			return Optional.of("inetInt was zero");
		}
		if (requesterIpAddress instanceof Inet4Address) {
			final byte[] bytes = unpack(inetInt);
			try {
				final InetAddress fromNut = InetAddress.getByAddress(bytes);
				if (requesterIpAddress.equals(fromNut)) {
					return Optional.empty();
				} else {
					return Optional
							.of("original IP=" + fromNut.toString() + ", current IP=" + requesterIpAddress.toString());
				}
			} catch (final UnknownHostException e) {
				throw new SqrlException(e, "Got UnknownHostException for inet ", Integer.toString(inetInt));
			}
		} else if (requesterIpAddress instanceof Inet6Address) {
			final int currentIpPacked = packInet6Address((Inet6Address) requesterIpAddress, config);
			if (currentIpPacked == inetInt) {
				return Optional.empty();
			} else {
				return Optional.of("original packed=" + inetInt + ", current packed=" + currentIpPacked);
			}
		} else {
			throw new SqrlException("Unknown InetAddress type of " + requesterIpAddress.getClass());
		}
	}

	@Override
	public long computeExpiresAt(final SqrlConfig config) {
		final long nutValidityMillis = config.getNutValidityInSeconds() * 1000L;
		return getIssuedTimestampMillis() + nutValidityMillis;
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

	// unit testing only
	int getInetInt() {
		return inetInt;
	}

	@Override
	public String asEncryptedBase64() {
		return base64UrlEncryptedNut;
	}

	/**
	 * Creation time of the Nut in standard java millis format, but with second granularity; the millis value will
	 * always be 0000.
	 *
	 * @return the millis time at which the Nut token was created with <b>second precision</b>. For example, if
	 *         {@link SqrlNutToken1SingleBlockFormat#Nut(int, SqrlConfig, int, long, int)} was called with a timestamp of
	 *         <code>1463948680679</code>, then this would return <code>1463948680000</code>
	 *
	 */
	@Override
	long getIssuedTimestampMillis() {
		return issuedTimestamp;
	}

	int getRandomInt() {
		return randomInt;
	}

	String asBase64UrlEncryptedNut() {
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

	// From https://stackoverflow.com/questions/2241229/going-from-127-0-0-1-to-2130706433-and-back-again
	static byte[] unpack(final int theInt) {
		return new byte[] { (byte) ((theInt >>> 24) & 0xff), (byte) ((theInt >>> 16) & 0xff),
				(byte) ((theInt >>> 8) & 0xff), (byte) ((theInt) & 0xff) };
	}

	@Override
	InetAddress getBrowserIPAddress() {
		// By contract, this will never be called since we override compareSqrlClientInetAddress
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("SqrlNutTokenLegacyFormat [inetInt=").append(inetInt).append(", issuedTimestamp=")
		.append(issuedTimestamp).append(", randomInt=").append(randomInt).append(", base64UrlEncryptedNut=")
		.append(base64UrlEncryptedNut).append("]");
		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + inetInt;
		result = prime * result + (int) (issuedTimestamp ^ (issuedTimestamp >>> 32));
		result = prime * result + randomInt;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SqrlNutToken1SingleBlockFormat other = (SqrlNutToken1SingleBlockFormat) obj;
		if (inetInt != other.inetInt) {
			return false;
		}
		if (issuedTimestamp != other.issuedTimestamp) {
			return false;
		}
		if (randomInt != other.randomInt) {
			return false;
		}
		return true;
	}

}
