package com.github.dbadia.sqrl.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.PersistenceException;

import com.github.dbadia.sqrl.server.data.SqrlAuthenticationProgress;
import com.github.dbadia.sqrl.server.data.SqrlIdentity;
import com.github.dbadia.sqrl.server.data.SqrlIdentityData;

/**
 * An in memory {@link SqrlPersistence} implementation that is only suitable for test case use
 * 
 */
public class TestOnlySqrlPersistence2 { // implements SqrlPersistence { TODO
	private final Map<String, SqrlIdentity> sqrlIdentityTable = new ConcurrentHashMap<>();
	private final Map<String, SqrlAuthenticationProgress> authenticatedProgressTable = new ConcurrentHashMap<>();
	private final List<String> usedTokens = new ArrayList<>();
	private final Map<String, Map<String, String>> sqrlTransientAuthDataTable = new ConcurrentHashMap<>();
	private final Map<String, Map<SqrlFlag, Boolean>> sqrlIdentityFlagTable = new ConcurrentHashMap<>();

	public TestOnlySqrlPersistence2() {
	}

	/* ***************** SQRL IDENTITY *********************/


	public boolean doesSqrlIdentityExistByIdk(final String sqrlIdk) throws SqrlPersistenceException {
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


	public void storeSqrlDataForSqrlIdentity(final String sqrlIdk, final Map<String, String> dataToStore)
			throws SqrlPersistenceException {
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		if (sqrlIdentity == null) {
			throw new PersistenceException("SQRL identity not found for " + sqrlIdk);
		}
		// TODO: rename to IdentityDataList
		Collection<SqrlIdentityData> sqrlDataForIdentity = sqrlIdentity.getUserDataList();
		if (sqrlDataForIdentity == null) {
			sqrlDataForIdentity = new ArrayList<>();
			sqrlIdentity.setUserDataList(sqrlDataForIdentity);
		}
		for (final Map.Entry<String, String> entry : dataToStore.entrySet()) {
			final SqrlIdentityData aData = new SqrlIdentityData(sqrlIdentity, entry.getKey(), entry.getValue());
			sqrlDataForIdentity.add(aData);
		}
	}


	public void userAuthenticatedViaSqrl(final String sqrlIdk, final String correlator)
			throws SqrlPersistenceException {
		// Normally we would associate some sort of timestamp with when this occurred (otherwise the user will appear to
		// be authenticated forever) but since this is short lived for test cases we don't need to worry about it
		final SqrlAuthenticationProgress authProgress = new SqrlAuthenticationProgress();
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		authProgress.setAuthenticationComplete(sqrlIdentity);
		authenticatedProgressTable.put(correlator, authProgress);
	}


	public SqrlIdentity fetchSqrlIdentityByUserXref(final String appUserXref) {
		for (final SqrlIdentity sqrlIdentity : sqrlIdentityTable.values()) {
			if (appUserXref.equals(sqrlIdentity.getNativeUserXref())) {
				return sqrlIdentity;
			}
		}
		return null;
	}


	public boolean hasTokenBeenUsed(final String nutTokenString) throws SqrlPersistenceException {
		return usedTokens.contains(nutTokenString);
	}


	public void markTokenAsUsed(final String nutTokenString, final Date expiryTime) throws SqrlPersistenceException {
		// No need to track expiryTime.getTime() in the test cases
		usedTokens.add(nutTokenString);
	}


	public void updateIdkForSqrlIdentity(final String previousIdk, final String newIdk) {
		final SqrlIdentity sqrlIdentity = sqrlIdentityTable.remove(previousIdk);
		sqrlIdentityTable.put(newIdk, sqrlIdentity);
	}


	public String fetchSqrlIdentityDataItem(final String sqrlIdk, final String toFetch) throws SqrlPersistenceException {
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		for (final SqrlIdentityData aData : sqrlIdentity.getUserDataList()) {
			if (toFetch.equals(aData.getName())) {
				return aData.getValue();
			}
		}
		return null;
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


	public String fetchTransientAuthData(final String correlator, final String name)
			throws SqrlPersistenceException {
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


	public Boolean fetchSqrlFlagForIdentity(final String sqrlIdk, final SqrlFlag flagToFetch)
			throws SqrlPersistenceException {
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


	public SqrlAuthenticationStatus fetchAuthenticationStatusRequired(final String correlator) {
		final SqrlAuthenticationProgress progress = authenticatedProgressTable.get(correlator);
		SqrlAuthenticationStatus status = progress.getAuthenticationStatus();
		if (status == null) {
			status = SqrlAuthenticationStatus.CORRELATOR_ISSUED;
		}
		return status;
	}


	public SqrlAuthenticationProgress fetchAuthenticationProgressRequired(final String correlator) {
		// TODO Auto-generated method stub
		return null;
	}
}
