package com.github.dbadia.sqrl.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in memory {@link SqrlPersistence} implementation that is only suitable for test case use
 * 
 */
public class TestOnlySqrlPersistence implements SqrlPersistence {

	private final List<String> knownUsers = new ArrayList<>();
	private final Map<String, String> authenticatedUsers = new ConcurrentHashMap<>();
	private final Map<String, Long> usedTokens = new ConcurrentHashMap<>();

	private final Map<String, Map<String, String>> sqrlIdentityDataTable = new ConcurrentHashMap<>();

	public TestOnlySqrlPersistence() {
	}

	@Override
	public boolean doesSqrlIdentityExistByIdk(final String sqrlIdk) throws SqrlPersistenceException {
		return knownUsers.contains(sqrlIdk);
	}

	@Override
	public void storeSqrlDataForSqrlIdentity(final String sqrlIdk, final Map<String, String> dataToStore)
			throws SqrlPersistenceException {
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
		knownUsers.remove(previousIdk);
		knownUsers.add(newIdk);
	}

	public String isUserAuthenticated(final String correlator) {
		return authenticatedUsers.get(correlator);
	}

	@Override
	public String fetchSqrlIdentityDataItem(final String sqrlIdk, final String toFetch) throws SqrlPersistenceException {
		final Map<String, String> extraUserDataTable = sqrlIdentityDataTable.get(sqrlIdk);
		if (extraUserDataTable == null || extraUserDataTable.isEmpty()) {
			throw new SqrlPersistenceException("No data for this user");
		}
		return extraUserDataTable.get(toFetch);
	}
}
