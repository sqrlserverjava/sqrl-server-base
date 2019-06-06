package com.github.sqrlserverjava.exception;

import com.github.sqrlserverjava.backchannel.SqrlTifFlag;

/**
 * Indicates that an error occurred (nut token timeout, invalid signature, etc) while processing a request from a SQRL
 * client, will trigger the setting of the provided tif on the response
 *
 * @author Dave Badia
 */
public class SqrlClientRequestProcessingException extends SqrlException {
	private static final long serialVersionUID = -7986435707384269525L;
	private final SqrlTifFlag			tifFlagToAdd;

	public SqrlClientRequestProcessingException(final CharSequence... messagePartArray) {
		// The caller does not wish to set an extra tif, so just re-use COMMAND_FAILED which is always added on a
		// failure
		this(SqrlTifFlag.COMMAND_FAILED, null, messagePartArray);
	}

	/**
	 * 
	 * @param tifToAdd
	 */
	public SqrlClientRequestProcessingException(final SqrlTifFlag tifToAdd, final Throwable cause,
			final CharSequence... messagePartArray) {
		super(cause, messagePartArray);
		this.tifFlagToAdd = tifToAdd;
	}

	public SqrlTifFlag getTifToAdd() {
		return tifFlagToAdd;
	}

}
