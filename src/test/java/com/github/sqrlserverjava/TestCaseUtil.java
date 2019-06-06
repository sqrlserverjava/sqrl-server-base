package com.github.sqrlserverjava;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import com.github.sqrlserverjava.backchannel.SqrlClientRequest;
import com.github.sqrlserverjava.backchannel.nut.SqrlNutToken;
import com.github.sqrlserverjava.backchannel.nut.TestCaseSqrlNutHelper;
import com.github.sqrlserverjava.enums.SqrlRequestCommand;
import com.github.sqrlserverjava.enums.SqrlRequestOpt;
import com.github.sqrlserverjava.enums.SqrlServerSideKey;
import com.github.sqrlserverjava.persistence.SqrlAutoCloseablePersistence;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;
import com.github.sqrlserverjava.persistence.SqrlJpaPersistenceProvider;
import com.github.sqrlserverjava.util.SqrlConstants;
import com.github.sqrlserverjava.util.SqrlServiceExecutor;
import com.github.sqrlserverjava.util.SqrlUtil;

import junitx.util.PrivateAccessor;

public class TestCaseUtil {
	public static final Date AWHILE_FROM_NOW = new Date(System.currentTimeMillis() + 1000000);
	static final String DEFAULT_CONFIG_SQRL_BACKCHANNEL_PATH = "http://127.0.0.1:8080/sqrlbc";
	static final String AES_TEST_KEY = Base64.getEncoder().encodeToString(new byte[16]);

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
		final SqrlConfig config = new TCSqrlConfig(System.currentTimeMillis());
		// Set SqrlServerOperations counter to generate the expected value
		// final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		// buffer.putInt(nutToken.getRandomInt());
		// config.setSecureRandom(new TestSecureRandom(buffer.array())); // TODO: fix or remove?
		config.setSecureRandom(new TestSecureRandom(null));

		config.setServerFriendlyName("SFN is DEPRECATED");
		config.setBackchannelServletPath("http://127.0.0.1:8080/sqrlbc");
		// set AES key to all zeros for test cases
		config.setAesKeyBase64(AES_TEST_KEY);
		config.setCpsCancelUri("www.google.com");

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
		config.setAesKeyBase64(AES_TEST_KEY);
		// TestSecureRandom isn't random at all which is very fast
		// If we didn't set a secure random, SecureRandom.getInstance will be called
		// which would slow down most of our test cases for no good reason
		config.setSecureRandom(new TestSecureRandom(null));
		config.setCpsCancelUri("www.google.com");

