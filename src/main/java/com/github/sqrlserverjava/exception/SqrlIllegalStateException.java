package com.github.sqrlserverjava.exception;

/**
 * Indicates an unrecoverable error has occurred, typically thrown during the initialization of SQRL
 * 
 * @author Dave Badia
 */
public class SqrlIllegalStateException extends IllegalStateException {
	private static final long serialVersionUID = -8274234408657792029L;

	public SqrlIllegalStateException(final String message) {
		super(message);
	}

	public SqrlIllegalStateException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
