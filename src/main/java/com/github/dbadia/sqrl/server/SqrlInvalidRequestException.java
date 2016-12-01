package com.github.dbadia.sqrl.server;

import com.github.dbadia.sqrl.server.backchannel.SqrlTif;
import com.github.dbadia.sqrl.server.exception.SqrlClientRequestProcessingException;

/**
 * Indicates that a SQRL client send us an invalid request and will trigger the setting of
 * {@link SqrlTif#TIF_CLIENT_FAILURE} on the SQRL response
 *
 * @author Dave Badia
 *
 */
// TODO: move to exception package
public class SqrlInvalidRequestException extends SqrlClientRequestProcessingException {
	private static final long serialVersionUID = -1136919442400493773L;

	public SqrlInvalidRequestException(final String message) {
		super(SqrlTif.TIF_CLIENT_FAILURE, message);
	}

}
