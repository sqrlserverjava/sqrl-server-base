package com.github.dbadia.sqrl.server.exception;

import com.github.dbadia.sqrl.server.backchannel.SqrlLoggingUtil;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif;

/**
 * Indicates that an error occurred (nut token timeout, invalid signature, etc) while processing a request from a SQRL
 * client, will trigger the setting of the provided tif on the response
 *
 * @author Dave Badia
 */
public class SqrlClientRequestProcessingException extends SqrlException {
	private static final long serialVersionUID = -7986435707384269525L;
	private final int			tifToAdd;

	public SqrlClientRequestProcessingException(final String message) {
		// The caller does not wish to set an extra tif, so just re-use TIF_COMMAND_FAILED which is always added on a
		// failure
		this(SqrlTif.TIF_COMMAND_FAILED, message);
	}

	public SqrlClientRequestProcessingException(final int tifToAdd, final String message) {
		this(tifToAdd, message, null);
	}

	public SqrlClientRequestProcessingException(final int tifToAdd, final String message, final Throwable cause) {
		super(new StringBuilder(SqrlLoggingUtil.getLogHeader()).append(message).toString(), cause);
		this.tifToAdd = tifToAdd;
	}

	public int getTifToAdd() {
		return tifToAdd;
	}

}
