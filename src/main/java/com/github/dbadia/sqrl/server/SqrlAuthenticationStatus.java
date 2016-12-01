package com.github.dbadia.sqrl.server;

public enum SqrlAuthenticationStatus {
	CORRELATOR_ISSUED, COMMUNICATING, AUTH_COMPLETE, ERROR_BAD_REQUEST, ERROR_SQRL_INTERNAL, SQRL_USER_DISABLED;

	public boolean isUpdatesForThisCorrelatorComplete() {
		return this.toString().startsWith("ERROR_") || this == SQRL_USER_DISABLED || this == AUTH_COMPLETE;
	}

}
