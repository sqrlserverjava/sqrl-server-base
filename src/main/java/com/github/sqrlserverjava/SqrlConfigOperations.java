package com.github.sqrlserverjava;
import static com.github.sqrlserverjava.util.SqrlConstants.FORWARD_SLASH;
import static com.github.sqrlserverjava.util.SqrlConstants.FORWARD_SLASH_X2_127_0_0_1;
import static com.github.sqrlserverjava.util.SqrlConstants.SCHEME_HTTP;
import static com.github.sqrlserverjava.util.SqrlConstants.SCHEME_HTTPS;
import static com.github.sqrlserverjava.util.SqrlConstants.SCHEME_HTTPS_COLON;
import static com.github.sqrlserverjava.util.SqrlConstants.SCHEME_HTTP_COLON;
import static com.github.sqrlserverjava.util.SqrlConstants.SCHEME_SQRL;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Key;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.exception.SqrlConfigSettingException;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.persistence.SqrlJpaPersistenceFactory;
import com.github.sqrlserverjava.util.SqrlConfigHelper;
import com.github.sqrlserverjava.util.SqrlServiceExecutor;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * Helper class to {@link SqrlConfig}
 *
 * @author Dave Badia
 *
 */
public class SqrlConfigOperations {
	private static final Logger			logger				= LoggerFactory.getLogger(SqrlConfig.class);
	/**
	 * Set automatically but only used if this class is set on {@link SqrlConfig#setSqrlPersistenceFactoryClass(String)}
	 */
	private static SqrlServiceExecutor	sqrlServiceExecutor	= null;

	private enum BackchannelSettingType {
		FULL_URL, FULL_PATH, PARTIAL_PATH
	}

	private final SqrlPersistenceFactory	sqrlPersistenceFactory;
	private final SqrlConfig				config;

	private final Key						aesKey;
	private final BackchannelSettingType	backchannelSettingType;

	private String	subsequentRequestPath;

