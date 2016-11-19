package com.github.dbadia.sqrl.server.exception;

import com.github.dbadia.sqrl.server.util.SqrlException;

/**
 * Indicates that the SQRL user exists, but is currently disabled per the SQRL protocol
 *
 * @author Dave Badia
 */
public class SqrlDisabledUserException extends SqrlException {
	private static final long serialVersionUID = -5708270310626694341L;

	public SqrlDisabledUserException(final String message) {
		super(message);
	}

}
