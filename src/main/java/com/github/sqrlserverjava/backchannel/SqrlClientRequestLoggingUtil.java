package com.github.sqrlserverjava.backchannel;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * Internal use only.
 * <p/>
 * SQRL Logging util class to include transaction data on SQRL logging statements
 *
 * @author Dave Badia
 *
 */
public class SqrlClientRequestLoggingUtil {
	private static final Logger logger = LoggerFactory.getLogger(SqrlClientRequestLoggingUtil.class);

	private SqrlClientRequestLoggingUtil() {
		// util class
	}


	private static final ThreadLocal<String> threadLocalLogHeader = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "";
		}
	};

	// TODO: fix this, store nvp, then add to header string, make new inner class
	public static void initLoggingHeader(final HttpServletRequest servletRequest) {
		String sqrlAgentString = "-";
		final String header = servletRequest.getHeader("user-agent");
		if (SqrlUtil.isNotBlank(header)) {
			sqrlAgentString = header;
			logger.trace("setting sqrlagent on thread local to {}", sqrlAgentString);
		}
		// - for unknown protocol version
		threadLocalLogHeader.set(new StringBuilder(50).append("\"").append(sqrlAgentString).append("\" ").toString());
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
