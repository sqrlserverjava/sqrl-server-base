package com.github.dbadia.sqrl.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;
import com.github.dbadia.sqrl.server.util.SelfExpiringHashMap;

public class SqrlAuthStateMonitor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SqrlAuthStateMonitor.class);
	/**
	 * Lock object to be used when {@link #pendingMonitorTable} is accessed
	 */
	private static final Object PENDING_MONITOR_TABLE_LOCK = new Object();
	private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
	private static ScheduledFuture monitorFuture = null;

	private final List<ClientAuthStateUpdater> updaterList = new ArrayList<>();

	private final SqrlConfig sqrlConfig;
	private final SqrlServerOperations sqrlServerOperations;
	/**
	 * Table of correlators to be monitored for state changes. key is a correlator string. Entries in this table expire
	 * automatically
	 */
	private final Map<String, CorrelatorToMonitor> monitorTable;

	/**
	 * Table of correlators that need to be added to {@link #monitorTable}; stored in a separate table to minimize
	 * synchronization overhead
	 */
	private Map<String, CorrelatorToMonitor> pendingMonitorTable = new ConcurrentHashMap<>();



	public SqrlAuthStateMonitor(final SqrlConfig sqrlConfig, final SqrlServerOperations sqrlServerOperations) {
		this.sqrlConfig = sqrlConfig;
		this.sqrlServerOperations = sqrlServerOperations;
		monitorTable = new SelfExpiringHashMap<>(sqrlConfig.getNutValidityInSeconds());
	}

	public void registerUpdater(final ClientAuthStateUpdater clientAuthStateUpdater) {
		if (clientAuthStateUpdater != null) {
			updaterList.add(clientAuthStateUpdater);
			if (updaterList.size() == 1 && monitorFuture == null) {
				monitorFuture = EXECUTOR_SERVICE.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS); // TODO:
				// configurable
			}
		}
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
		synchronized (PENDING_MONITOR_TABLE_LOCK) {
			pendingMonitorTable.put(correlatorString,
					new CorrelatorToMonitor(atmosphereSessionId, correlatorString, browserStatus));
		}
	}


	@Override
	public void run() {
		// A temporary reference so we can free up pendingWatchTable
		Map<String, CorrelatorToMonitor> tempTable = null;
		// "Steal" pendingMonitorTable as tempTable and create a new empty table for pendingMonitorTable
		synchronized (PENDING_MONITOR_TABLE_LOCK) {
			tempTable = pendingMonitorTable;
			pendingMonitorTable = new ConcurrentHashMap<>();
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
				for (final ClientAuthStateUpdater clientUpdater : updaterList) {
					clientUpdater.pushStatusUpdateToBrowser(correlatorToMonitor.browserSessionId,
							correlatorToMonitor.browserStatus, entry.getValue());
				}
			}
		}
	}

	private static class CorrelatorToMonitor {
		private final String browserSessionId;
		private final String correlatorString;
		private final SqrlAuthenticationStatus browserStatus;
		private long id; // assigned later
		private SqrlCorrelator detachedCorrelator; // assigned later

		public CorrelatorToMonitor(final String atmosphereSessionId, final String correlatorString,
				final SqrlAuthenticationStatus browserStatus) {
			super();
			this.browserSessionId = atmosphereSessionId;
			this.correlatorString = correlatorString;
			this.browserStatus = browserStatus;
		}
	}
}
