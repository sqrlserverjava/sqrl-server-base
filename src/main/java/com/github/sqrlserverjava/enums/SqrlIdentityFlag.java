package com.github.sqrlserverjava.enums;

/**
 * Flags that can be applied to a particular SQRL identity
 * 
 * @author Dave Badia
 *
 */
public enum SqrlIdentityFlag {
	// @formatter:off
	SQRL_AUTH_ENABLED(null), 

	/**
	 *  @see {@link SqrlRequestOpt#hardlock}
	 */
	HARDLOCK(SqrlRequestOpt.hardlock),

	/**
	 * @see {@link SqrlRequestOpt#sqrlonly}
	 */
	SQRLONLY(SqrlRequestOpt.sqrlonly),
	;
	// @formatter:on

	/**
	 * The SqrlClientOpt that corresponds to this flag, or null if there is no equivalent
	 */
	private final SqrlRequestOpt opt;

	private SqrlIdentityFlag(final SqrlRequestOpt opt) {
		this.opt = opt;
	}

	/**
	 * @returns true if this flag has an equivalent SqrlClientOpt
	 *
	 * @see {@link SqrlRequestOpt}
	 */

	public SqrlRequestOpt getSqrlClientOpt() {
		return this.opt;
	}

	/**
	 * @returns The SqrlClientOpt that corresponds to this flag, or null if there is no equivalent
	 */
	public boolean hasOptEquivalent() {
		return this.opt != null;
	}
}
