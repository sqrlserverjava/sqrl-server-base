package com.github.dbadia.sqrl.server.backchannel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlConfigOperations;
import com.github.dbadia.sqrl.server.SqrlException;
import com.github.dbadia.sqrl.server.SqrlUtil;

/**
 * The SQRL "Nut" one time use (nonce) token as described on https://www.grc.com/sqrl/server.htm<br/>
 *
 * @author Dave Badia
 *
 */
public class SqrlNutToken {

	private final int		inetInt;
	private final int		counter;
	private final long		issuedTimestamp;
	private final int		randomInt;
	/**
	 * The encrypted nut in sqbase64 format as it appeared in the query string
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
	public SqrlNutToken(final int inetInt, final SqrlConfigOperations configOps, final int counter,
			final long timestamp, final int randomInt) throws SqrlException {
		this.inetInt = inetInt;
		this.counter = counter;
		// Convert the timestamp param from millis precision to second precision
		this.issuedTimestamp = (timestamp / 1000) * 1000;
		this.randomInt = randomInt;
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
			cipher.init(Cipher.ENCRYPT_MODE, configOps.getAESKey());
			final byte[] encrypted = cipher.doFinal(nutBytes);
			this.base64UrlEncryptedNut = SqrlUtil.sqrlBase64UrlEncode(encrypted);
		} catch (final GeneralSecurityException e) {
			throw new SqrlException("Error during nut encryption", e);
		} catch (final IOException e) {
			throw new SqrlException("IO exception during write", e);
		}
	}

	public SqrlNutToken(final SqrlConfigOperations configOps, final String sqBase64EncryptedNut) throws SqrlException {
		this.base64UrlEncryptedNut = sqBase64EncryptedNut;
		// Decrypt the nut
		byte[] cleartextBytes = null;
		try {
			final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, configOps.getAESKey());
			final byte[] cipherbytes = SqrlUtil.base64UrlDecode(sqBase64EncryptedNut);
			cleartextBytes = cipher.doFinal(cipherbytes);
		} catch (final GeneralSecurityException e) {
			throw new SqrlException("Error during nut decryption for " + sqBase64EncryptedNut, e);
		}

		try (final ByteArrayInputStream bais = new ByteArrayInputStream(cleartextBytes);
				final DataInputStream nutIs = new DataInputStream(bais)) {
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

			// D2) FUTURE: 1 bit: flag bit to indicate source: QRcode or URL click
		} catch (final IOException e) {
			throw new SqrlException("IO exception during read", e);
		}
	}

	public int getInetInt() {
		return inetInt;
	}

	public int getCounter() {
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
	public long getIssuedTimestampMillis() {
		return issuedTimestamp;
	}

	public int getRandomInt() {
		return randomInt;
	}

	public String asSqrlBase64EncryptedNut() {
		return base64UrlEncryptedNut;
	}

	@Override
	public String toString() {
		return "SqrlNutToken [inetInt=" + inetInt + ", counter=" + counter + ", issuedTimestamp=" + issuedTimestamp
				+ ", randomInt=" + randomInt + ", base64UrlEncryptedNut=" + base64UrlEncryptedNut + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base64UrlEncryptedNut == null) ? 0 : base64UrlEncryptedNut.hashCode());
		return result;
	}

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
		final SqrlNutToken other = (SqrlNutToken) obj;
		if (base64UrlEncryptedNut == null) {
			if (other.base64UrlEncryptedNut != null) {
				return false;
			}
		} else if (!base64UrlEncryptedNut.equals(other.base64UrlEncryptedNut)) {
			return false;
		}
		return true;
	}
}
