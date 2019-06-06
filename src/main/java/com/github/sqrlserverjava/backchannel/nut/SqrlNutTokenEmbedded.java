package com.github.sqrlserverjava.backchannel.nut;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlConfigOperations;
import com.github.sqrlserverjava.backchannel.SqrlTifFlag;
import com.github.sqrlserverjava.exception.SqrlClientRequestProcessingException;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.exception.SqrlInvalidRequestException;
import com.github.sqrlserverjava.util.SqrlConstants;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * Nut token format which allows all pre-login SQRL state to be embedded in it. This allows the server to generate a
 * login page including SQRL without the need to store data in the database
 * 
 * @author Dave Badia
 *
 */
public class SqrlNutTokenEmbedded extends SqrlNutToken {
	private static final Logger	logger		= LoggerFactory.getLogger(SqrlNutTokenEmbedded.class);
	private static final int	AES_KEY_SIZE_BITS		= 128;
	/**
	 * NIST recommends a 96 bit IV: https://csrc.nist.gov/publications/detail/sp/800-38d/final
	 */
	private static final int	GCM_IV_SIZE_BYTES		= 12;
	/**
	 * AAD data is comprised of 1 byte format ID followed by the IV
	 */
	private static final int	AAD_SIZE_BYTES			= 1 + GCM_IV_SIZE_BYTES;
	private static final int	GCM_TAG_LENGTH_BYTES	= 16;

	static final int			FORMAT_ID				= buildFormatId((byte) 2);
	static final byte			FORMAT_ID_BYTE			= (byte) FORMAT_ID;
	private static final String	JSON_TAG_TIMESTAMP			= "ts";
	private static final String	JSON_TAG_IP_ADDRESS			= "ip";
	private static final String	JSON_TAG_CORRELATOR			= "cor";
	private static final String	JSON_TAG_BROWSER_LOGIN_URL	= "url";

	private final long			issuedTimestamp;
	private final InetAddress	browserIPAddress;
	private final String		correlator;
	private final String		browserLoginUrl;
	private final String		base64UrlEncryptedNut;

	// marshal to string
	public SqrlNutTokenEmbedded(final InetAddress browserIPAddress, final SqrlConfigOperations configOperations,
			final long timestamp, final String correlator, final String browserLoginUrl) throws SqrlException {
		final SqrlConfig config = configOperations.getSqrlConfig();
		this.issuedTimestamp = timestamp;
		this.browserIPAddress = browserIPAddress;
		this.correlator = correlator;
		this.browserLoginUrl = browserLoginUrl;
		final String jsonPayload = buildJsonPayload(issuedTimestamp, browserIPAddress, correlator, browserLoginUrl);
		// Build IV and AAD
		final byte[] iv = new byte[GCM_IV_SIZE_BYTES];
		config.getSecureRandom().nextBytes(iv);
		// additional authenticated data (AAD) is authenticated, but not encrypted. Use it to store our format ID and IV
		final byte[] additionalAuthenticatedData = new byte[AAD_SIZE_BYTES];
		additionalAuthenticatedData[0] = FORMAT_ID_BYTE;
		System.arraycopy(iv, 0, additionalAuthenticatedData, 1, GCM_IV_SIZE_BYTES);

		// Encrypt
		final byte[] cipherText = encryptWithAesGcm(additionalAuthenticatedData, iv, jsonPayload, configOperations);

		// build the final data to be encoded
		final byte[] finalBytes = new byte[AAD_SIZE_BYTES + cipherText.length];
		System.arraycopy(additionalAuthenticatedData, 0, finalBytes, 0, AAD_SIZE_BYTES);
		System.arraycopy(cipherText, 0, finalBytes, AAD_SIZE_BYTES, cipherText.length);
		this.base64UrlEncryptedNut = SqrlUtil.sqrlBase64UrlEncode(finalBytes);
	}

