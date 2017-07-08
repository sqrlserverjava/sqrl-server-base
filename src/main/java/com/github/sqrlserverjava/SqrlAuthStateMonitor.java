package com.github.sqrlserverjava;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;
import com.github.sqrlserverjava.util.SelfExpiringHashMap;

public class SqrlAuthStateMonitor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SqrlAuthStateMonitor.class);

	private final SqrlClientAuthStateUpdater			clientAuthStateUpdater;
	private final SqrlServerOperations					sqrlServerOperations;
	/**
	 * Table of correlators to be monitored for state changes. key is a correlator string, value is the auth status
	 * reported by the browser. Entries in this table expire automatically
	 */
	private final Map<String, SqrlAuthenticationStatus>	monitorTable;

	public SqrlAuthStateMonitor(final SqrlConfig sqrlConfig, final SqrlServerOperations sqrlServerOperations,
			final SqrlClientAuthStateUpdater clientAuthStateUpdater) {
		this.clientAuthStateUpdater = clientAuthStateUpdater;
		this.sqrlServerOperations = sqrlServerOperations;
		monitorTable = new SelfExpiringHashMap<>(TimeUnit.SECONDS.toMillis(sqrlConfig.getNutValidityInSeconds()));
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
	public void monitorCorrelatorForChange(final String correlatorString,
			final SqrlAuthenticationStatus browserStatus) {
		monitorTable.put(correlatorString, browserStatus);
	}

	public void stopMonitoringCorrelator(final String correlatorString) {
		if (correlatorString != null && monitorTable.remove(correlatorString) == null) {
			logger.debug(
					"Tried to remove correlator {} from monitorTable but it wasn't present, was probably already removed",
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
			final Map<String, SqrlAuthenticationStatus> statusChangedTable = sqrlServerOperations
					.fetchSqrlCorrelatorStatusUpdates(Collections.unmodifiableMap(monitorTable));
			for (final Map.Entry<String, SqrlAuthenticationStatus> entry : statusChangedTable.entrySet()) {
				final String correlator = entry.getKey();
				final SqrlAuthenticationStatus newState = entry.getValue();
				SqrlAuthenticationStatus oldStatus = null;
				if (newState.isUpdatesForThisCorrelatorComplete()) {
					oldStatus = monitorTable.remove(correlator);
				} else {
					oldStatus = monitorTable.get(correlator);
				}
				if (oldStatus == null) {
					logger.error("Extracted null oldStatus from monitorTable for correlator {}", correlator);
				} else {
					clientAuthStateUpdater.pushStatusUpdateToBrowser(correlator, oldStatus, entry.getValue());
				}
			}
		} catch (final Throwable t) { // Don't let anything escape
			logger.error("Caught exception in SqrlAuthMonitor.run()", t);
		}
	}
}
