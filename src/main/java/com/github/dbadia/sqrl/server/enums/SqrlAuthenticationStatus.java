package com.github.dbadia.sqrl.server.enums;

public enum SqrlAuthenticationStatus {
	//@formatter:off
	CORRELATOR_ISSUED,
	COMMUNICATING,
	AUTHENTICATED_BROWSER,
	AUTHENTICATED_CPS,
	ERROR_BAD_REQUEST,
	ERROR_SQRL_INTERNAL,
	SQRL_USER_DISABLED,
	;
	//@formatter:on

	/**
	 * @return true if there will be no more requests for the given correlator; typically indicating auth is complete or
	 *         has errored out
	 */
	public boolean isUpdatesForThisCorrelatorComplete() {
		return this.toString().startsWith("ERROR_") || this == SQRL_USER_DISABLED || isAuthComplete();
	}

	/**
	 * @return true if this status represents one of the authentication complete states
	 */
	public boolean isAuthComplete() {
		return this == AUTHENTICATED_BROWSER || this == AUTHENTICATED_CPS;
	}

	/**
	 * @return true if the enum is one of the happy path login flow states
	 */
	public boolean isHappyPath() {
		return !this.toString().startsWith("ERROR_"); // TODO: what is SQRL_USER_DISABLED
	}

}
