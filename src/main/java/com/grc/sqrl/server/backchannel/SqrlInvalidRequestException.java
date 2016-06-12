package com.grc.sqrl.server.backchannel;

import com.grc.sqrl.server.SqrlException;

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
