package com.github.dbadia.sqrl.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;

public class SqrlAuthStateMonitor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SqrlAuthStateMonitor.class);
	private static final Object NEW_LIST_LOCK = new Object();

	private final SqrlConfig sqrlConfig;
	private final SqrlServerOperations sqrlServerOperations;
	private final Map<String, CorrelatorToMonitor> monitorTable = new ConcurrentHashMap<>();

	private Map<String, CorrelatorToMonitor> newToMonitorTable= new ConcurrentHashMap<>(); // TODO: pendingWatchTable

	public SqrlAuthStateMonitor(final SqrlConfig sqrlConfig, final SqrlServerOperations sqrlServerOperations) {
		this.sqrlConfig = sqrlConfig;
		this.sqrlServerOperations = sqrlServerOperations;
	}

	public void monitorCorrelatorForChange(final AsyncContext asyncContext, final String correlatorString, final SqrlAuthenticationStatus browserStatus) {
		synchronized (NEW_LIST_LOCK) {
			newToMonitorTable.put(correlatorString, new CorrelatorToMonitor(asyncContext, correlatorString, browserStatus));
		}
	}

	@Override
	public void run() {
		Map<String, CorrelatorToMonitor> ourTable = null;
		// "Steal" newToMonitorTable as ourTable and create a new empty table for newToMonitorTable
		synchronized (NEW_LIST_LOCK) {
			ourTable = newToMonitorTable;
			newToMonitorTable = new ConcurrentHashMap<>();
		}

		if (!ourTable.isEmpty()) {
			// For each item, fetch the detached SqrlCorrelator object and assign them to CorrealtorToMonitor
			final Map<String, SqrlCorrelator> detachedCorrealtorTable = sqrlServerOperations
					.fetchSqrlCorrelatorsDetached(ourTable.keySet());
			for (final Entry<String, CorrelatorToMonitor> entry : ourTable.entrySet()) {
				final SqrlCorrelator detachedCorrelator = detachedCorrealtorTable.get(entry.getKey());
				if (detachedCorrelator != null) {
					final String correaltorString = entry.getKey();
					final CorrelatorToMonitor toMonitor = ourTable.get(correaltorString);
					toMonitor.detachedCorrelator = detachedCorrelator;
					// Add it to monitorTable
					monitorTable.put(correaltorString, toMonitor);
				}
			}
		}
		// Now that we've moved ourTable to monitorTable, clear ourTable
		ourTable = null;
		// TODO: Clean up expired correlators every X minutes

		if (monitorTable.isEmpty()) {
			return;
		}

		final Map<String, SqrlAuthenticationStatus> correlatorToCurrentStatusTable = new ConcurrentHashMap<>();
		for(final Map.Entry<String, CorrelatorToMonitor> entry : monitorTable.entrySet()) {
			correlatorToCurrentStatusTable.put(entry.getKey(), entry.getValue().browserStatus);
		}
		final Map<String, SqrlAuthenticationStatus> newStatusTable = sqrlServerOperations.fetchSqrlCorrelatorStatusChanged(correlatorToCurrentStatusTable);
		for(final Map.Entry<String, SqrlAuthenticationStatus> entry : newStatusTable.entrySet()) {
			final CorrelatorToMonitor correlatorToMonitor = monitorTable.remove(entry.getKey());
			if (correlatorToMonitor == null) {
				logger.warn("Extracted null correlatorToMonitor from monitorTable");
			} else {
				// TODO: info
				logger.error("State changed from {} to {}, sending SSE response", correlatorToMonitor.browserStatus,
						entry.getValue());
				sendSseResponse(correlatorToMonitor.asyncContext, entry.getValue());
			}
		}
	}


	public static void sendSseResponse(final AsyncContext asyncContext, final SqrlAuthenticationStatus newAuthStatus) {
		final HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "keep-alive");
		response.setCharacterEncoding("UTF-8");

		try {
			final PrintWriter writer = response.getWriter();

			writer.write("data:");
			writer.write(newAuthStatus.toString());
			writer.write("\n\n");
			writer.flush();
			writer.close();
		} catch (final IOException e) {
			logger.error("Error sending SSE auth status reply", e);
		}

		// Since we are sending the response ourselves we have to call complete
		asyncContext.complete();
	}


	private static class CorrelatorToMonitor {
		private final AsyncContext asyncContext;
		private final String correlatorString;
		private final SqrlAuthenticationStatus browserStatus;
		private long id; // assigned later
		private SqrlCorrelator detachedCorrelator; // assigned later

		public CorrelatorToMonitor(final AsyncContext asyncContext, final String correlatorString,
				final SqrlAuthenticationStatus browserStatus) {
			super();
			this.asyncContext = asyncContext;
			this.correlatorString = correlatorString;
			this.browserStatus = browserStatus;
		}
	}


}
