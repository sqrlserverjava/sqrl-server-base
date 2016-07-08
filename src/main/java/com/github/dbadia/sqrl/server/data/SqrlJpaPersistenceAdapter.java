package com.github.dbadia.sqrl.server.data;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;
import com.github.dbadia.sqrl.server.SqrlFlag;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlPersistenceException;

public class SqrlJpaPersistenceAdapter implements SqrlPersistence {
	private static final Logger logger = LoggerFactory.getLogger(SqrlJpaPersistenceAdapter.class);

	private static final EntityManager entityManager = Persistence
			.createEntityManagerFactory(Constants.PERSISTENCE_UNIT_NAME).createEntityManager();

	@Override
	public boolean doesSqrlIdentityExistByIdk(final String sqrlIdk) {
		return fetchSqrlIdentity(sqrlIdk) != null;
	}

	@Override
	public void updateIdkForSqrlIdentity(final String previousSqrlIdk, final String newSqrlIdk) {
		final SqrlIdentity sqrlIdentity = fetchRequiredSqrlIdentity(previousSqrlIdk);
		sqrlIdentity.setIdk(newSqrlIdk);
	}

	private SqrlIdentity fetchSqrlIdentity(final String sqrlIdk) {
		return (SqrlIdentity) returnOneOrNull(
				entityManager.createQuery("SELECT i FROM SqrlIdentity i WHERE i.idk = :sqrlIdk")
				.setParameter("sqrlIdk", sqrlIdk).getResultList());
	}

	private SqrlIdentity fetchRequiredSqrlIdentity(final String sqrlIdk) {
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		if (sqrlIdentity == null) {
			throw new EntityNotFoundException("SqrlIdentity does not exist for idk=" + sqrlIdk);
		} else {
			return sqrlIdentity;
		}
	}

	@Override
	public SqrlIdentity fetchSqrlIdentityByUserXref(final String userXref) {
		return (SqrlIdentity) entityManager
				.createQuery("SELECT i FROM SqrlIdentity i WHERE i.nativeUserXref = :userXref")
				.setParameter("userXref", userXref).getResultList();
	}

	@Override
	public void deleteSqrlIdentity(final String sqrlIdk) {
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		if (sqrlIdentity == null) {
			logger.warn("Can't find idk " + sqrlIdk + " to delete");
		} else {
			for (final SqrlIdentityData aData : sqrlIdentity.getUserDataList()) {
				entityManager.remove(aData);
			}
			for (final SqrlIdentityFlag flag : sqrlIdentity.getFlagList()) {
				entityManager.remove(flag);
			}
			// TODO: clean up
			// For some reason cascading delete in JPA isn't working, so delete the children here
			// entityManager.createQuery("DELETE FROM SqrlIdentityData x WHERE x.identity = :identity")
			// .setParameter("identity", sqrlIdentity).executeUpdate();
			entityManager.remove(sqrlIdentity);
		}
	}

	@Override
	public void userAuthenticatedViaSqrl(final String sqrlIdk, final String correlator) {
		// Find the sqrlIdentity and mark SQRL authentication as occurred
		final SqrlAuthenticationProgress sqrlAuthenticationProgress = fetchAuthenticationProgressRequired(correlator);
		final SqrlIdentity sqrlIdentity = fetchRequiredSqrlIdentity(sqrlIdk);
		if (sqrlAuthenticationProgress == null) {
			// progress object must have expired and was cleaned up
			throw new PersistenceException(
					"SqrlAuthenticationProgress not found for correlator=" + correlator + " and idk=" + sqrlIdk);
		}
		sqrlAuthenticationProgress.setAuthenticationComplete(sqrlIdentity);
	}

	@Override
	public SqrlAuthenticationStatus fetchAuthenticationStatusRequired(final String correlator) {
		return fetchAuthenticationProgressRequired(correlator).getAuthenticationStatus();
	}

