package com.grc.sqrl.server;
import static com.grc.sqrl.server.SqrlConstants.SCHEME_HTTP;
import static com.grc.sqrl.server.SqrlConstants.SCHEME_HTTPS;
import static com.grc.sqrl.server.SqrlConstants.SCHEME_HTTPS_COLON;
import static com.grc.sqrl.server.SqrlConstants.SCHEME_QRL;
import static com.grc.sqrl.server.SqrlConstants.SCHEME_SQRL;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to {@link SqrlConfig}
 * 
 * @author Dave Badia
 *
 */
public class SqrlConfigOperations {
	private static final Logger logger = LoggerFactory.getLogger(SqrlConfig.class);

	private static enum BackchannelSettingType {
		FULL_URL, FULL_PATH, PARTIAL_PATH
	};

	private final SqrlConfig sqrlConfig;

	private final Key aesKey;
	private final BackchannelSettingType backchannelSettingType;

	private URI backchannelRequestUrl;
	private String subsequentRequestPath;

	public SqrlConfigOperations(final SqrlConfig sqrlConfig) {
		this.sqrlConfig = sqrlConfig;

		// SecureRandom init
		SecureRandom secureRandom = sqrlConfig.getSecureRandom();
		if(secureRandom == null) {
			logger.warn("No SecureRandom set, initializing");
			try {
				secureRandom = SecureRandom.getInstanceStrong();
			} catch (final NoSuchAlgorithmException e) {
				// Per SecureRandom.getInstanceStrong() javadoc: Every implementation of the Java platform is required to
				// support at least one strong {@code SecureRandom} implementation. 
				throw new IllegalStateException("SecureRandom.getInstanceStrong() threw NoSuchAlgorithmException: "+e.getMessage(), e);
			}
		}

		// Add extra entropy to the secureRandom - setSeed ADDs entropy, it does not replace existing entropy
		// To help ensure random numbers are unique per JVM, seed with a UUID
		secureRandom.setSeed(UUID.randomUUID().toString().getBytes());
		secureRandom.setSeed(Runtime.getRuntime().freeMemory()); // Don't use total memory since it is predictable
		for (final File aRootDir : File.listRoots()) {
			secureRandom.setSeed(aRootDir.getFreeSpace()); // Don't use total space since it is predictable
		}

		// Server Friendly Name - not required, we can compute from server name if necessary

		// AES key init
		byte[] aesKeyBytes = sqrlConfig.getAESKeyBytes();
		if (aesKeyBytes == null || aesKeyBytes.length == 0) {
			logger.warn("No AES key set, generating new one");
			aesKeyBytes = new byte[SqrlConstants.AES_KEY_LENGTH];
			secureRandom.nextBytes(aesKeyBytes);
		} else if(aesKeyBytes.length != SqrlConstants.AES_KEY_LENGTH) {
			throw new IllegalArgumentException("SqrlConfig AES key must be " + SqrlConstants.AES_KEY_LENGTH + " bytes, found "+aesKeyBytes.length);
		}
		aesKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");

		// backchannelServletPath
		final String backchannelServletPathSetting = sqrlConfig.getBackchannelServletPath();
		backchannelSettingType = validateBackchannelSetting(backchannelServletPathSetting);
	}

	public Key getAESKey() {
		return aesKey;
	}

