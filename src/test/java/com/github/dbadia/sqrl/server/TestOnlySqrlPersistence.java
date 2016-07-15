package com.github.dbadia.sqrl.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.PersistenceException;

import com.github.dbadia.sqrl.server.data.SqrlIdentity;

/**
 * A non-JPA memory only {@link SqrlPersistence} implementation that is only suitable for test case use
 * 
 */
public class TestOnlySqrlPersistence { // implements SqrlPersistence { TODO
	private final Map<String, SqrlIdentity>				sqrlIdentityTable			= new ConcurrentHashMap<>();
	private final List<String>							usedTokens					= new ArrayList<>();
	private final Map<String, Map<String, String>>		sqrlTransientAuthDataTable	= new ConcurrentHashMap<>();
	private final Map<String, Map<SqrlFlag, Boolean>>	sqrlIdentityFlagTable		= new ConcurrentHashMap<>();

	public TestOnlySqrlPersistence() {
	}

	/* ***************** SQRL IDENTITY *********************/

	public boolean doesSqrlIdentityExistByIdk(final String sqrlIdk) {
		return fetchSqrlIdentity(sqrlIdk) != null;
	}

	private SqrlIdentity fetchSqrlIdentity(final String sqrlIdk) {
		return sqrlIdentityTable.get(sqrlIdk);
	}

	public void createAndEnableSqrlIdentity(final String sqrlIdk, final Map<String, String> identityDataTable) {
		SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		if (sqrlIdentity != null) {
			throw new PersistenceException("SQRL identity already exists for " + sqrlIdk);
		}
		sqrlIdentity = new SqrlIdentity(sqrlIdk);
		sqrlIdentityTable.put(sqrlIdk, sqrlIdentity);
		setSqrlFlagForIdentity(sqrlIdk, SqrlFlag.SQRL_AUTH_ENABLED, true);
	}

	public SqrlIdentity fetchSqrlIdentityByUserXref(final String appUserXref) {
		for (final SqrlIdentity sqrlIdentity : sqrlIdentityTable.values()) {
			if (appUserXref.equals(sqrlIdentity.getNativeUserXref())) {
				return sqrlIdentity;
			}
		}
		return null;
	}

	public boolean hasTokenBeenUsed(final String nutTokenString) {
		return usedTokens.contains(nutTokenString);
	}

	public void markTokenAsUsed(final String nutTokenString, final Date expiryTime) {
		// No need to track expiryTime.getTime() in the test cases
		usedTokens.add(nutTokenString);
	}

	public void updateIdkForSqrlIdentity(final String previousIdk, final String newIdk) {
		final SqrlIdentity sqrlIdentity = sqrlIdentityTable.remove(previousIdk);
		sqrlIdentityTable.put(newIdk, sqrlIdentity);
	}

	public void storeTransientAuthenticationData(final String correlator, final String name, final String value,
			final Date deleteAfter) {
		Map<String, String> transientAuthDataForCorrelator = sqrlTransientAuthDataTable.get(correlator);
		if (transientAuthDataForCorrelator == null) {
			transientAuthDataForCorrelator = new ConcurrentHashMap<>();
			sqrlTransientAuthDataTable.put(correlator, transientAuthDataForCorrelator);
		}
		transientAuthDataForCorrelator.put(name, value);
	}

	public String fetchTransientAuthData(final String correlator, final String name) {
		final Map<String, String> table = sqrlTransientAuthDataTable.get(correlator);
		if (table != null) {
			final String value = table.get(name);
			if (value != null && value.trim().length() > 0) {
				return value;
			}
		}
		return null;
	}

	public void deleteSqrlIdentity(final String sqrlIdk) {
		sqrlIdentityTable.remove(sqrlIdk);
	}

	public void startTransaction() {
		// do nothing
	}

	public void commitTransaction() {
		// do nothing
	}

	public void rollbackTransaction() {
		// do nothing
	}

	public Boolean fetchSqrlFlagForIdentity(final String sqrlIdk, final SqrlFlag flagToFetch) {
		final Map<SqrlFlag, Boolean> flagTable = sqrlIdentityFlagTable.get(sqrlIdk);
		if (flagTable == null) {
			return null;
		}
		return flagTable.get(flagToFetch);
	}

	public void setSqrlFlagForIdentity(final String sqrlIdk, final SqrlFlag flagToSet, final boolean valueToSet) {
		Map<SqrlFlag, Boolean> flagTable = sqrlIdentityFlagTable.get(sqrlIdk);
		if (flagTable == null) {
			flagTable = new ConcurrentHashMap<>();
			sqrlIdentityFlagTable.put(sqrlIdk, flagTable);
		}
		flagTable.put(flagToSet, valueToSet);
	}

}
