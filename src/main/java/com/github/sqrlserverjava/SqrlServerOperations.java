package com.github.sqrlserverjava;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.exception.SqrlIllegalStateException;
import com.github.sqrlserverjava.exception.SqrlPersistenceException;
import com.github.sqrlserverjava.persistence.SqrlAutoCloseablePersistence;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;
import com.github.sqrlserverjava.persistence.SqrlIdentity;
import com.github.sqrlserverjava.persistence.SqrlPersistenceCleanupTask;
import com.github.sqrlserverjava.util.SqrlServiceExecutor;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * The core SQRL class which processes all SQRL requests and generates the appropriates responses.
 * 
 * @author Dave Badia
 *
 */
public class SqrlServerOperations {
	private static final Logger logger = LoggerFactory.getLogger(SqrlServerOperations.class);

	private static final AtomicBoolean authStateMonitorInitialized = new AtomicBoolean(false);
	private static final AtomicBoolean dbCleanupInitialized = new AtomicBoolean(false);

	private static SqrlServiceExecutor sqrlServiceExecutor;
	static URL browserFacingUrlAndContextPath;

	private final SqrlConfig config;
	private final SqrlConfigOperations configOperations;
	private final SqrlPersistenceFactory persistenceFactory;

	private final SqrlBrowserFacingOperations sqrlBrowserFacingOperations;
	private final SqrlClientFacingOperations sqrlClientFacingOperations;

	/**
	 * Initializes the operations class with the given config, defaulting to the built in JPA persistence provider.
	 *
	 * @param config
	 *            the SQRL settings to be used
	 * @throws SqrlException
	 */
	public SqrlServerOperations(final SqrlConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("SqrlConfig object must not be null", null);
		}
		this.config = config;
		this.configOperations = SqrlConfigOperationsFactory.get(config);
		this.persistenceFactory = configOperations.getSqrlPersistenceFactory();
		this.sqrlBrowserFacingOperations = new SqrlBrowserFacingOperations(config, configOperations);
		this.sqrlClientFacingOperations = new SqrlClientFacingOperations(config, configOperations);

		// It's bad form to pass "this" to another object from our constructor since technically, we aren't
		// completely initialized. But here we do so as the only tasks left are administrative
		if (authStateMonitorInitialized.get() == false) {
			initializeSqrlClientAuthStateUpdater(config, this);
		}

