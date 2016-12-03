package com.github.dbadia.sqrl.server.enums;

/**
 * Enum representing the different params a client can send to us The SQRL spec defines these in lowercase so we break
 * java convention and do the same for readability
 */
public enum SqrlClientParam {
	// @formatter:off
	/**
	 * is not in the SQRL spec, but this parameter is added by our library as allowed by the spec
	 */
	cor,
	ver,
	cmd,
	opt,
	;
}
