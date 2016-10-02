package com.github.dbadia.sqrl.server;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;
import com.github.dbadia.sqrl.server.util.SelfExpiringHashMap;

public class SqrlAuthStateMonitor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SqrlAuthStateMonitor.class);

	private final SqrlConfig sqrlConfig;
	private final SqrlServerOperations sqrlServerOperations;
	/**
	 * Table of correlators to be monitored for state changes. key is a correlator string. Entries in this table expire
	 * automatically
	 */
	private final Map<String, CorrelatorToMonitor> monitorTable;

	/**
	 * Table of atmosphere sessionId to most current AtmosphereResource request object. This is necessary as certain
	 * polling mechanisms, such as long polling can timeout and result in subsequent requests. This ensure we send our
	 * reply to the current request instead of a stale one
	 */
	private final Map<String, AtmosphereResource> currentAtmosphereRequestTable;

	/**
	 * Table of correlators that need to be added to {@link #monitorTable}; stored in a separate table to minimize
	 * synchronization overhead
	 */
	private volatile Map<String, CorrelatorToMonitor> pendingWatchTable = new ConcurrentHashMap<>();

	/**
	 * Lock object to be used when {@link #pendingWatchTable} is accessed
	 */
	private static final Object PENDING_WATCH_TABLE_LOCK = new Object();

	public SqrlAuthStateMonitor(final SqrlConfig sqrlConfig, final SqrlServerOperations sqrlServerOperations) {
		this.sqrlConfig = sqrlConfig;
		this.sqrlServerOperations = sqrlServerOperations;
		monitorTable = new SelfExpiringHashMap<>(sqrlConfig.getNutValidityInSeconds());
		currentAtmosphereRequestTable = new SelfExpiringHashMap<>(sqrlConfig.getNutValidityInSeconds());
	}

	/**
	 * Add the given correlator to the list of monitored items for state changes
	 *
	 * @param atmosphereSessionId
	 *            the browsers atmosphere atmosphereSessionId
	 * @param correlatorString
	 *            our correlator ID of the session to monitor
	 * @param browserStatus
	 *            the current status as reported by the browser
	 */
	public void monitorCorrelatorForChange(final String atmosphereSessionId, final String correlatorString,
			final SqrlAuthenticationStatus browserStatus) {
		synchronized (PENDING_WATCH_TABLE_LOCK) {
			pendingWatchTable.put(correlatorString,
					new CorrelatorToMonitor(atmosphereSessionId, correlatorString, browserStatus));
		}
	}

	/**
	 * Certain atmosphere polling mechanisms (long polling, etc)timeout and result in subsequent polling requests. This
	 * method must be called each time a new request is received so we can send our response to the current, valid
	 * resource object
	 *
	 * @param resource
	 *            the atmosphere resource that was received
	 */
	public void updateCurrentAtomosphereRequest(final AtmosphereResource resource) {
		final String atmosphereSessionId = extractAtmosphereSessionId(resource);
		if (logger.isDebugEnabled()) {
			logger.debug("In updateCurrentAtomosphereRequest for atmosphereSessionId {}, update? {}",
					atmosphereSessionId, currentAtmosphereRequestTable.containsKey(atmosphereSessionId));
		}
		currentAtmosphereRequestTable.put(atmosphereSessionId, resource);
	}

	@Override
	public void run() {
		// A temporary reference so we can free up pendingWatchTable
		Map<String, CorrelatorToMonitor> tempTable = null;
		// "Steal" newToMonitorTable as ourTable and create a new empty table for newToMonitorTable
		synchronized (PENDING_WATCH_TABLE_LOCK) {
			tempTable = pendingWatchTable;
			pendingWatchTable = new ConcurrentHashMap<>();
		}

		if (!tempTable.isEmpty()) {
			// For each item, fetch the detached SqrlCorrelator object and assign them to CorrealtorToMonitor
			final Map<String, SqrlCorrelator> detachedCorrealtorTable = sqrlServerOperations
					.fetchSqrlCorrelatorsDetached(tempTable.keySet());
			for (final Entry<String, CorrelatorToMonitor> entry : tempTable.entrySet()) {
				final SqrlCorrelator detachedCorrelator = detachedCorrealtorTable.get(entry.getKey());
				if (detachedCorrelator != null) {
					final String correaltorString = entry.getKey();
					final CorrelatorToMonitor toMonitor = tempTable.get(correaltorString);
					toMonitor.detachedCorrelator = detachedCorrelator;
					// Add it to monitorTable
					monitorTable.put(correaltorString, toMonitor);
				}
			}
		}
		// We're done with tempTable, all have been added to monitorTable
		tempTable = null;

		if (monitorTable.isEmpty()) {
			return;
		}

		final Map<String, SqrlAuthenticationStatus> correlatorToCurrentStatusTable = new ConcurrentHashMap<>();
		for(final Map.Entry<String, CorrelatorToMonitor> entry : monitorTable.entrySet()) {
			correlatorToCurrentStatusTable.put(entry.getKey(), entry.getValue().browserStatus);
		}
		final Map<String, SqrlAuthenticationStatus> statusChangedTable = sqrlServerOperations.fetchSqrlCorrelatorStatusChanged(correlatorToCurrentStatusTable);
		for(final Map.Entry<String, SqrlAuthenticationStatus> entry : statusChangedTable.entrySet()) {
			// Since the status changed, we no longer monitor it
			final CorrelatorToMonitor correlatorToMonitor = monitorTable.remove(entry.getKey());
			if (correlatorToMonitor == null) {
				logger.error("Extracted null correlatorToMonitor from monitorTable for correlator {}", entry.getKey());
			} else {
				logger.info("State changed from {} to {}, sending atmosphere response",
						correlatorToMonitor.browserStatus, entry.getValue());
				sendAtmostphereResponse(correlatorToMonitor.atmosphereSessionId, correlatorToMonitor.browserStatus,
						entry.getValue());
			}
		}
	}

	public void sendAtmostphereResponse(final String atmosphereSessionId, final SqrlAuthenticationStatus oldAuthStatus,
			final SqrlAuthenticationStatus newAuthStatus) {
		final AtmosphereResource resource = currentAtmosphereRequestTable.get(atmosphereSessionId);
		if (resource == null) {
			logger.error("AtmosphereResource not found for sessionId {}, can't communicate status change from {} to {}",
					atmosphereSessionId, oldAuthStatus, newAuthStatus);
			return;
		}
		final AtmosphereResponse response = resource.getResponse();
		logger.error("Sending atmosphere state change from {} to  {} via {} to {}, ", oldAuthStatus,
				newAuthStatus, resource.transport(), atmosphereSessionId);
		try {
			response.getWriter().write(newAuthStatus.toString());
			switch (resource.transport()) {
				case JSONP:
				case LONG_POLLING:
					resource.resume();
					break;
				case WEBSOCKET:
					break;
				case SSE: // this is not in the original examples but is necessary for SSE
				case STREAMING:
					response.getWriter().flush();
					break;
				default:
					// No point in throwing an exception since we are running in a separate thread.
					// Just log the error
					logger.error("Don't know how to handle transport {} for atmosphereSessionId {}",
							resource.transport(), atmosphereSessionId);
			}
		} catch (final Exception e) {
			logger.error(new StringBuilder("Caught IO error trying to send status of ").append(newAuthStatus)
					.append(" via atmosphere to atmosphereSessionId ").append(atmosphereSessionId)
					.append(" with transport ")
					.append(resource.transport()).toString(), e);
		}
	}

	public static String extractAtmosphereSessionId(final AtmosphereResource resource) {
		return resource.getRequest().getRequestedSessionId();
	}

	private static class CorrelatorToMonitor {
		private final String atmosphereSessionId;
		private final String correlatorString;
		private final SqrlAuthenticationStatus browserStatus;
		private long id; // assigned later
		private SqrlCorrelator detachedCorrelator; // assigned later

		public CorrelatorToMonitor(final String atmosphereSessionId, final String correlatorString,
				final SqrlAuthenticationStatus browserStatus) {
			super();
			this.atmosphereSessionId = atmosphereSessionId;
			this.correlatorString = correlatorString;
			this.browserStatus = browserStatus;
		}
	}
}
