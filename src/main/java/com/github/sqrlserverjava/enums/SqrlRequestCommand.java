package com.github.sqrlserverjava.enums;

public enum SqrlRequestCommand {
	// @formatter:off
	QUERY,
	IDENT,
	DISABLE,
	ENABLE,
	REMOVE,
	// @formatter:on
	;

	/**
	 * The SQRL spec states that opt args should only be processed during certain commands
	 *
	 * @return
	 */
	public boolean shouldProcessOpts() {
		// opt args should only be processed during non-query per the spec
		// we also add remove since it doesn't make sense to process data we are deleting
		return this != QUERY && this != REMOVE;
	}
}
