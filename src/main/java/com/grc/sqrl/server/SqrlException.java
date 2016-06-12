package com.grc.sqrl.server;

/**
 * SQRL exception class
 * 
 * @author Dave Badia
 *
 */
public class SqrlException extends Exception {
	private static final long serialVersionUID = -693580346221526789L;

	public SqrlException(final String message, final Throwable e) {
		super(message, e);
	}

	public SqrlException(final String message) {
		super(message);
	}

}
