package com.github.dbadia.sqrl.server.exception;

import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.util.SqrlException;

/**
 * Indicates that the one time use Nut token received from the SQRL client was already used in a previous request as
 * determined by {@link SqrlPersistence#hasTokenBeenUsed(com.github.dbadia.sqrl.server.backchannel.Nut)}
 * 
 * @author Dave Badia
 *
 */
public class SqrlNutTokenReplayedException extends SqrlException {

	private static final long serialVersionUID = 3262027974608634373L;

	/**
	 * {@inheritDoc}
	 */
	public SqrlNutTokenReplayedException(final String message) {
		super(message);
	}

}
