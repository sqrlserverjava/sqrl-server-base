package com.github.sqrlserverjava.enums;

/**
 * Enum representing the different params a client can send to us The SQRL spec defines these in lowercase so we break
 * java convention and do the same for readability
 */
public enum SqrlClientParam {
	/*
	 * The spec defines these in lower case, so we do the same. It breaks java convention but is more readable 
	 */
	
	// @formatter:off
	/**
	 * cor is not in the SQRL spec and is unique to this library.  Custom params is permitted by the spec
	 */
	cor,
	ver,
	cmd,
	opt,
	;
}
