package com.github.dbadia.sqrl.server;

public enum SqrlAuthenticationStatus {
	//@formatter:off
	CORRELATOR_ISSUED,
	COMMUNICATING,
	AUTH_COMPLETE,
	ERROR_BAD_REQUEST,
	ERROR_SQRL_INTERNAL,
	SQRL_USER_DISABLED;
	//@formatter:on

	/**
	 * @return true if there will be no more requests for the given correlator; typically indicating auth is complete or
	 *         has errored out
	 */
	public boolean isUpdatesForThisCorrelatorComplete() {
		return this.toString().startsWith("ERROR_") || this == SQRL_USER_DISABLED || this == AUTH_COMPLETE;
	}

	/**
	 * @return true if the enum is one of the happy path login flow states
	 */
	public boolean isHappyPath() {
		return this == CORRELATOR_ISSUED || this == SqrlAuthenticationStatus.COMMUNICATING || this == AUTH_COMPLETE;
	}

}
