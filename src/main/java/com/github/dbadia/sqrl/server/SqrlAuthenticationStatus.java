package com.github.dbadia.sqrl.server;

public enum SqrlAuthenticationStatus {
    CORRELATOR_ISSUED, COMMUNICATING, AUTH_COMPLETE, ERROR_BAD_REQUEST, ERROR_INTERNAL;

    public boolean isErrorStatus() {
	return this.toString().startsWith("ERROR_");
    }

}
