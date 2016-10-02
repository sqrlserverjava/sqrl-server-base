package com.github.dbadia.sqrl.server;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;

// TODO: delete now that we have atomosphere
/**
 * An efficient, async io servlet for processing SSE (server side event) queries from the browser during SQRL auth, as
 * seen in the example app. To use this servlet, it must be added to your application's web.xml file with
 * async-supported=true set
 * 
 * @author Dave Badia
 * @deprecated use atmosphere handler instead
 */
@Deprecated
public class SqrlAuthStateAsyncSseServlet extends HttpServlet implements ServletContextListener {
	private static final long serialVersionUID = -9005876954442678700L;
	private static final Logger logger = LoggerFactory.getLogger(SqrlAuthStateAsyncSseServlet.class);
	// Use statics since the servlet can be created many times
	private static SqrlServerOperations sqrlServerOperations = null;
	private static SqrlAuthStateMonitor sqrlAuthStateMonitor = null;
	private static String correlatorCookieName = null;
	private static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
	private static ScheduledFuture executingTask = null;

	public static synchronized void init(final SqrlConfig sqrlConfig, final SqrlAuthStateMonitor sqrlAuthStateMonitor,
			final SqrlServerOperations sqrlServerOperations) {
		SqrlAuthStateAsyncSseServlet.correlatorCookieName = sqrlConfig.getCorrelatorCookieName();
		SqrlAuthStateAsyncSseServlet.sqrlAuthStateMonitor = sqrlAuthStateMonitor;
		SqrlAuthStateAsyncSseServlet.sqrlServerOperations = sqrlServerOperations;
		if (executingTask != null) {
			executingTask.cancel(false);
		}
		executingTask = scheduledExecutor.scheduleAtFixedRate(sqrlAuthStateMonitor, 1, 1, TimeUnit.SECONDS); // TODO:
	}

	@Override
	public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		if (SqrlAuthStateAsyncSseServlet.correlatorCookieName == null) {
			throw new IllegalStateException("SqrlAuthStateAsyncSseServlet.init() was not called");
		}
		final String acceptHeader = request.getHeader("Accept");
		if (!"text/event-stream".equals(acceptHeader))  {
			logger.error("Recieved non SSE request: {}",acceptHeader);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		final String lastMsgId = request.getHeader("Last-Event-ID");
		logger.error("last event ID={}", lastMsgId); // TODO: info or debug

		final AsyncContext asyncContext = request.startAsync();

		// Get the current SQRL state that the browser has
		String sqrlStateParamValue = request.getParameter("sqrlstate");
		if (sqrlStateParamValue == null) {
			sqrlStateParamValue = SqrlAuthenticationStatus.CORRELATOR_ISSUED.toString();
		}
		SqrlAuthenticationStatus clientStatus = null;
		SqrlAuthenticationStatus newStatus = null;

		final String correlatorString = SqrlUtil.findCookieValue(request, correlatorCookieName);
		if (correlatorString == null) {
			newStatus = SqrlAuthenticationStatus.ERROR_SQRL_INTERNAL;
		}

		try {
			clientStatus = SqrlAuthenticationStatus.valueOf(sqrlStateParamValue);
			// Set them the same so, by default we will query the db
		} catch (final RuntimeException e) {
			logger.warn("Received invalid sqrlStateParam from auth page: {}", sqrlStateParamValue, e);
			newStatus = SqrlAuthenticationStatus.ERROR_SQRL_INTERNAL;
		}
		if(newStatus != null) {
			// Error state, send the reply right away
			// SqrlAuthStateMonitor.sendSseResponse(asyncContext, SqrlAuthenticationStatus.ERROR_SQRL_INTERNAL);
		} else {
			// Let the monitor watch the db for correaltor chagne, then send the reply when it changes
			// sqrlAuthStateMonitor.monitorCorrelatorForChange(asyncContext, correlatorString, clientStatus);
		}
	}

	@Override
	public void contextDestroyed(final ServletContextEvent arg0) {
		scheduledExecutor.shutdown();
	}

	@Override
	public void contextInitialized(final ServletContextEvent arg0) {
		// do nothing
	}
}