	public URI getBackchannelRequestUrl(final HttpServletRequest loginPageRequest) throws SqrlException {
		// No synchronization as worst case is we compute the value a few times
		String backchannelRequestString = null;
		if (this.backchannelRequestUrl == null) {
			final String requestUrl = loginPageRequest.getRequestURL().toString();
			if(backchannelSettingType == BackchannelSettingType.FULL_PATH) {
				// Chop off the URI, then add our path
				final String baseUrl = requestUrl.substring(0,
						requestUrl.length() - loginPageRequest.getRequestURI().length());
				backchannelRequestString = baseUrl + sqrlConfig.getBackchannelServletPath();
			} else if(backchannelSettingType == BackchannelSettingType.PARTIAL_PATH) {
				// Replace the last path with ours
				String workingCopy = requestUrl;
				if (workingCopy.endsWith(SqrlConstants.FORWARD_SLASH)) {
					workingCopy = workingCopy.substring(0, workingCopy.length() - 1);
				}
				final int lastIndex = workingCopy.lastIndexOf('/');
				workingCopy = workingCopy.substring(0, lastIndex + 1);
				backchannelRequestString = workingCopy + sqrlConfig.getBackchannelServletPath();
			} else if(backchannelSettingType == BackchannelSettingType.FULL_URL) {
				backchannelRequestString = sqrlConfig.getBackchannelServletPath();
			} else {
				throw new SqrlException("Don't know how to handle BackchannelSettingType: " + backchannelSettingType);
			}
			// Some SQRL clients require a dotted ip, so replace localhost with 127.0.0.1
			if (backchannelRequestString.contains(SqrlConstants.FORWARD_SLASH_X2_LOCALHOST)) {
				backchannelRequestString = backchannelRequestString.replace(SqrlConstants.FORWARD_SLASH_X2_LOCALHOST,
						SqrlConstants.FORWARD_SLASH_X2_127_0_0_1);
			}
			this.backchannelRequestUrl = changeToSqrlScheme(backchannelRequestString);
			logger.info("backchannelRequestUrl set to: " + this.backchannelRequestUrl);
		}
		return this.backchannelRequestUrl;
	}

	/**
	 * Converts a URL with http or https to qrl or sqrl, respectively
	 * 
	 * @param fullBackChannelUrl
	 * @return
	 */
	private static URI changeToSqrlScheme(final String fullBackChannelUrl) throws SqrlException {
		// Compute the proper protocol
		StringBuilder urlBuf = null;
		if (fullBackChannelUrl.startsWith(SCHEME_HTTPS_COLON)) {
			urlBuf = new StringBuilder(fullBackChannelUrl.replace(SCHEME_HTTPS, SCHEME_SQRL));
		} else if (fullBackChannelUrl.startsWith("http:")) {
			urlBuf = new StringBuilder(fullBackChannelUrl.replace(SCHEME_HTTP, SCHEME_QRL));
		} else {
			throw new SqrlException(
					"Don't know how to handle protocol of config.getBackChannelUrl(): " + fullBackChannelUrl);
		}
		try {
			return new URI(urlBuf.toString());
		} catch (final URISyntaxException e) {
			throw new SqrlException("Caught URISyntaxException with backchannel baseURL: " + urlBuf.toString(), e);
		}
	}

	public String getSubsequentRequestPath(final HttpServletRequest sqrlBackchannelRequest) throws SqrlException {
		// No synchronization as worst case is we compute the value a few times
		if (subsequentRequestPath == null) {
			// getBackchannelRequestUrl was called already
			try {
				this.subsequentRequestPath = new URI(sqrlBackchannelRequest.getRequestURL().toString()).getPath();
			} catch (final URISyntaxException e) {
				throw new SqrlException("Caught URISyntaxException with backchannel sqrlBackchannelRequest: "
						+ sqrlBackchannelRequest.getRequestURL().toString(), e);
			}
		}
		return this.subsequentRequestPath;
	}

	/**
	 * backchannelServletPath can be: 1) full URL, 2) full path ("/sqrlbc") or 3) partial path ("sqrlbc")
	 */
	private static BackchannelSettingType validateBackchannelSetting(final String backchannelServletPath) {
		BackchannelSettingType type = null;
		if (backchannelServletPath == null) {
			throw new IllegalArgumentException("SqrlConfig object must have backchannelServletPath set", null);
		} else if(backchannelServletPath.startsWith("/")) {
			type = BackchannelSettingType.FULL_PATH;
		} else if (backchannelServletPath.contains(".")) {
			// Must be a full url, validate it
			try {
				new URL(backchannelServletPath);
				type = BackchannelSettingType.FULL_URL;
			} catch (final MalformedURLException e) {
				// Try build a specific error message depending on the circumstances
				final StringBuilder buf = new StringBuilder(
						"SqrlConfig backchannelServletPath contained what appeared to be a URL but it ");
				buf.append("couldn't be parsed: ").append(backchannelServletPath);
				throw new IllegalArgumentException(buf.toString(), e);
			}
		} else {
			type = BackchannelSettingType.PARTIAL_PATH;
		}
		logger.info("BackchannelSettingType is " + type + " for backchannelServletPath: " + backchannelServletPath);
		return type;
	}
}
