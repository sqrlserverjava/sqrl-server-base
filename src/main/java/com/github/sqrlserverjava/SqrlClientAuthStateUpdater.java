package com.github.sqrlserverjava;

import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;

/**
 * For use when SQRL auto refresh is desired so that a user is automatically logged in upon a successful SQRL
 * authentication. There is a pre-built implementation of this class which uses the atmosphere framework. See the SQRL
 * atmosphere project at https://github.com/sqrlserverjava/sqrl-server-atmosphere
 *
 * @author Dave Badia
 *
 */
public interface SqrlClientAuthStateUpdater {
	/**
	 * Invoked during initialization so that the {@link SqrlClientAuthStateUpdater} has access to the resources it may
	 * need
	 */
	public void initSqrl(SqrlServerOperations sqrlServerOperations, final SqrlConfig sqrlConfig,
			SqrlAuthStateMonitor sqrlAuthStateMonitor);

	/**
	 * Invoked by {@link SqrlAuthStateMonitor} when it is time to respond to a browsers polling request with an update
	 */
	public void pushStatusUpdateToBrowser(final String browserId, final SqrlAuthenticationStatus oldAuthStatus,
			final SqrlAuthenticationStatus newAuthStatus);
}
