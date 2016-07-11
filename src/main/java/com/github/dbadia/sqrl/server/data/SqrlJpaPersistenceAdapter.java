package com.github.dbadia.sqrl.server.data;

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

	/**
	 * Test case use only
	 */
	protected static EntityManager getEntityManager() {
		return entityManager;
	}

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
			entityManager.remove(sqrlIdentity);
		}
	}

	@Override
	public void userAuthenticatedViaSqrl(final String sqrlIdk, final String correlatorString) {
		// Find the sqrlIdentity and mark SQRL authentication as occurred
		final SqrlCorrelator sqrlCorrelator = fetchSqrlCorrelatorRequired(correlatorString);
		sqrlCorrelator.setAuthenticationStatus(SqrlAuthenticationStatus.AUTH_COMPLETE);
		final SqrlIdentity sqrlIdentity = fetchRequiredSqrlIdentity(sqrlIdk);
		sqrlCorrelator.setAuthenticatedIdentity(sqrlIdentity);
	}

	private SqrlCorrelator fetchSqrlCorrelator(final String sqrlCorrelatorString) {
		return (SqrlCorrelator) returnOneOrNull(
				entityManager.createQuery("SELECT i FROM SqrlCorrelator i WHERE i.value = :correlator")
				.setParameter("correlator", sqrlCorrelatorString).getResultList());
	}

	@Override
	public SqrlCorrelator fetchSqrlCorrelatorRequired(final String sqrlCorrelatorString) {
		final SqrlCorrelator sqrlCorrelator = fetchSqrlCorrelator(sqrlCorrelatorString);
		if (sqrlCorrelator == null) {
			throw new EntityNotFoundException("SqrlCorrelator does not exist for correlator=" + sqrlCorrelatorString);
		} else {
			return sqrlCorrelator;
		}
	}

	@Override
	public SqrlAuthenticationStatus fetchAuthenticationStatusRequired(final String correlatorString) {
		return fetchSqrlCorrelatorRequired(correlatorString).getAuthenticationStatus();
	}

	@Override
	public void storeSqrlDataForSqrlIdentity(final String sqrlIdk, final Map<String, String> dataToStore) {
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		if (sqrlIdentity == null) {
			throw new PersistenceException("SqrlIdentity not found for " + sqrlIdk);
		}
		storeSqrlDataForSqrlIdentity(sqrlIdentity, dataToStore);
	}

	private void storeSqrlDataForSqrlIdentity(final SqrlIdentity sqrlIdentity, final Map<String, String> dataToStore) {
		// Update any SQRL specific data we have received from the SQRL client
		if (!dataToStore.isEmpty()) {
			sqrlIdentity.getIdentityDataTable().putAll(dataToStore);
		}
		entityManager.persist(sqrlIdentity);
	}

	@Override
	public String fetchSqrlIdentityDataItem(final String sqrlIdk, final String toFetch) {
		final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
		if (sqrlIdentity == null) {
			throw new EntityNotFoundException("Couldn't find SqrlIdentity for idk " + sqrlIdk);
		} else {
			return sqrlIdentity.getIdentityDataTable().get(toFetch);
		}
	}

	@Override
	public boolean hasTokenBeenUsed(final String nutTokenString) {
		final SqrlCorrelator sqrlCorrelator = (SqrlCorrelator) returnOneOrNull(
				entityManager
				.createQuery(
						"SELECT i FROM SqrlCorrelator i WHERE :nutTokenString MEMBER OF  i.usedNutTokenList")
				.setParameter("nutTokenString", nutTokenString).getResultList());
		return sqrlCorrelator != null;
	}

	@Override
	public void markTokenAsUsed(final String correlatorString, final String nutTokenString, final Date expiryTime) {
		final SqrlCorrelator sqrlCorrelator = fetchSqrlCorrelator(correlatorString);
		sqrlCorrelator.getUsedNutTokenList().add(nutTokenString);
		if (sqrlCorrelator.getExpiryTime() == null || sqrlCorrelator.getExpiryTime().before(expiryTime)) {
			sqrlCorrelator.setExpiryTime(expiryTime);
		}
	}

	@Override
	public String fetchTransientAuthData(final String correlator, final String dataName) {
		final SqrlCorrelator correlatorObject = fetchSqrlCorrelatorRequired(correlator);
		return correlatorObject.getTransientAuthDataTable().get(dataName);
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
		return fetchRequiredSqrlIdentity(sqrlIdk).getFlagTable().get(flagToFetch);
	}

	@Override
	public void setSqrlFlagForIdentity(final String sqrlIdk, final SqrlFlag flagToSet, final boolean valueToSet)
			throws SqrlPersistenceException {
		final SqrlIdentity sqrlIdentity = fetchRequiredSqrlIdentity(sqrlIdk);
		sqrlIdentity.getFlagTable().put(flagToSet, valueToSet);
		entityManager.persist(sqrlIdentity);
	}

	@Override
	public void createAndEnableSqrlIdentity(final String sqrlIdk, final Map<String, String> identityDataTable) {
		final SqrlIdentity sqrlIdentity = new SqrlIdentity(sqrlIdk);
		sqrlIdentity.getFlagTable().put(SqrlFlag.SQRL_AUTH_ENABLED, true);
		sqrlIdentity.getIdentityDataTable().putAll(identityDataTable);
		entityManager.persist(sqrlIdentity);
	}

	@Override
	public SqrlCorrelator createCorrelator(final String correlatorString, final Date expiryTime) {
		final SqrlCorrelator sqrlCorrelator = new SqrlCorrelator(correlatorString, expiryTime);
		entityManager.persist(sqrlCorrelator);
		return sqrlCorrelator;
	}
}
