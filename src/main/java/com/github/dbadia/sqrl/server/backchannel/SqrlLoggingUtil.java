package com.github.dbadia.sqrl.server.backchannel;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.util.SqrlUtil;

/**
 * Internal use only.
 * <p/>
 * SQRL Logging util class to include transaction data on SQRL logging statements
 *
 * @author Dave Badia
 *
 */
public class SqrlLoggingUtil {
	private static final Logger logger = LoggerFactory.getLogger(SqrlLoggingUtil.class);

	private static final ThreadLocal<String> threadLocalLogHeader = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "";
		}
	};

	private SqrlLoggingUtil() {
		// util class
	}

	public static void initLoggingHeader(final HttpServletRequest servletRequest) {
		String sqrlAgentString = "unknown";
		final String header = servletRequest.getHeader("user-agent");
		if (SqrlUtil.isNotBlank(header)) {
			sqrlAgentString = header;
			logger.trace("setting sqrlagent on thread local to {}", sqrlAgentString);
		}
		threadLocalLogHeader.set(new StringBuilder(sqrlAgentString).append(" ").toString());
	}

	/**
	 * Internal use only.
	 *
	 * @param logHeader
	 *            the data to be appended to the current log header
	 * @return the updated logHeader for convience
	 */
	public static String updateLogHeader(final String logHeader) {
		threadLocalLogHeader.set(threadLocalLogHeader.get() + " " + logHeader);
		return logHeader;
	}

	public static void clearLogHeader() {
		threadLocalLogHeader.remove();
	}

	public static String getLogHeader() {
		return threadLocalLogHeader.get();
	}
}
