package com.github.dbadia.sqrl.server;

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;

public interface ClientAuthStateUpdater {
	public void initSqrl(final SqrlConfig sqrlConfig, final SqrlServerOperations sqrlServerOperations);
	public void pushStatusUpdateToBrowser(final String browserSessionId, final SqrlAuthenticationStatus oldAuthStatus,
			final SqrlAuthenticationStatus newAuthStatus);
}
