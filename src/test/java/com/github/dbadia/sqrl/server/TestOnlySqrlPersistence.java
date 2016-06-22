package com.github.dbadia.sqrl.server;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.dbadia.sqrl.server.backchannel.SqrlAuthState;

/**
 * An in memory {@link SqrlPersistence} implementation that is only suitable for test case use
 * 
 */
public class TestOnlySqrlPersistence implements SqrlPersistence {

	private final Map<String, SqrlAuthState> knownSqrlUserToStateTable = new ConcurrentHashMap<>();
	private final Map<String, String> authenticatedUsers = new ConcurrentHashMap<>();
	private final Map<String, Long> usedTokens = new ConcurrentHashMap<>();

	private final Map<String, Map<String, String>> sqrlIdentityDataTable = new ConcurrentHashMap<>();
	private final Map<String, Map<String, String>> sqrlTransientAuthDataTable = new ConcurrentHashMap<>();

	public TestOnlySqrlPersistence() {
	}

	@Override
	public boolean doesSqrlIdentityExistByIdk(final String sqrlIdk) throws SqrlPersistenceException {
		return knownSqrlUserToStateTable.containsKey(sqrlIdk);
	}

	@Override
	public void storeSqrlDataForSqrlIdentity(final String sqrlIdk, final Map<String, String> dataToStore)
			throws SqrlPersistenceException {
		knownSqrlUserToStateTable.put(sqrlIdk, SqrlAuthState.ENABLE);
		Map<String, String> sqrlDataForIdentity = sqrlIdentityDataTable.get(sqrlIdk);
		if (sqrlDataForIdentity == null) {
			sqrlDataForIdentity = new ConcurrentHashMap<>();
			sqrlIdentityDataTable.put(sqrlIdk, sqrlDataForIdentity);
		}
		sqrlDataForIdentity.putAll(dataToStore);
	}

	@Override
	public void userAuthenticatedViaSqrl(final String sqrlIdk, final String correlator)
			throws SqrlPersistenceException {
		// Normally we would associate some sort of timestamp with when this occurred (otherwise the user will appear to
		// be authenticated forever) but since this is short lived for test cases we don't need to worry about it
		authenticatedUsers.put(correlator, sqrlIdk);
	}

	@Override
	public boolean hasTokenBeenUsed(final String nutTokenString) throws SqrlPersistenceException {
		return usedTokens.containsKey(nutTokenString);
	}

	@Override
	public void markTokenAsUsed(final String nutTokenString, final Date expiryTime) throws SqrlPersistenceException {
		usedTokens.put(nutTokenString, expiryTime.getTime());
	}

	@Override
	public void updateIdkForSqrlIdentity(final String previousIdk, final String newIdk) {
		knownSqrlUserToStateTable.remove(previousIdk);
		knownSqrlUserToStateTable.put(newIdk, SqrlAuthState.ENABLE);
	}

	public String isUserAuthenticated(final String correlator) {
		return authenticatedUsers.get(correlator);
	}

	@Override
	public String fetchSqrlIdentityDataItem(final String sqrlIdk, final String toFetch) throws SqrlPersistenceException {
		final Map<String, String> extraUserDataTable = sqrlIdentityDataTable.get(sqrlIdk);
		if (extraUserDataTable == null || extraUserDataTable.isEmpty()) {
			return null;
		}
		return extraUserDataTable.get(toFetch);
	}

	@Override
	public void storeTransientAuthenticationData(final String correlator, final String name, final String value,
			final LocalDateTime deleteAfter) {
		Map<String, String> transientAuthDataForCorrelator = sqrlTransientAuthDataTable.get(correlator);
		if (transientAuthDataForCorrelator == null) {
			transientAuthDataForCorrelator = new ConcurrentHashMap<>();
			sqrlTransientAuthDataTable.put(correlator, transientAuthDataForCorrelator);
		}
		transientAuthDataForCorrelator.put(name, value);
	}

	@Override
	public String fetchTransientAuthData(final String correlator, final String name)
			throws SqrlPersistenceException, SqrlException {
		final Map<String, String> table = sqrlTransientAuthDataTable.get(correlator);
		if (table != null) {
			final String value = table.get(name);
			if (value != null && value.trim().length() > 0) {
				return value;
			}
		}
		throw new SqrlException("Transient auth data not found for " + name + " with correlator " + correlator);
	}

	@Override
	public void setSqrlAuthState(final String sqrlIdk, final SqrlAuthState state) {
		knownSqrlUserToStateTable.put(sqrlIdk, state);
	}

	@Override
	public SqrlAuthState getSqrlAuthState(final String sqrlIdk) {
		return knownSqrlUserToStateTable.get(sqrlIdk);
	}
}
