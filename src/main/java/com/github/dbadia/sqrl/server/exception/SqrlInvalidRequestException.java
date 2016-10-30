package com.github.dbadia.sqrl.server.exception;

import com.github.dbadia.sqrl.server.util.SqrlException;

/**
 * Indicates an invalid SQRL request was received
 * 
 * @author Dave Badia
 */
public class SqrlInvalidRequestException extends SqrlException {
	private static final long serialVersionUID = 5873378145444320669L;

	public SqrlInvalidRequestException(final String message, final Throwable e) {
		super(message, e);
	}

	public SqrlInvalidRequestException(final String message) {
		super(message);
	}

}
