package com.github.sqrlserverjava.exception;

import com.github.sqrlserverjava.backchannel.SqrlTif;
import com.github.sqrlserverjava.backchannel.SqrlTifFlag;

/**
 * Indicates that a SQRL client send us an invalid request and will trigger the setting of
 * {@link SqrlTif#CLIENT_FAILURE} on the SQRL response
 *
 * @author Dave Badia
 *
 */
public class SqrlInvalidRequestException extends SqrlClientRequestProcessingException {
	private static final long serialVersionUID = -1136919442400493773L;

	public SqrlInvalidRequestException(final String message) {
		this(message, null);
	}

	public SqrlInvalidRequestException(final String message, final Throwable cause) {
		super(SqrlTifFlag.CLIENT_FAILURE, message, cause);
	}

}
