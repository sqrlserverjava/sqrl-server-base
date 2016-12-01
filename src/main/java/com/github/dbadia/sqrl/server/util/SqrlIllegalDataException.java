package com.github.dbadia.sqrl.server.util;

import com.github.dbadia.sqrl.server.exception.SqrlException;

public class SqrlIllegalDataException extends SqrlException {
	private static final long serialVersionUID = 5724065701447520962L;

	public SqrlIllegalDataException(final String message) {
		super(message);
	}

}
