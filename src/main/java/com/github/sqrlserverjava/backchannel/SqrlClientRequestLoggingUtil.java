package com.github.sqrlserverjava.backchannel;

import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.CHANNEL;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.COR;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.POLL_UUID;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.PROCESS;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.SQRL_AGENT;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.USER_AGENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.exception.SqrlException;
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
	private static final boolean LOG_EMPTY_FIELDS = false;

	public enum Channel {
		FRONT, POLL, SQRLBC
	}

	// @formatter:off
	public enum LogField {
		CHANNEL("channel"),
		COR("cor"), 
		PROCESS("process"),
		POLL_UUID("polluuid"),
		USER_AGENT("useragent"),
		SQRL_AGENT("sqrlagent"),
		CLIENT_COMMAND("command"),
		PROTOCOL("protocol"),
		;

		private String logFormat;
		private LogField(final String logFormat) {
			this.logFormat = logFormat;
		}
		public String logformat() {
			return logFormat;
		}

		// 
	}
	// @formatter:on

	private static List<LogField> HEADER_FIELD_ORDER = Collections
			.unmodifiableList(Arrays.asList(CHANNEL, COR, PROCESS));
	private static List<LogField> FOOTER_FIELD_ORDER = Collections.unmodifiableList(Arrays.asList(POLL_UUID));

	private SqrlClientRequestLoggingUtil() {
		// util class
	}

	private static final ThreadLocal<Map<LogField, String>> tlDataTable = new ThreadLocal<Map<LogField, String>>() {
		@Override
		protected Map<LogField, String> initialValue() {
			return new ConcurrentHashMap<LogField, String>();
		}
	};

	private static final ThreadLocal<String> tlHeader = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "";
		}
	};

	private static final ThreadLocal<String> tlFooter = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "";
		}
	};
	@Deprecated // TODO: delete
	private static final ThreadLocal<String> threadLocalLogHeader = new ThreadLocal<String>() {
		@Override
		protected String initialValue() {
			return "";
		}
	};

	/**
	 * Internal use only.
	 *
	 * @param logDataToAdd
	 *            the data to be appended to the current log header
	 * @return the updated logHeader for convience
	 */
	public static String formatForLogging(final CharSequence message) {
		return formatForLogging(message);
	}

	/**
	 * 
	 * @param message
	 * @param additionalFieldPairs
	 *            additional field=value pairs. Expected to be even in size
	 * @return
	 */
	public static String formatForLogging(final CharSequence message, final String... additionalFieldPairs) {
		final StringBuilder buf = new StringBuilder(300 + message.length());
		buf.append(tlHeader.get()).append(" ");
		buf.append(message);
		for (int i = 0; i < additionalFieldPairs.length; i++) {
			final String name = additionalFieldPairs[i];
			final boolean hasValue = i + 1 < additionalFieldPairs.length;
			if (hasValue) {
				append(buf, additionalFieldPairs[i], additionalFieldPairs[i + 1]);
			} else {
				// Just log the first item on it's own
				buf.append(name);
				if (logger.isDebugEnabled()) {
					// To cut down on noise, we only log this warning if debug is enabled as it's not a critical error
					logger.warn("Programmatic loggging error, additionalFieldPairs has odd number",
							new SqrlException("log debug stack"));
				}
			}
		}
		buf.append(tlFooter);
		return buf.toString();
	}

	public static void initLogging(final Channel channel, final String process,
			final HttpServletRequest servletRequest) {
		tlDataTable.get().clear();
		putData(CHANNEL, channel.toString().toLowerCase());
		putData(PROCESS, process);
		// Common

		if (channel == Channel.SQRLBC) {
			putData(SQRL_AGENT, servletRequest.getHeader("User-Agent"));
		} else if (channel == Channel.POLL) {
		} else if (channel == Channel.FRONT) {
			// No need to log the entire user agent string, the last token will identify the browser
			final String simpleUserAgent = servletRequest.getHeader("User-Agent").split(" ")[0];
			putData(USER_AGENT, simpleUserAgent);
		} else {
			logger.error("Programmtic error: case not implemented for Channel " + channel);
		}
		rebuildHeader();
		rebuildFooter();
	}

	private static void putData(final LogField field, final String valueParam) {
		String value = valueParam;
		if (SqrlUtil.isBlank(value)) {
			value = "-";
		}
		tlDataTable.get().put(field, value);
	}

	private static StringBuilder rebuildHeader() {
		return rebuild(tlHeader, HEADER_FIELD_ORDER);
	}

	private static StringBuilder rebuildFooter() {
		return rebuild(tlFooter, FOOTER_FIELD_ORDER);
	}

	private static StringBuilder rebuild(final ThreadLocal<String> setOn, final List<LogField> fieldList) {
		final StringBuilder buf = new StringBuilder(100);
		fieldList.stream().forEach(f -> append(buf, f));
		setOn.set(buf.toString());
		return buf;
	}

	private static void append(final StringBuilder buf, final LogField field) {
		append(buf, field.logformat(), tlDataTable.get().get(field));
	}

	@Deprecated // TODO: delete
	private static void append(final StringBuilder buf, final LogField field, final String value) {
		append(buf, field.logformat(), value);
	}

	private static void append(final StringBuilder buf, final String field, final String valueParam) {
		if (SqrlUtil.isNotBlank(valueParam)) {
			// Replace double quote with single quotes
			final String value = valueParam.replace("\"", "'");
			buf.append(" ").append(field).append("=");
			if (value.contains(" ")) {
				buf.append("\"").append(value).append("\"");
			}
		} else if (LOG_EMPTY_FIELDS) {
			buf.append(" ").append(field).append("=-");
		}
	}
	public static void setLoggingField(final LogField logField, final String value) {
		tlDataTable.get().put(logField, value);
	}

	/**
	 * @deprecated
	 */
	@Deprecated // TODO: delete
	public static String updateLogHeader(final CharSequence logDataToAdd) {
		final String currentLogHeader = threadLocalLogHeader.get();
		final String updatedLogHeader = new StringBuilder(currentLogHeader.length() + logDataToAdd.length() + 1)
				.append(currentLogHeader).append(" ").append(logDataToAdd).toString();
		threadLocalLogHeader.set(updatedLogHeader);
		return updatedLogHeader;
	}

	/**
	 * @deprecated
	 */
	@Deprecated // TODO: delete
	public static void clearLogHeader() {
		threadLocalLogHeader.remove();
	}

	/**
	 * @deprecated
	 */
	@Deprecated // TODO: delete
	public static String getLogHeader() {
		return threadLocalLogHeader.get();
	}

}