	/**
	 * Internal use only.
	 *	
	 * @param config
	 *            the SQRL config object
	 */
	SqrlConfigOperations(final SqrlConfig config) {
		this.config = config;

		// SecureRandom
		final SecureRandom secureRandom = config.getSecureRandom();
		if (secureRandom == null) {
			throw new SqrlConfigSettingException("config.getSecureRandom() was null");
		}

		// Add extra entropy to the secureRandom - setSeed ADDs entropy, it does not replace existing entropy
		// To help ensure random numbers are unique per JVM, seed with a UUID
		secureRandom.setSeed(UUID.randomUUID().toString().getBytes());
		secureRandom.setSeed(Runtime.getRuntime().freeMemory()); // Don't use total memory since it is predictable
		for (final File aRootDir : File.listRoots()) {
			secureRandom.setSeed(aRootDir.getFreeSpace()); // Don't use total space since it is predictable
		}

		// if CPS is enabled, then <cpsCancelUri> must be set as well
		if (config.isEnableCps() && SqrlUtil.isBlank(config.getCpsCancelUri())) {
			throw new SqrlConfigSettingException("config cpsCancelUri must be set since CPS is enabled");
		}
		
		// AES key init
		byte[] aesKeyBytes = SqrlConfigHelper.getAESKeyBytes(config);
		aesKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");

		// backchannelServletPath
		final String backchannelServletPathSetting = config.getBackchannelServletPath();
		backchannelSettingType = validateBackchannelSetting(backchannelServletPathSetting);

		// SQRL persistence factory class name
		final String factoryClassName = config.getSqrlPersistenceFactoryClass();
		if (SqrlUtil.isBlank(factoryClassName)) {
			sqrlPersistenceFactory = new SqrlJpaPersistenceFactory();
		} else {
			try {
				@SuppressWarnings("rawtypes")
				final Class clazz = Class.forName(factoryClassName);
				sqrlPersistenceFactory = (SqrlPersistenceFactory) createInstanceFromNoArgConstructor(clazz,
						"sqrlPersistenceFactory");
			} catch (final Exception e) {
				throw new IllegalArgumentException(
						"Could not create SqrlPersistenceFactory with name '" + factoryClassName + "'", e);
			}
		}
		// register the cleanup task
		final Class<? extends Runnable> cleanUpTaskClass = sqrlPersistenceFactory.getCleanupTaskClass();
		if (cleanUpTaskClass == null) {
			logger.warn("sqrlPersistenceFactory cleanup task was null, no background cleanup job scheduled");
		} else {
			final Runnable cleanupTask = (Runnable) createInstanceFromNoArgConstructor(cleanUpTaskClass,
					"SqrlPersistenceFactory.getCleanupTaskClass()");
			final int intervalMinutes = config.getCleanupTaskExecInMinutes();
			sqrlServiceExecutor.scheduleAtFixedRate(cleanupTask, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
		}
	}

	private static Object createInstanceFromNoArgConstructor(@SuppressWarnings("rawtypes") final Class clazz,
			final String description) {
		try {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			final Constructor constructor = clazz.getConstructor();
			return constructor.newInstance();
		} catch (final NoSuchMethodException e) {
			throw new SqrlConfigSettingException(
					clazz.getName() + " must have a default no-arg constructor, but none was found", e);
		} catch (final Exception e) {
			throw new SqrlConfigSettingException("Error instantiating " + description + " of " + clazz.getName(), e);
		}
	}

	/**
	 * Poor mans dependency injection. Can't use CDI since we want to support lightweight JEE servers like tomcat
	 *
	 * @param sqrlServiceExecutor
	 */
	public static void setExecutor(final SqrlServiceExecutor sqrlServiceExecutor) {
		SqrlConfigOperations.sqrlServiceExecutor = sqrlServiceExecutor;
	}

	public Key getAESKey() {
		return aesKey;
	}

	/**
	 * Internal use only. Determines backchannel request URL based on the loginPageRequest
	 *
	 * @param loginPageRequest
	 *            the login page request
	 * @return the URI where SQRL client requests should be sent
	 * @throws SqrlException
	 *             if an invalid backchannelSettingType is present
	 */
	public URI buildBackchannelRequestUrl(final HttpServletRequest loginPageRequest) throws SqrlException {
		// No synchronization as worst case is we compute the value a few times
		String backchannelRequestString = null;
		final String requestUrl = loginPageRequest.getRequestURL().toString();
		if (backchannelSettingType == BackchannelSettingType.FULL_PATH) {
			// Chop off the URI, then add our path
			final String baseUrl = requestUrl.substring(0,
					requestUrl.length() - loginPageRequest.getRequestURI().length());
			backchannelRequestString = baseUrl + config.getBackchannelServletPath();
		} else if (backchannelSettingType == BackchannelSettingType.PARTIAL_PATH) {
			// Replace the last path with ours
			String workingCopy = requestUrl;
			if (workingCopy.endsWith(FORWARD_SLASH)) {
				workingCopy = workingCopy.substring(0, workingCopy.length() - 1);
			}
			final int lastIndex = workingCopy.lastIndexOf('/');
			workingCopy = workingCopy.substring(0, lastIndex + 1);
			backchannelRequestString = workingCopy + config.getBackchannelServletPath();
		} else if (backchannelSettingType == BackchannelSettingType.FULL_URL) {
			backchannelRequestString = config.getBackchannelServletPath();
		} else {
			throw new SqrlException("Don't know how to handle BackchannelSettingType: " + backchannelSettingType);
		}
		// Some SQRL clients require a dotted ip, so replace localhost with 127.0.0.1
		if (backchannelRequestString.contains("//localhost/") || backchannelRequestString.contains("//localhost:")) {
			backchannelRequestString = backchannelRequestString.replace("//localhost",
					FORWARD_SLASH_X2_127_0_0_1);
			// Some testers use a localhost.com alias, replace the .com if it's there
			// backchannelRequestString = backchannelRequestString.replace(".com", "");
			// TODO:
		}
		final URI backchannelRequestUrl = changeToSqrlScheme(backchannelRequestString);
		logger.debug("requestUrl={}, backchannelRequestString={},  backchannelRequestUrl={} ", requestUrl,
				backchannelRequestString, backchannelRequestUrl);
		return backchannelRequestUrl;
	}

	/**
	 * Verifies the URL is secure
	 *
	 * @param fullBackChannelUrl
	 * @return
	 */
	private static URI changeToSqrlScheme(final String fullBackChannelUrl) throws SqrlException {
		// Compute the proper protocol
		StringBuilder urlBuf = new StringBuilder(fullBackChannelUrl.length() + 5);
		if (fullBackChannelUrl.startsWith(SCHEME_HTTPS_COLON)) {
			urlBuf.append(fullBackChannelUrl.replace(SCHEME_HTTPS, SCHEME_SQRL));
		} else if (fullBackChannelUrl.startsWith(SCHEME_HTTP_COLON)) {
			// reverse proxy may go unencrypted between SSL termination and the JEE
			// container
			urlBuf.append(fullBackChannelUrl.replace(SCHEME_HTTP, SCHEME_SQRL));
		} else {
			throw new SqrlException(
					"Don't know how to handle protocol of config.getBackChannelUrl(): " + fullBackChannelUrl);
		}
		try {
			return new URI(urlBuf.toString());
		} catch (final URISyntaxException e) {
			throw new SqrlException(e, "Caught URISyntaxException with backchannel baseURL: ", urlBuf.toString());
		}
	}

	/**
	 * Internal use only. Computes the subsequent URI path for the SQRL client
	 *
	 * @param sqrlBackchannelRequest
	 *            the original SQRL client request
	 * @return the URI string to be sent back to the client
	 * @throws SqrlException
	 *             if a URISyntaxException occurs
	 */
	public String getSubsequentRequestPath(final HttpServletRequest sqrlBackchannelRequest) throws SqrlException {
		// No synchronization as worst case is we compute the value a few times
		if (subsequentRequestPath == null) {
			// getBackchannelRequestUrl was called already
			try {
				this.subsequentRequestPath = new URI(sqrlBackchannelRequest.getRequestURL().toString()).getPath();
			} catch (final URISyntaxException e) {
				throw new SqrlException(e, "Caught URISyntaxException with backchannel sqrlBackchannelRequest: ",
						sqrlBackchannelRequest.getRequestURL().toString());
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
		} else if (backchannelServletPath.startsWith("/")) {
			type = BackchannelSettingType.FULL_PATH;
		} else if (backchannelServletPath.contains(".")) {
			// Must be a full url, validate it
			try {
				new URL(backchannelServletPath).toString(); // toString() = @SuppressWarnings(value = "squid:S1848")
				type = BackchannelSettingType.FULL_URL;
			} catch (final MalformedURLException e) {
				// Try build a specific error message depending on the circumstances
				final StringBuilder buf = new StringBuilder(100);
				buf.append("SqrlConfig backchannelServletPath contained what appeared to be a URL but it ");
				buf.append("couldn't be parsed: ").append(backchannelServletPath);
				throw new IllegalArgumentException(buf.toString(), e);
			}
		} else {
			type = BackchannelSettingType.PARTIAL_PATH;
		}
		logger.info("BackchannelSettingType is " + type + " for backchannelServletPath: " + backchannelServletPath);
		return type;
	}

	public SqrlConfig getSqrlConfig() {
		return config;
	}

	public SqrlPersistenceFactory getSqrlPersistenceFactory() {
		return sqrlPersistenceFactory;
	}
}
