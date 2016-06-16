package com.github.dbadia.sqrl.server;

/**
 * Indicates a problem (such as data corruption, inaccessibility, etc) with the web apps SqrlDatastore
 * 
 * @author Dave Badia
 */
public class SqrlPersistenceException extends SqrlException {
	private static final long serialVersionUID = -3396203877892330273L;

	/**
	 * {@inheritDoc}
	 */
	public SqrlPersistenceException(final String message) {
		super(message);
	}

	/**
	 * {@inheritDoc}
	 */
	public SqrlPersistenceException(final String message, final Exception e) {
		super(message, e);
	}

}
