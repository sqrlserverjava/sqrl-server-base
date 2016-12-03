package com.github.dbadia.sqrl.server.exception;

/**
 * Indicates that data received is invalid
 * 
 * @author Dave Badia
 *
 */
public class SqrlInvalidDataException extends SqrlException {
	private static final long serialVersionUID = 7999271266503909263L;

	/**
	 * {@inheritDoc}
	 */
	public SqrlInvalidDataException(final String message, final Throwable e) {
		super(message, e);
	}

	/**
	 * {@inheritDoc}
	 */
	public SqrlInvalidDataException(final String message) {
		super(message);
	}
}
