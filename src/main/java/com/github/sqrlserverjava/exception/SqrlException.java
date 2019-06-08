package com.github.sqrlserverjava.exception;

import com.github.sqrlserverjava.backchannel.LoggingUtil;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * SQRL exception class; all exceptions in this framework extend this class
 *
 * @author Dave Badia
 *
 */
public class SqrlException extends Exception {
	private static final long serialVersionUID = -693580346221526789L;

	public SqrlException(final Throwable e, final CharSequence... messagePartArray) {
		super(LoggingUtil.formatForException((Object[]) messagePartArray), e);
	}

	/**
	 * {@inheritDoc}
	 */
	public SqrlException(final CharSequence... messagePartArray) {
		this(null, messagePartArray);
	}

	// TODO: is this used?
	static String buildMessageWithHeader(final CharSequence[] originalArray, final CharSequence header) {
		final int originalArrayLength = originalArray.length;
		final CharSequence[] newArray = new CharSequence[originalArrayLength + 1];
		newArray[0] = header;
		System.arraycopy(originalArray, 0, newArray, 1, originalArrayLength);
		return SqrlUtil.buildString((Object[]) newArray);
	}
}
