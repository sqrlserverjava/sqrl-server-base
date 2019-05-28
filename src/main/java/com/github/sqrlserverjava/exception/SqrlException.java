package com.github.sqrlserverjava.exception;

import com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * SQRL exception class; all exceptions in this framework extend this class
 *
 * @author Dave Badia
 *
 */
public class SqrlException extends Exception {
	private static final long serialVersionUID = -693580346221526789L;

	public SqrlException(final Throwable e, final Object... messagePartArray) {
		super(SqrlClientRequestLoggingUtil.formatForException(messagePartArray), e);
	}

	/**
	 * {@inheritDoc}
	 */
	public SqrlException(final CharSequence... messagePartArray) {
		this(null, messagePartArray);
	}

	static String buildMessageWithHeader(final Object[] originalArray, final CharSequence header) {
		final int originalArrayLength = originalArray.length;
		final Object[] newArray = new Object[originalArrayLength + 1];
		newArray[0] = header;
		System.arraycopy(originalArray, 0, newArray, 1, originalArrayLength);
		return SqrlUtil.buildString(newArray);
	}
}