	// unmarshal from string
	public SqrlNutTokenEmbedded(final SqrlConfigOperations configOps, final String base64UrlEncryptedNut)
			throws SqrlClientRequestProcessingException {
		this.base64UrlEncryptedNut = base64UrlEncryptedNut;
		final byte[] decoded = SqrlUtil.base64UrlDecodeDataFromSqrlClient(base64UrlEncryptedNut);
		final byte[] aadBytes = new byte[AAD_SIZE_BYTES];
		System.arraycopy(decoded, 0, aadBytes, 0, AAD_SIZE_BYTES);
		final int formatId = buildFormatId(decoded[0]);
		final byte[] ivBytes = new byte[GCM_IV_SIZE_BYTES];
		System.arraycopy(decoded, 1, ivBytes, 0, GCM_IV_SIZE_BYTES);
		final int cipherTextLength = decoded.length - AAD_SIZE_BYTES;
		final byte[] cipherTextBytes = new byte[cipherTextLength];
		System.arraycopy(decoded, AAD_SIZE_BYTES, cipherTextBytes, 0, cipherTextLength);

		final byte[] plainText = verifyAndDecryptWithAesGcm(aadBytes, ivBytes, cipherTextBytes, configOps,
				base64UrlEncryptedNut);
		// Now we know the aad data was not modified
		if(formatId != FORMAT_ID) {
			throw new SqrlClientRequestProcessingException("Nut token contained incorrect formatId=",
					Integer.toString(formatId), " expected ", Integer.toString(FORMAT_ID));
		}
		final String jsonString = new String(plainText, SqrlConstants.UTF8_CHARSET);
		logger.debug("after decryption jsonString={}", jsonString);
		final JsonObject object = Json.parse(jsonString).asObject();
		this.issuedTimestamp = object.get(JSON_TAG_TIMESTAMP).asLong();
		final String ipAddressString = object.get(JSON_TAG_IP_ADDRESS).asString();
		try {
			this.browserIPAddress = InetAddress.getByName(ipAddressString);
		} catch (final UnknownHostException e) {
			throw new SqrlClientRequestProcessingException(SqrlTifFlag.COMMAND_FAILED, e, "Error parsing ipaddress=",
					ipAddressString);
		}
		this.correlator = object.get(JSON_TAG_CORRELATOR).asString();
		this.browserLoginUrl = object.get(JSON_TAG_BROWSER_LOGIN_URL).asString();
	}

	private byte[] encryptWithAesGcm(final byte[] additionalAuthenticatedData, final byte[] iv, final String jsonPayload,
			final SqrlConfigOperations configOperations) throws SqrlException {
		// A good overview of AES GCM is here: https://crypto.stackexchange.com/a/18092
		// Encrypt and encode the nut
		try {
			final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			final GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BYTES * 8, iv);
			cipher.init(Cipher.ENCRYPT_MODE, configOperations.getAESKey(), spec);
			cipher.updateAAD(additionalAuthenticatedData);
			final byte[] plainTextBytes = jsonPayload.getBytes(SqrlConstants.UTF8_CHARSET);
			return cipher.doFinal(plainTextBytes);
		} catch (final GeneralSecurityException e) {
			throw new SqrlException(e, "Error during encryption of SQRL nut token");
		}
	}

	private byte[] verifyAndDecryptWithAesGcm(final byte[] additionalAuthenticatedData, final byte[] iv, final byte[] cipherTextBytes,
			final SqrlConfigOperations configOperations, final String base64UrlEncryptedNut) throws SqrlInvalidRequestException {
		// A good overview of AES GCM is here: https://crypto.stackexchange.com/a/18092
		// Encrypt and encode the nut
		try {
			final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			final GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BYTES * 8, iv);
			cipher.init(Cipher.DECRYPT_MODE, configOperations.getAESKey(), spec);
			cipher.updateAAD(additionalAuthenticatedData);
			return cipher.doFinal(cipherTextBytes);
		} catch (final GeneralSecurityException e) {
			throw new SqrlInvalidRequestException(e, "Error during verification and decryption of SQRL nut token=",
					base64UrlEncryptedNut);
		}
	}

	private static String buildJsonPayload(final long issuedTimestamp, final InetAddress browserIPAddress, final String correlator,
			final String browserLoginUrl) {
		final JsonObject jsonObject = Json.object();
		jsonObject.add(JSON_TAG_TIMESTAMP, issuedTimestamp);
		jsonObject.add(JSON_TAG_IP_ADDRESS, browserIPAddress.getHostAddress());
		jsonObject.add(JSON_TAG_CORRELATOR, correlator);
		jsonObject.add(JSON_TAG_BROWSER_LOGIN_URL, browserLoginUrl);
		return jsonObject.toString();
	}

	public static byte[] compress(final byte[] data) throws SqrlException {
		final Deflater deflater = new Deflater();
		deflater.setInput(data);
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

		deflater.finish();
		final byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			final int count = deflater.deflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		try {
			outputStream.close();
			final byte[] output = outputStream.toByteArray();
			logger.debug("Nut compression: compressed zlib={} original={}", output.length, data.length);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final GZIPOutputStream gzipOs = new GZIPOutputStream(baos);
			// ZipOutputStream zos = new ZipOutputStream(baos);
			gzipOs.write(data);
			gzipOs.close();
			return output;
		} catch (final IOException e) {
			throw new SqrlException(e, "Caught error during compression");
		}
	}

	@Override
	InetAddress getBrowserIPAddress() {
		return browserIPAddress;
	}

	@Override
	public String asEncryptedBase64() {
		return base64UrlEncryptedNut;
	}

	@Override
	public long getIssuedTimestampMillis() {
		return issuedTimestamp;
	}

}
