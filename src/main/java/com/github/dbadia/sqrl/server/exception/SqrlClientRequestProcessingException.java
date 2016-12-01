package com.github.dbadia.sqrl.server.exception;

/**
 * Indicates that an error occurred (nut token timeout, invalid signature, etc) while processing a request from a SQRL
 * client, will trigger the setting of the provided tif on the response
 *
 * @author Dave Badia
 */
public class SqrlClientRequestProcessingException extends SqrlException {
	private static final long serialVersionUID = -7986435707384269525L;
	private final int			tifToAdd;

	public SqrlClientRequestProcessingException(final int tifToAdd, final String message) {
		super(message);
		this.tifToAdd = tifToAdd;
	}

	public int getTifToAdd() {
		return tifToAdd;
	}

}
