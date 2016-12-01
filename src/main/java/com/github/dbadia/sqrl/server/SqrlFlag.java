package com.github.dbadia.sqrl.server;

import com.github.dbadia.sqrl.server.backchannel.SqrlRequestOpt;

public enum SqrlFlag {
	SQRL_AUTH_ENABLED(null), HARDLOCK(SqrlRequestOpt.hardlock);

	/**
	 * The SqrlClientOpt that corresponds to this flag, or null if there is no equivalent
	 */
	private final SqrlRequestOpt opt;

	private SqrlFlag(final SqrlRequestOpt opt) {
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