		if (dbCleanupInitialized.get() == false) {
			initializeDbCleanup();
		}
	}

	private void initializeDbCleanup() {
		if (dbCleanupInitialized.get()) {
			return;
		}
		// DB Cleanup task
		final int cleanupIntervalInMinutes = config.getCleanupTaskExecInMinutes();
		if (cleanupIntervalInMinutes == -1) {
			logger.warn("Auto cleanup is disabled since config.getCleanupTaskExecInMinutes() == -1");
		} else if (cleanupIntervalInMinutes <= 0) {
			throw new IllegalArgumentException("config.getCleanupTaskExecInMinutes() must be -1 or > 0");
		} else {
			logger.info("Persistence cleanup task registered to run every {} minutes", cleanupIntervalInMinutes);
			final SqrlPersistenceCleanupTask cleanupRunnable = new SqrlPersistenceCleanupTask(persistenceFactory);
			// TODO: put executor somewhere else, state?
			sqrlServiceExecutor.scheduleAtFixedRate(cleanupRunnable, 0, cleanupIntervalInMinutes, TimeUnit.MINUTES);
		}
		dbCleanupInitialized.set(true);
	}

	private static synchronized void initializeSqrlClientAuthStateUpdater(final SqrlConfig config,
			final SqrlServerOperations serverOperations) {
		if (authStateMonitorInitialized.get()) {
			return;
		}
		final String classname = config.getClientAuthStateUpdaterClass();
		if (classname == null) {
			logger.warn("No ClientAuthStateUpdaterClass is set, auto client status refresh is disabled");
		} else {
			try {
				logger.info("Instantiating ClientAuthStateUpdater class of {}", classname);
				@SuppressWarnings("rawtypes")
				final Class clazz = Class.forName(classname);
				final Constructor<SqrlClientAuthStateUpdater> constructor = clazz.getConstructor();
				if (constructor == null) {
					throw new SqrlIllegalStateException("SQRL AuthStateUpdaterClass of " + classname
							+ " must have a no-arg constructor, but does not");
				}
				final Object object = constructor.newInstance();
				if (!(object instanceof SqrlClientAuthStateUpdater)) {
					throw new SqrlIllegalStateException("SQRL AuthStateUpdaterClass of " + classname
							+ " was not an instance of ClientAuthStateUpdater");
				}
				final SqrlClientAuthStateUpdater clientAuthStateUpdater = (SqrlClientAuthStateUpdater) object;
				final SqrlAuthStateMonitor authStateMonitor = new SqrlAuthStateMonitor(config, serverOperations, clientAuthStateUpdater);

				clientAuthStateUpdater.initSqrl(serverOperations, config, authStateMonitor);
				final long intervalInMilis = config.getAuthSyncCheckInMillis();
				logger.info("Client auth state task scheduled to run every {} ms", intervalInMilis);
				sqrlServiceExecutor.scheduleAtFixedRate(authStateMonitor, intervalInMilis, intervalInMilis,
						TimeUnit.MILLISECONDS);
			} catch (final Exception e) {
				throw new SqrlIllegalStateException(
						"SQRL: Error instantiating or initializing ClientAuthStateUpdaterClass of " + classname, e);
			}
		}
		authStateMonitorInitialized.set(true);
	}

	public SqrlBrowserFacingOperations browserFacingOperations() {
		return sqrlBrowserFacingOperations;
	}

	public SqrlClientFacingOperations clientFacingOperations() {
		return sqrlClientFacingOperations;
	}

	/**
	 * Poor mans dependency injection. Can't use CDI since we want to support lightweight JEE servers like tomcat
	 *
	 * @param sqrlServiceExecutor
	 */
	public static void setExecutor(final SqrlServiceExecutor sqrlServiceExecutor) {
		SqrlServerOperations.sqrlServiceExecutor = sqrlServiceExecutor;
	}

	// TODO: remove
	static void storeBrowserFacingUrlAndContextPathOLD(final HttpServletRequest request) throws SqrlException {
		URL currentRequestBrowserFacingUri;
		try {
			currentRequestBrowserFacingUri = new URI(request.getRequestURL().toString())
					.resolve(request.getContextPath()).toURL();
		} catch (final URISyntaxException | MalformedURLException e) {
			throw new SqrlException("Error computing currentRequestBrowserFacingUri", e);
		}
		if (browserFacingUrlAndContextPath == null) {
			logger.debug("Setting browserFacingUrlAndContextPath to {}", currentRequestBrowserFacingUri.toString());
			browserFacingUrlAndContextPath = currentRequestBrowserFacingUri;
		} else if (!browserFacingUrlAndContextPath.equals(currentRequestBrowserFacingUri)) {
			throw new SqrlException(SqrlUtil.buildString(
					"Found multiple browser facing login paths which is not currently supported: ",
					browserFacingUrlAndContextPath.toString(), " and ", currentRequestBrowserFacingUri.toString()));
		}
	}

	// TODO: remove
	static URL getBrowserFacingUrlAndContextPathOLD() {
		return browserFacingUrlAndContextPath;
	}

	// @formatter:off
	/*
	 * **************** Persistence layer interface **********************
	 * To isolate the web app from the full persistence API and transaction management, we expose the limited subset here
	 */
	// @formatter:on

	// TODO: rename to indlcude auto closeable
	static SqrlAutoCloseablePersistence createSqrlPersistence(final SqrlConfigOperations sqrlConfigOperations) {
		final SqrlPersistence sqrlPersistence = sqrlConfigOperations.getSqrlPersistenceFactory()
				.createSqrlPersistence();
		return new SqrlAutoCloseablePersistence(sqrlPersistence);
	}

	public void updateNativeUserXref(final SqrlIdentity sqrlIdentity, final String nativeUserCrossReference) {
		try (SqrlAutoCloseablePersistence sqrlPersistence = createSqrlPersistence(configOperations)) {
			sqrlPersistence.updateNativeUserXref(sqrlIdentity.getId(), nativeUserCrossReference);
			sqrlPersistence.closeCommit();
		}
	}

	/**
	 * Checks the request and trys to extract the correlator string from the cookie. Useful for error logging/reporting
	 * when a persistence call is unnecessary
	 *
	 * @return the value or null if the cookie was not present
	 */
	public String extractSqrlCorrelatorStringFromRequestCookie(final HttpServletRequest request) {
		return SqrlUtil.findCookieValue(request, config.getCorrelatorCookieName());
	}

	public SqrlCorrelator fetchSqrlCorrelator(final HttpServletRequest request) {
		final String correlatorString = extractSqrlCorrelatorStringFromRequestCookie(request);
		if (correlatorString == null) {
			return null;
		}
		return fetchSqrlCorrelator(correlatorString);
	}

	public SqrlCorrelator fetchSqrlCorrelator(final String correlatorString) {
		if (SqrlUtil.isBlank(correlatorString)) {
			throw new SqrlPersistenceException("Correlator cookie not found on request");
		}
		try (SqrlAutoCloseablePersistence sqrlPersistence = createSqrlPersistence(configOperations)) {
			final SqrlCorrelator sqrlCorrelator = sqrlPersistence.fetchSqrlCorrelator(correlatorString);
			sqrlPersistence.closeCommit();
			return sqrlCorrelator;
		}
	}

	/**
	 * Clears SQRL auth one time use data from the browser and database
	 */
	public void cleanSqrlAuthData(final HttpServletRequest request, final HttpServletResponse response) {
		final SqrlCorrelator sqrlCorrelator = fetchSqrlCorrelator(request);
		browserFacingOperations().deleteSqrlAuthCookies(request, response);
		deleteSqrlCorrelator(sqrlCorrelator);
	}

	/**
	 * Invoked to see if a web app user has a corresponding SQRL identity registered
	 *
	 * @param webAppUserCrossReference
	 *            the username, customer id, or whatever mechanism is used to uniquely identify users in the web app
	 * @return the SQRL identity or null if none exists for this web app user
	 */
	public SqrlIdentity fetchSqrlIdentityByUserXref(final String webAppUserCrossReference) {
		try (SqrlAutoCloseablePersistence sqrlPersistence = createSqrlPersistence(configOperations)) {
			final SqrlIdentity sqrlIdentity = sqrlPersistence.fetchSqrlIdentityByUserXref(webAppUserCrossReference);
			sqrlPersistence.closeCommit();
			return sqrlIdentity;
		}
	}

	public Map<String, SqrlCorrelator> fetchSqrlCorrelatorsDetached(final Set<String> correlatorStringSet) {
		try (SqrlAutoCloseablePersistence sqrlPersistence = createSqrlPersistence(configOperations)) {
			final Map<String, SqrlCorrelator> resultTable = sqrlPersistence
					.fetchSqrlCorrelatorsDetached(correlatorStringSet);
			sqrlPersistence.closeCommit();
			return resultTable;
		}
	}

	public Map<String, SqrlAuthenticationStatus> fetchSqrlCorrelatorStatusUpdates(
			final Map<String, SqrlAuthenticationStatus> correlatorToCurrentStatusTable) {
		try (SqrlAutoCloseablePersistence sqrlPersistence = createSqrlPersistence(configOperations)) {
			final Map<String, SqrlAuthenticationStatus> resultTable = sqrlPersistence
					.fetchSqrlCorrelatorStatusUpdates(correlatorToCurrentStatusTable);
			sqrlPersistence.closeCommit();
			return resultTable;
		}
	}

	public void deleteSqrlCorrelator(final SqrlCorrelator sqrlCorrelator) {
		try (SqrlAutoCloseablePersistence sqrlPersistence = createSqrlPersistence(configOperations)) {
			sqrlPersistence.deleteSqrlCorrelator(sqrlCorrelator);
			sqrlPersistence.closeCommit();
		}
	}
	
	public void shutdwon() {
		sqrlServiceExecutor.shutdown();
	}
}
