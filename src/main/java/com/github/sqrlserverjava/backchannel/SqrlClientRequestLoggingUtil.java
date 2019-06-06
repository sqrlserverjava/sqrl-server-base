package com.github.sqrlserverjava.backchannel;

import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.CHANNEL;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.CLIENT_COMMAND;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.CLIENT_IP;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.CLIENT_PARAM;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.COR;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.IDK;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.OPT_LIST;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.POLL_BROWSER_STATE;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.POLL_TRANSPORT;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.POLL_UUID;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.PROCESS;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.PROTOCOL_VERSION;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.SERVER_PARAM;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.SQRL_AGENT;
import static com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField.USER_AGENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlConfig;
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
public class SqrlClientRequestLoggingUtil { // TODO: rename
	private static final Logger logger = LoggerFactory.getLogger(SqrlClientRequestLoggingUtil.class);
	private static final boolean LOG_EMPTY_FIELDS = false;
	private static SqrlConfig sqrlConfig = null;

	public enum Channel {
		FRONT, POLL, SQRLBC
	}

	// @formatter:off
	public enum LogField {
		CHANNEL("channel"),
		COR("cor"), 
		PROCESS("process"),
		CLIENT_IP("clientip"),
		POLL_UUID("polluuid"),
		USER_AGENT("useragent"),
		SQRL_AGENT("sqrlagent"),
		CLIENT_COMMAND("command"),
		PROTOCOL_VERSION("protover"),
		POLL_TRANSPORT("polltransport"),
		POLL_BROWSER_STATE("pollstate"), 
		IDK("idk"),
		CLIENT_PARAM("clientParam"),
		SERVER_PARAM("serverParam"),
		OPT_LIST("optList"),
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
	// Visible for unit testing
	protected static List<LogField> HEADER_FIELD_ORDER = Collections
			.unmodifiableList(Arrays.asList(CHANNEL, COR, PROCESS, CLIENT_COMMAND));
	// Visible for unit testing
	protected static List<LogField> FOOTER_FIELD_ORDER = Collections
			.unmodifiableList(Arrays.asList(POLL_BROWSER_STATE, USER_AGENT, CLIENT_IP, SQRL_AGENT, POLL_TRANSPORT,
					POLL_UUID, PROTOCOL_VERSION, IDK, OPT_LIST, CLIENT_PARAM, SERVER_PARAM));

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

	/**
	 * Internal use only.
	 */
	public static String formatForException(final Object... messageStringPartArray) {
		final String message = SqrlUtil.buildString(messageStringPartArray);
		final StringBuilder buf = new StringBuilder(300 + message.length());
		buf.append(message).append(" ");
		buf.append(tlHeader.get()).append(" ");
		buf.append(tlFooter.get());
		return buf.toString();
	}

	/**
	 * 
	 * @param message
	 * @param additionalFieldPairs
	 *            additional field=value pairs. Expected to be even in size
	 * @return
	 */
	public static String formatForLogging(final CharSequence message, final Object... additionalFieldPairs) {
		final StringBuilder buf = new StringBuilder(300 + message.length());
		buf.append(tlHeader.get());
		buf.append(" message=\"").append(message).append("\"");
		for (int i = 0; i < additionalFieldPairs.length; i += 2) {
			String name = "null";
			if (additionalFieldPairs[i] != null) {
				name = additionalFieldPairs[i].toString();
			}
			final boolean hasValue = i + 1 < additionalFieldPairs.length;
			if (hasValue) {
				append(buf, name, additionalFieldPairs[i + 1]);
			} else {
				// Just log the first item on it's own
				buf.append(name);
				if (logger.isDebugEnabled()) {
					// To cut down on noise, we only log this warning if debug is enabled as it's not a critical error
					logger.warn("Programmatic loggging error, additionalFieldPairs has odd number name='" + name + "'",
							new SqrlException("log debug stack"));
				}
			}
		}
		buf.append(" ").append(tlFooter.get());
		return buf.toString();
	}

	public static boolean isLogging() {
		return tlDataTable.get().containsKey(CHANNEL);
	}