		return config;
	}

	public static SqrlConfig buildTestSqrlConfig(final boolean enableCps) {
		final SqrlConfig sqrlConfig = buildTestSqrlConfig();
		sqrlConfig.setEnableCps(enableCps);
		return sqrlConfig;
	}

	public static void setupSqrlPersistence(final String correlatorFromServerParam, final String serverParam)
			throws Throwable {
		final SqrlPersistence sqrlPersistence = TestCaseUtil.createEmptySqrlPersistence();
		final SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(correlatorFromServerParam,
				TestCaseUtil.AWHILE_FROM_NOW);
		if (serverParam != null) {
			sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		}
		sqrlPersistence.closeCommit();
	}

	/**
	 * @deprecated OK for test case use
	 */
	@Deprecated
	public static SqrlPersistence setupIdk(final String idk, final String correlator, final String serverParam) {
		final SqrlPersistence persistence = new SqrlJpaPersistenceProvider();
		final SqrlCorrelator sqrlCorrelator = persistence.createCorrelator(correlator, TestCaseUtil.AWHILE_FROM_NOW);
		if (serverParam != null) {
			sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, serverParam);
		}
		persistence.createAndEnableSqrlIdentity(idk);
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
	 *            a URI string from the GRC client log such as
	 *            "client=123&server=456&ids=789"
	 * @param requestIpOnServletRequest
	 * @return
	 * @throws URISyntaxException
	 */
	public static MockHttpServletRequest buildMockRequest(final String requestUrl, final String mockDataParams,
			final String requestIpOnServletRequest)
					throws URISyntaxException {
		final MockHttpServletRequest mockRequest = buildMockRequest(requestUrl);
		for (final String nameValuePair : mockDataParams.split("&")) {
			final String[] parts = nameValuePair.split("=");
			mockRequest.addParameter(parts[0], parts[1]);
		}
		if (SqrlUtil.isNotBlank(requestIpOnServletRequest)) {
			mockRequest.setRemoteAddr(requestIpOnServletRequest);
		}
		return mockRequest;
	}

	public static MockHttpServletRequest buildMockRequest(final String requestUrl, final String mockDataParams)
			throws URISyntaxException {
		return buildMockRequest(requestUrl, mockDataParams, null);
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

	public static SqrlNutToken buildValidSqrlNut(final SqrlConfig config) throws Exception {
		return buildValidSqrlNut(config, System.currentTimeMillis());
	}

	public static SqrlNutToken buildValidSqrlNut(final SqrlConfig config, final LocalDateTime nutIssuedAt)
			throws Exception {
		final long timestamp = nutIssuedAt.toEpochSecond(ZoneOffset.UTC);
		return TestCaseSqrlNutHelper.buildValidSqrlNut(timestamp, config);
	}

	public static SqrlNutToken buildValidSqrlNut(final SqrlConfig config, final long timestamp)
			throws Exception {
		return TestCaseSqrlNutHelper.buildValidSqrlNut(timestamp, config);
	}

	public static SqrlConfigOperations buildSqrlConfigOperations(final SqrlConfig config) {
		return new SqrlConfigOperations(config);
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

	static EntityManagerFactory extractEntityManagerFactory(final SqrlPersistence sqrlPersistence)
			throws NoSuchFieldException {
		SqrlPersistence extracted = sqrlPersistence;
		if (extracted instanceof SqrlAutoCloseablePersistence) {
			extracted = (SqrlPersistence) PrivateAccessor.getField(extracted, "sqrlPersistence");
		}
		return (EntityManagerFactory) PrivateAccessor.getField(extracted, "entityManagerFactory");
	}

	public static SqrlAutoCloseablePersistence createSqrlPersistence() {
		return new SqrlAutoCloseablePersistence(new SqrlConfigOperations(TestCaseUtil.buildTestSqrlConfig())
				.getSqrlPersistenceFactory().createSqrlPersistence());
	}

	/**
	 * Performs the SQRL required base64URL encoding
	 *
	 * @param toEncode
	 *            the string to be encoded
	 * @return the encoded string
	 */
	protected static String sqrlBase64UrlEncode(final String toEncode) {
		return sqrlBase64UrlEncode(toEncode.getBytes(SqrlConstants.UTF8_CHARSET));
	}

	public static String sqrlBase64UrlEncode(final byte[] bytes) {
		final String encoded = new String(Base64.getUrlEncoder().encode(bytes), SqrlConstants.UTF8_CHARSET);
		return encoded;
	}

	public static void setSqrlServerOpsBrowserFacingUrl(final URL browserFacingUrlAndContextPath) {
		SqrlServerOperations.browserFacingUrlAndContextPath = browserFacingUrlAndContextPath;
	}

	public static void clearStaticFields() {
		SqrlServerOperations.browserFacingUrlAndContextPath = null;
	}

	public static SqrlClientRequest buildMockSqrlRequest(final String idk, final SqrlRequestCommand command,
			final String correlator, final boolean hasUrsSignature, final SqrlRequestOpt... optArray) {
		final SqrlClientRequest mock = Mockito.mock(SqrlClientRequest.class);
		Mockito.when(mock.containsUrs()).thenReturn(false);
		Mockito.when(mock.getKey(SqrlServerSideKey.idk)).thenReturn(idk);
		Mockito.when(mock.getCorrelator()).thenReturn(correlator);
		Mockito.when(mock.getClientCommand()).thenReturn(command);
		Mockito.when(mock.containsUrs()).thenReturn(hasUrsSignature);
		final Set<SqrlRequestOpt> optList = new HashSet<>();
		optList.addAll(Arrays.asList(optArray));
		Mockito.when(mock.getOptList()).thenReturn(optList);
		return mock;
	}

}
