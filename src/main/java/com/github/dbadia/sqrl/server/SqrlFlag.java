package com.github.dbadia.sqrl.server;

import com.github.dbadia.sqrl.server.backchannel.SqrlClientOpt;

public enum SqrlFlag {
	SQRL_AUTH_ENABLED(null), HARDLOCK(SqrlClientOpt.hardlock);

	/**
	 * The SqrlClientOpt that corresponds to this flag, or null if there is no equivalent
	 */
	private final SqrlClientOpt opt;

	private SqrlFlag(final SqrlClientOpt opt) {
		this.opt = opt;
	}

	/**
	 * @returns true if this flag has an equivalent SqrlClientOpt
	 *
	 * @see {@link SqrlClientOpt}
	 */

	public SqrlClientOpt getSqrlClientOpt() {
		return this.opt;
	}

	/**
	 * @returns The SqrlClientOpt that corresponds to this flag, or null if there is no equivalent
	 */
	public boolean hasOptEquivalent() {
		return this.opt != null;
	}
}