	@Override
	public SqrlAuthenticationProgress fetchAuthenticationProgressRequired(final String correlator) {
		final SqrlAuthenticationProgress sqrlAuthenticationProgress = (SqrlAuthenticationProgress) returnOneOrNull(
				entityManager.createQuery("SELECT x FROM SqrlAuthenticationProgress x WHERE x.correlator = :correlator")
				.setParameter("correlator", correlator).getResultList());
		if (sqrlAuthenticationProgress == null) {
			throw new PersistenceException("SqrlAuthenticationProgress not found for correlator " + correlator);
		} else {
			if (sqrlAuthenticationProgress.getExpiryTime().after(new Date())) {
				throw new PersistenceException(
						"SqrlAuthenticationProgress expired at " + sqrlAuthenticationProgress.getExpiryTime());
			}
		}
		return sqrlAuthenticationProgress ;
	}

	@Override
	public void storeSqrlDataForSqrlIdentity(final String sqrlIdk, final Map<String, String> dataToStore) {
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		if (sqrlIdentity == null) {
			throw new PersistenceException("SqrlIdentity not found for " + sqrlIdk);
		}

	}

	private void storeSqrlDataForSqrlIdentity(final SqrlIdentity sqrlIdentity, final Map<String, String> dataToStore) {
		// Update any SQRL specific data we have received from the SQRL client
		if (!dataToStore.isEmpty()) {
			final Collection<SqrlIdentityData> sqrlUserDataList = sqrlIdentity.getUserDataList();
			if (sqrlUserDataList == null) {
				throw new EntityNotFoundException("sqrlUserDataList not found");
			}
			// userSpecificDataToStore could contain vuk, suk, or something else
			for (final Map.Entry<String, String> entry : dataToStore.entrySet()) {
				SqrlIdentityData dataBeingUpdated = null;
				// See if the entry.getKey() already exists in the DB
				for (final SqrlIdentityData data : sqrlUserDataList) {
					if (data.getName().equals(entry.getKey())) {
						dataBeingUpdated = data;
					}
				}
				// update existing or create a new one
				if (dataBeingUpdated != null) {
					dataBeingUpdated.setValue(entry.getValue());
				} else {
					dataBeingUpdated = new SqrlIdentityData(sqrlIdentity, entry.getKey(), entry.getValue());
					sqrlUserDataList.add(dataBeingUpdated);
				}
			}
		}
		entityManager.persist(sqrlIdentity);
	}

	@Override
	public String fetchSqrlIdentityDataItem(final String sqrlIdk, final String toFetch) {
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		if (sqrlIdentity == null) {
			throw new EntityNotFoundException("Couldn't find SqrlIdentity for idk " + sqrlIdk);
		} else {
			for (final SqrlIdentityData aData : sqrlIdentity.getUserDataList()) {
				if (toFetch.equals(aData.getName())) {
					return aData.getValue();
				}
			}
		}
		return null;
	}

	private SqrlUsedNutToken fetchSqrlUsedNutToken(final String nutTokenString) {
		return (SqrlUsedNutToken) returnOneOrNull(
				entityManager.createQuery("SELECT t FROM SqrlUsedNutToken t WHERE t.token = :token")
				.setParameter("token", nutTokenString).getResultList());
	}

	@Override
	public boolean hasTokenBeenUsed(final String nutTokenString) {
		return fetchSqrlUsedNutToken(nutTokenString) != null;
	}

	@Override
	public void markTokenAsUsed(final String nutTokenString, final Date expiryTime) {
		SqrlUsedNutToken usedToken = fetchSqrlUsedNutToken(nutTokenString);
		if (usedToken == null) {
			usedToken = new SqrlUsedNutToken(nutTokenString, new Date());
			entityManager.persist(usedToken);
		}
	}

	@Override
	public void storeTransientAuthenticationData(final String correlator, final String dataName, final String dataValue,
			final Date deleteAfter) {
		SqrlTransientAuthData transientAuthData = fetchTransientAuthDataRaw(correlator, dataName);
		if (transientAuthData == null) {
			transientAuthData = new SqrlTransientAuthData(correlator, dataName, dataValue, deleteAfter);
			entityManager.persist(transientAuthData);
		} else {
			transientAuthData.setValue(dataValue);
			transientAuthData.setDeleteAfter(deleteAfter);
		}
	}

