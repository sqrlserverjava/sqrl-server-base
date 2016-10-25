package com.github.dbadia.sqrl.server;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.mock.web.MockHttpServletRequest;

import com.github.dbadia.sqrl.server.backchannel.SqrlNutToken;
import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.backchannel.SqrlServiceExecutor;
import com.github.dbadia.sqrl.server.data.SqrlAutoCloseablePersistence;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;
import com.github.dbadia.sqrl.server.data.SqrlJpaPersistenceProvider;

import junitx.util.PrivateAccessor;

public class TCUtil {
	public static final Date	AWHILE_FROM_NOW							= new Date(
			System.currentTimeMillis() + 1000000);
	static final String			DEFAULT_CONFIG_SQRL_BACKCHANNEL_PATH	= "http://127.0.0.1:8080/sqrlbc";
	static final byte[]			AES_TEST_KEY							= new byte[16];

	static class TCSqrlConfig extends SqrlConfig {
		private final long timestampForNextNut;

		private TCSqrlConfig(final long timestampForNextNut) {
			this.timestampForNextNut = timestampForNextNut;
		}

		@Override
		public long getCurrentTimeMs() {
			return timestampForNextNut;
		}
	}

	@SuppressWarnings("deprecation")
	public static final SqrlConfig buildTestSqrlConfig(final String nutString) throws Exception {
		final SqrlNutToken nutToken = new SqrlNutToken(new SqrlConfigOperations(TCUtil.buildTestSqrlConfig()),
				nutString);

		final SqrlConfig config = new TCSqrlConfig(nutToken.getIssuedTimestampMillis());
		// Set SqrlServerOperations counter to generate the expected value
		final AtomicInteger sqrlServerOperationscounter = (AtomicInteger) PrivateAccessor
				.getField(SqrlServerOperations.class, "COUNTER");
		sqrlServerOperationscounter.set(nutToken.getCounter());
		// TestSecureRandom isn't random at all which is very fast
		final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(nutToken.getRandomInt());
		config.setSecureRandom(new TestSecureRandom(buffer.array()));

		config.setServerFriendlyName("Dave Test");
		config.setBackchannelServletPath("http://127.0.0.1:8080/sqrlbc");
		// set AES key to all zeros for test cases
		config.setAESKeyBytes(AES_TEST_KEY);

		return config;
	}

	@SuppressWarnings("deprecation") // test cases can use deprecated TestSecureRandom
	public static final SqrlConfig buildTestSqrlConfig() {
		final SqrlServiceExecutor executor = new SqrlServiceExecutor();
		executor.contextInitialized(null);
		final SqrlConfig config = new SqrlConfig();
		config.setServerFriendlyName("Dave Test");
		config.setBackchannelServletPath("http://127.0.0.1:8080/sqrlbc");
		// set AES key to all zeros for test cases
		config.setAESKeyBytes(AES_TEST_KEY);
		// TestSecureRandom isn't random at all which is very fast
		// If we didn't set a secure random, SecureRandom.getInstance will be called
		// which would slow down most of our test cases for no good reason
		config.setSecureRandom(new TestSecureRandom(null));

		return config;
	}

	public static void setupSqrlPersistence(final String correlatorFromServerParam, final String serverParam)
			throws Throwable {
		final SqrlPersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence();
		final SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(correlatorFromServerParam,
				TCUtil.AWHILE_FROM_NOW);
		if (serverParam != null) {
			sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		}
		sqrlPersistence.closeCommit();
	}

	@SuppressWarnings("deprecation") // OK for test case use
	public static SqrlPersistence setupIdk(final String idk, final String correlator, final String serverParam) {
		final SqrlPersistence persistence = new SqrlJpaPersistenceProvider();
		final SqrlCorrelator sqrlCorrelator = persistence.createCorrelator(correlator, TCUtil.AWHILE_FROM_NOW);
		if (serverParam != null) {
			sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		}
		persistence.createAndEnableSqrlIdentity(idk, Collections.emptyMap());
		persistence.closeCommit();
		return new SqrlJpaPersistenceProvider();
	}

	public static MockHttpServletRequest buildMockRequest(final String uriString) throws URISyntaxException {
		return buildMockRequest(new URI(uriString));
	}

