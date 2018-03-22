package com.github.sqrlserverjava.exception;

import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * SQRL exception class; all exceptions in this framework extend this class
 *
 * @author Dave Badia
 *
 */
public class SqrlException extends Exception {
	private static final long serialVersionUID = -693580346221526789L;

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated TODO
	 */
	@Deprecated
	public SqrlException(final String message, final Throwable e) {
		super(message, e);
	}

	public SqrlException(final Throwable e, final Object... messagePartArray) {
		super(SqrlUtil.buildString(messagePartArray), e);
	}

	/**
	 * {@inheritDoc}
	 */
	public SqrlException(final Object... messagePartArray) {
		super(SqrlUtil.buildString(messagePartArray));
	}

	static String buildMessageWithHeader(Object[] originalArray, String header) {
		int originalArrayLength = originalArray.length;
		Object[] newArray = new Object[originalArrayLength + 1];
		newArray[0] = header;
		System.arraycopy(originalArray, 0, newArray, 1, originalArrayLength);
		return SqrlUtil.buildString(newArray);
	}
}