	@Override
	public String fetchTransientAuthData(final String correlator, final String dataName) {
		final SqrlTransientAuthData transientAuthData = fetchTransientAuthDataRaw(correlator, dataName);
		if (transientAuthData == null) {
			return null;
		} else {
			return transientAuthData.getValue();
		}
	}

	private SqrlTransientAuthData fetchTransientAuthDataRaw(final String correlator, final String dataName) {
		return (SqrlTransientAuthData) returnOneOrNull(entityManager
				.createQuery(
						"SELECT t FROM SqrlTransientAuthData t WHERE t.correlator = :correlator AND t.name = :name")
				.setParameter("correlator", correlator).setParameter("name", dataName).getResultList());
	}

	private Object returnOneOrNull(@SuppressWarnings("rawtypes") final List resultList) {
		if (resultList == null || resultList.isEmpty()) {
			return null;
		} else if (resultList.size() == 1) {
			return resultList.get(0);
		} else {
			// TODO: extend and create our own? It's confusing to use the built in JPA exceptions
			throw new EntityExistsException("Expected one, but found multiple results: " + resultList);
		}
	}

	@Override
	public void startTransaction() {
		entityManager.getTransaction().begin();
	}

	@Override
	public void commitTransaction() {
		entityManager.getTransaction().commit();
	}

	@Override
	public void rollbackTransaction() {
		entityManager.getTransaction().rollback();
	}

	@Override
	public Boolean fetchSqrlFlagForIdentity(final String sqrlIdk, final SqrlFlag flagToFetch)
			throws SqrlPersistenceException {
		final SqrlIdentityFlag flagData = (SqrlIdentityFlag) returnOneOrNull(
				entityManager.createQuery("SELECT x FROM SqrlIdentityFlag x WHERE x.flagName = :flag")
				.setParameter("flag", flagToFetch).getResultList());
		if (flagData == null) {
			return null;
		} else {
			return flagData.getFlagValue();
		}
	}

	@Override
	public void setSqrlFlagForIdentity(final String sqrlIdk, final SqrlFlag flagToSet, final boolean valueToSet)
			throws SqrlPersistenceException {
		final SqrlIdentity sqrlIdentity = fetchRequiredSqrlIdentity(sqrlIdk);
		final SqrlIdentityFlag flagData = (SqrlIdentityFlag) returnOneOrNull(
				entityManager.createQuery("SELECT x FROM SqrlIdentityFlag x WHERE x.flagName = :flagToSet")
				.setParameter("flagToSet", flagToSet).getResultList());
		if (flagData == null) {
			final SqrlIdentityFlag newflagData = new SqrlIdentityFlag(sqrlIdentity, flagToSet, valueToSet);
			entityManager.persist(newflagData);
		} else {
			flagData.setValue(valueToSet);
		}
	}

	@Override
	public void createAndEnableSqrlIdentity(final String sqrlIdk, final Map<String, String> identityDataTable) {
		final SqrlIdentity sqrlIdentity = new SqrlIdentity(sqrlIdk);
		final SqrlIdentityFlag newflagData = new SqrlIdentityFlag(sqrlIdentity, SqrlFlag.SQRL_AUTH_ENABLED, true);
		sqrlIdentity.getFlagList().add(newflagData);
		entityManager.persist(sqrlIdentity);
		storeSqrlDataForSqrlIdentity(sqrlIdentity, identityDataTable);
		// Transaction hasnn't been commited yet, so use our existing sqrlIdentity
	}

	@Override
	public void createAuthenticationProgress(final String correlator, final Date expiryTime) {
		final SqrlAuthenticationProgress authenticationProgress = new SqrlAuthenticationProgress(correlator,
				expiryTime);
		entityManager.persist(authenticationProgress);
	}
}
