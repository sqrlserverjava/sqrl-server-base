package com.github.dbadia.sqrl.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.util.SelfExpiringHashMap;

public class SqrlAuthStateMonitor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SqrlAuthStateMonitor.class);

	private final ClientAuthStateUpdater clientAuthStateUpdater;
	private final SqrlServerOperations sqrlServerOperations;
	/**
	 * Table of correlators to be monitored for state changes. key is a correlator string. Entries in this table expire
	 * automatically
	 */
	private final Map<String, CorrelatorToMonitor> monitorTable;

	private final Map<String, String> sessionIdToCorrelatorTable;

	public SqrlAuthStateMonitor(final SqrlConfig sqrlConfig, final SqrlServerOperations sqrlServerOperations,
			final ClientAuthStateUpdater clientAuthStateUpdater) {
		this.clientAuthStateUpdater = clientAuthStateUpdater;
		this.sqrlServerOperations = sqrlServerOperations;
		monitorTable = new SelfExpiringHashMap<>(TimeUnit.SECONDS.toMillis(sqrlConfig.getNutValidityInSeconds()));
		sessionIdToCorrelatorTable = new ConcurrentHashMap<>();
	}

	/**
	 * Add the given correlator to the list of monitored items for state changes
	 *
	 * @param browserSessionId
	 *            the browsers session ID
	 * @param correlatorString
	 *            our correlator ID of the session to monitor
	 * @param browserStatus
	 *            the current status as reported by the browser
	 */
	public void monitorCorrelatorForChange(final String browserSessionId, final String correlatorString,
			final SqrlAuthenticationStatus browserStatus) {
		final String oldCorrelatorString = sessionIdToCorrelatorTable.remove(browserSessionId);
		if (oldCorrelatorString != null) {
			// Since the browser sent a new correlator, we can stop monitoring the old one
			monitorTable.remove(oldCorrelatorString);
			// TODO: Warn if null
		}
		monitorTable.put(correlatorString, new CorrelatorToMonitor(browserSessionId, browserStatus));
		sessionIdToCorrelatorTable.put(browserSessionId, correlatorString);
	}

	public void stopMonitoringSessionId(final String browserSessionId) {
		final String correlatorString = sessionIdToCorrelatorTable.remove(browserSessionId);
		if (correlatorString != null && monitorTable.remove(correlatorString) == null) {
			logger.warn("Tried to remove browserSessionId {} from monitorTable but it wasn't present",
					correlatorString);
		}
	}

	public void stopMonitoringCorrelator(final String correlatorString) {
		if (correlatorString != null && monitorTable.remove(correlatorString) == null) {
			logger.warn("Tried to remove browserSessionId {} from monitorTable but it wasn't present",
					correlatorString);
		}
	}

	@Override
	public void run() {
		try {
			if (monitorTable.isEmpty()) {
				return;
			}

			// Map<String=correlator,...
			final Map<String, SqrlAuthenticationStatus> correlatorToCurrentStatusTable = new ConcurrentHashMap<>();
			for (final Map.Entry<String, CorrelatorToMonitor> entry : monitorTable.entrySet()) {
				correlatorToCurrentStatusTable.put(entry.getKey(), entry.getValue().browserStatus);
			}
			// Map<String=correlator,...
			final Map<String, SqrlAuthenticationStatus> statusChangedTable = sqrlServerOperations
					.fetchSqrlCorrelatorStatusUpdates(correlatorToCurrentStatusTable);
			for (final Map.Entry<String, SqrlAuthenticationStatus> entry : statusChangedTable.entrySet()) {
				final String correlator = entry.getKey();
				final SqrlAuthenticationStatus newState = entry.getValue();
				CorrelatorToMonitor correlatorToMonitor = null;
				if (newState.isUpdatesForThisCorrelatorComplete()) {
					// Since the status changed, we no longer need monitor it
					correlatorToMonitor = monitorTable.remove(correlator);
				} else {
					correlatorToMonitor = monitorTable.get(correlator);
				}
				if (correlatorToMonitor == null) {
					logger.error("Extracted null correlatorToMonitor from monitorTable for correlator {}",
							correlator);
				} else {
					clientAuthStateUpdater.pushStatusUpdateToBrowser(correlatorToMonitor.browserSessionId,
							correlatorToMonitor.browserStatus, entry.getValue());
				}
			}
		} catch (final Throwable t) { // Don't let anything escape
			logger.error("Caught exception in SqrlAuthMonitor.run()", t);
		}
	}

	private static class CorrelatorToMonitor {
		private final String browserSessionId;
		private final SqrlAuthenticationStatus browserStatus;

		public CorrelatorToMonitor(final String atmosphereSessionId,
				final SqrlAuthenticationStatus browserStatus) {
			super();
			this.browserSessionId = atmosphereSessionId;
			this.browserStatus = browserStatus;
		}
	}

}
