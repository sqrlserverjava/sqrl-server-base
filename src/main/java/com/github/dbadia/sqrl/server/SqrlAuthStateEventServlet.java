package com.github.dbadia.sqrl.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;

/**
 * @deprecated use {@link SqrlAuthStateAsyncSseServlet} instead
 * @author Dave Badias
 *
 */
@Deprecated
public class SqrlAuthStateEventServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(SqrlAuthStateEventServlet.class);
	private static SqrlServerOperations sqrlServerOperations = null;
	private static String correlatorCookieName = null;

	public static void init(final SqrlConfig sqrlConfig) {
		SqrlAuthStateEventServlet.correlatorCookieName = sqrlConfig.getCorrelatorCookieName();
		SqrlAuthStateEventServlet.sqrlServerOperations = new SqrlServerOperations(sqrlConfig);
	}

	@Override
	public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final String acceptHeader = request.getHeader("Accept");
		if (!"text/event-stream".equals(acceptHeader))  {
			logger.error("Recieved non SSE request: {}",acceptHeader);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		final String lastMsgId = request.getHeader("Last-Event-ID");
		logger.error("last event ID={}", lastMsgId);

		// Get the current SQRL state
		SqrlAuthenticationStatus clientStatus = null;
		SqrlAuthenticationStatus newStatus = null;
		final String sqrlStateParamValue = request.getParameter("sqrlstate");
		try {
			clientStatus = SqrlAuthenticationStatus.valueOf(sqrlStateParamValue);
			// Set them the same so, by default we will query the db
			newStatus = clientStatus;
		} catch (final RuntimeException e) {
			logger.warn("Received invalid sqrlStateParam from auth page: {}", sqrlStateParamValue, e);
			newStatus = SqrlAuthenticationStatus.ERROR_SQRL_INTERNAL;
		}

		final String sqrlCorrelatorString = SqrlUtil.findCookieValue(request, correlatorCookieName);
		if (sqrlCorrelatorString == null) {
			newStatus = SqrlAuthenticationStatus.ERROR_SQRL_INTERNAL;
		}

		// TODO: if client status == done or error
		// Poll the database for a different status
		while (clientStatus == newStatus) {
			try {
				Thread.sleep(500);
			} catch (final InterruptedException e) {
				// do nothing
			}

			final SqrlCorrelator sqrlCorrelator = sqrlServerOperations.fetchSqrlCorrelator(sqrlCorrelatorString);
			newStatus = sqrlCorrelator.getAuthenticationStatus();
		}
		final String reply = newStatus.toString();
		// State has changed, reply accordingly
		logger.debug("State changed, sending {}", reply);

		// TODO: if client status == done or error
		// Poll the database for a sText(reply);

		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "keep-alive");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter writer = response.getWriter();

		writer.write("data:");
		writer.write(reply);
		writer.write("\n\n");
		writer.flush();
		writer.close();
	}
}