	/**
	 *
	 * @param loginRequestUrl
	 * @param mockDataParams
	 *            a URI string from the GRC client log such as "client=123&server=456&ids=789"
	 * @return
	 * @throws URISyntaxException
	 */
	public static MockHttpServletRequest buildMockRequest(final String requestUrl, final String mockDataParams)
			throws URISyntaxException {
		final MockHttpServletRequest mockRequest = buildMockRequest(requestUrl);
		for (final String nameValuePair : mockDataParams.split("&")) {
			final String[] parts = nameValuePair.split("=");
			mockRequest.addParameter(parts[0], parts[1]);
		}
		return mockRequest;
	}

	public static MockHttpServletRequest buildMockRequest(final URI uri) {
		final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setScheme(uri.getScheme());
		mockRequest.setServerName(uri.getHost());
		mockRequest.setServerPort(uri.getPort());
		mockRequest.setRequestURI(uri.getPath());
		return mockRequest;
	}

	public static MockHttpServletRequest buildMockRequest(final URL url) {
		final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setScheme(url.getProtocol());
		mockRequest.setServerName(url.getHost());
		mockRequest.setServerPort(url.getPort());
		mockRequest.setRequestURI(url.getPath());
		return mockRequest;
	}

	public static SqrlNutToken buildValidSqrlNut(final SqrlConfig config) throws SqrlException {
		final long timestamp = System.currentTimeMillis();
		final int inetInt = 4;
		final int counter = 234;
		final int random = 6;
		final SqrlNutToken nut = new SqrlNutToken(inetInt, new SqrlConfigOperations(config), counter, timestamp,
				random);
		return nut;
	}

	public static SqrlNutToken buildValidSqrlNut(final SqrlConfig config, final LocalDateTime nutIssuedAt)
			throws SqrlException {
		final long timestamp = nutIssuedAt.toEpochSecond(ZoneOffset.UTC);
		final int inetInt = 4;
		final int counter = 234;
		final int random = 6;
		final SqrlNutToken nut = new SqrlNutToken(inetInt, new SqrlConfigOperations(config), counter, timestamp,
				random);
		return nut;
	}

	public static SqrlAutoCloseablePersistence createEmptySqrlPersistence() throws NoSuchFieldException {
		final SqrlPersistence sqrlPersistence = createSqrlPersistence();
		final EntityManagerFactory entityManagerFactory = extractEntityManagerFactory(sqrlPersistence);
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();
		// entityManager.createQuery("DELETE FROM SqrlCorrelator m").executeUpdate();
		// entityManager.createQuery("DELETE FROM SqrlIdentity m").executeUpdate();
		for (final Object object : entityManager.createQuery("SELECT e FROM SqrlCorrelator e").getResultList()) {
			entityManager.remove(object);
		}
		for (final Object object : entityManager.createQuery("SELECT e FROM SqrlIdentity e").getResultList()) {
			entityManager.remove(object);
		}
		for (final Object object : entityManager.createQuery("SELECT e FROM SqrlUsedNutToken e").getResultList()) {
			entityManager.remove(object);
		}
		entityManager.getTransaction().commit();
		entityManager.close();
		sqrlPersistence.closeCommit();
		return new SqrlAutoCloseablePersistence(createSqrlPersistence());
	}

	static EntityManagerFactory extractEntityManagerFactory(final SqrlPersistence sqrlPersistence) throws NoSuchFieldException {
		SqrlPersistence extracted = sqrlPersistence;
		if(extracted instanceof SqrlAutoCloseablePersistence) {
			extracted =  (SqrlPersistence) PrivateAccessor.getField(extracted, "sqrlPersistence");
		}
		return (EntityManagerFactory) PrivateAccessor.getField(extracted, "entityManagerFactory");
	}

	public static SqrlAutoCloseablePersistence createSqrlPersistence() {
		return new SqrlAutoCloseablePersistence(new SqrlConfigOperations(TCUtil.buildTestSqrlConfig()).getSqrlPersistenceFactory()
				.createSqrlPersistence());
	}

	/**
	 * Performs the SQRL required base64URL encoding
	 *
	 * @param toEncode
	 *            the string to be encoded
	 * @return the encoded string
	 */
	protected static String sqrlBase64UrlEncode(final String toEncode) {
		try {
			return sqrlBase64UrlEncode(toEncode.getBytes(SqrlConstants.UTF8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException ", e);
		}
	}

	public static String sqrlBase64UrlEncode(final byte[] bytes) {
		try {
			String encoded = new String(Base64.getUrlEncoder().encode(bytes), SqrlConstants.UTF8);
			while (encoded.endsWith("=")) {
				encoded = encoded.substring(0, encoded.length() - 1);
			}
			return encoded;
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException during base64 encode", e);
		}
	}

}
