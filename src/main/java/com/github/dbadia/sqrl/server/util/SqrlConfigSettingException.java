package com.github.dbadia.sqrl.server.util;

public class SqrlConfigSettingException extends RuntimeException {
	private static final long serialVersionUID = -7855553556234268204L;

	public SqrlConfigSettingException(final String message) {
		super(message);
	}

	public SqrlConfigSettingException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
