package com.github.sqrlserverjava.exception;

import com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil;
import com.github.sqrlserverjava.backchannel.SqrlTifFlag;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * Indicates that an error occurred (nut token timeout, invalid signature, etc) while processing a request from a SQRL
 * client, will trigger the setting of the provided tif on the response
 *
 * @author Dave Badia
 */
public class SqrlClientRequestProcessingException extends SqrlException {
	private static final long serialVersionUID = -7986435707384269525L;
	private final SqrlTifFlag			tifFlagToAdd;

	public SqrlClientRequestProcessingException(final String message) {
		// The caller does not wish to set an extra tif, so just re-use COMMAND_FAILED which is always added on a
		// failure
		this(SqrlTifFlag.COMMAND_FAILED, message);
	}

	public SqrlClientRequestProcessingException(final SqrlTifFlag tifToAdd, final String message) {
		this(tifToAdd, message, null);
	}

	public SqrlClientRequestProcessingException(final SqrlTifFlag tifToAdd, final String message, final Throwable cause) {
		super(SqrlUtil.buildString(SqrlClientRequestLoggingUtil.getLogHeader(), message), cause);
		this.tifFlagToAdd = tifToAdd;
	}

	public SqrlTifFlag getTifToAdd() {
		return tifFlagToAdd;
	}

}