	public static void initLogging(final Channel channel, final String process,
			final HttpServletRequest request) {
		tlDataTable.get().clear();
		putData(CHANNEL, channel.toString().toLowerCase());
		putData(PROCESS, process);
		// Common
		putData(CLIENT_IP, SqrlUtil.findBrowserIpAddressString(request, sqrlConfig));
		if (channel == Channel.SQRLBC) {
			putData(SQRL_AGENT, request.getHeader("User-Agent"));
		} else if (channel == Channel.POLL) {
			putData(USER_AGENT, buildSimpleUserAgent(request.getHeader("User-Agent")));
			final String correlator = request.getHeader("X-sqrl-corelator");
			if (SqrlUtil.isNotBlank(correlator)) {
				putData(COR, correlator);
			}
		} else if (channel == Channel.FRONT) {
			// No need to log the entire user agent string, the last token will identify the browser
			putData(USER_AGENT, buildSimpleUserAgent(request.getHeader("User-Agent")));
		} else {
			logger.error("Programmtic error: case not implemented for Channel " + channel);
		}
		rebuildHeader();
		rebuildFooter();
	}

	public static void cleanup() {
		tlDataTable.get().clear();
		tlHeader.set("");
		tlFooter.set("");
	}

	private static String buildSimpleUserAgent(final String fullUserAgentString) {
		if (SqrlUtil.isBlank(fullUserAgentString)) {
			return null;
		}
		final String[] parts = fullUserAgentString.split(" ");
		if (parts.length > 1) {
			// the last token will identify the browser
			return parts[parts.length - 1];
		} else {
			return fullUserAgentString;
		}
	}

	public static void putData(final LogField field, final Object valueParam) {
		String value = ""; // Put empty string for no value
		if (valueParam != null) {
			value = valueParam.toString();
		}
		tlDataTable.get().put(field, value);
		if (HEADER_FIELD_ORDER.contains(field)) {
			rebuildHeader();
		} else if (FOOTER_FIELD_ORDER.contains(field)) {
			rebuildFooter();
		} else {
			if (logger.isDebugEnabled()) { // log as warn but only if debug is enabled
				logger.warn("Programmatic error, logging field {} not found in header or footer list", field);
			}
		}
	}

	public static void putData(final LogField field1, final Object value1, final LogField field2, final Object value2) {
		putData(field1, value1);
		putData(field2, value2);
	}

	public static void putData(final LogField field1, final Object value1, final LogField field2, final Object value2,
			final LogField field3, final Object value3) {
		putData(field1, value1);
		putData(field2, value2);
		putData(field3, value3);
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

	private static void append(final StringBuilder buf, final String field, final Object valueParam) {
		String stringValue = "null";
		if (valueParam != null) {
			stringValue = valueParam.toString();
		}
		if (SqrlUtil.isNotBlank(stringValue)) {
			// Replace double quote with single quotes
			final String value = stringValue.replace("\"", "\\\"");
			buf.append(" ").append(field).append("=");
			if (value.contains(" ")) {
				buf.append("\"").append(value).append("\"");
			} else {
				buf.append(value);
			}
		} else if (LOG_EMPTY_FIELDS) {
			buf.append(" ").append(field).append("=-");
		}
	}
	public static void setLoggingField(final LogField logField, final String value) {
		tlDataTable.get().put(logField, value);
	}

	public static String[] buildParamArrayForLogging(final HttpServletRequest servletRequest) {
		final List<String> nameValueParamList = new ArrayList<>();

		for (final Map.Entry<String, String[]> entry : servletRequest.getParameterMap().entrySet()) {
			nameValueParamList.add(entry.getKey());
			if (entry.getValue().length == 1) {
				nameValueParamList.add(entry.getValue()[0]);
			} else {
				nameValueParamList.add(Arrays.toString(entry.getValue()));
			}
		}
		return nameValueParamList.toArray(new String[nameValueParamList.size()]);
	}

	public static void setSqrlConfig(final SqrlConfig sqrlConfig) {
		SqrlClientRequestLoggingUtil.sqrlConfig = sqrlConfig;
	}

}
