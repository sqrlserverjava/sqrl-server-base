package com.github.dbadia.sqrl.server.exception;

import com.github.dbadia.sqrl.server.backchannel.SqrlLoggingUtil;

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
		super(new StringBuilder(SqrlLoggingUtil.getLogHeader()).append(message).toString());
		this.tifToAdd = tifToAdd;
	}

	public int getTifToAdd() {
		return tifToAdd;
	}

}
