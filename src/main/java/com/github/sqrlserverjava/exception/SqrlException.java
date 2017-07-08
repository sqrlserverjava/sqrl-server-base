package com.github.sqrlserverjava.exception;

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
	 */
	public SqrlException(final String message, final Throwable e) {
		super(message, e);
	}

	/**
	 * {@inheritDoc}
	 */
	public SqrlException(final String message) {
		super(message);
	}

}
