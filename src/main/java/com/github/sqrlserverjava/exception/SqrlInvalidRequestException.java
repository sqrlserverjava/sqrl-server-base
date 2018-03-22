package com.github.sqrlserverjava.exception;

import com.github.sqrlserverjava.backchannel.SqrlTifFlag;
import com.github.sqrlserverjava.backchannel.SqrlTifResponse;

/**
 * Indicates that a SQRL client send us an invalid request and will trigger the setting of
 * {@link SqrlTifResponse#CLIENT_FAILURE} on the SQRL response
 *
 * @author Dave Badia
 *
 */
public class SqrlInvalidRequestException extends SqrlClientRequestProcessingException {
	private static final long serialVersionUID = -1136919442400493773L;

	public SqrlInvalidRequestException(final Object... messagePartArray) {
		this(null, messagePartArray);
	}

	public SqrlInvalidRequestException(final Throwable cause, Object... messagePartArray) {
		super(SqrlTifFlag.CLIENT_FAILURE, cause, messagePartArray);
	}

}
