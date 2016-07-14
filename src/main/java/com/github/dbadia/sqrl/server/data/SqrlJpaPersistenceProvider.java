package com.github.dbadia.sqrl.server.data;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TemporalType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;
import com.github.dbadia.sqrl.server.SqrlFlag;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;

/**
 * The default implementation of {@link SqrlPersistence} which uses JPA in order to provide SQL and no-SQL connectivity.
 * <p>
 * Web apps should not use this class directly, instead {@link SqrlServerOperations} should be used
 * 
 * @author Dave Badia
 *
 */
public class SqrlJpaPersistenceProvider implements SqrlPersistence {
    public static final String PERSISTENCE_UNIT_NAME = "javasqrl-persistence";
    private static final Logger logger = LoggerFactory.getLogger(SqrlJpaPersistenceProvider.class);

    private static EntityManagerFactory entityManagerFactory = Persistence
	    .createEntityManagerFactory(SqrlJpaPersistenceProvider.PERSISTENCE_UNIT_NAME);
    private static final Map<EntityManager, Long> LAST_USED_TIME_TABLE = new WeakHashMap<>();
    // Need strong references so we can check that it was closed, will be removed below
    private static final Map<EntityManager, Exception> CREATED_BY_STACK_TABLE = new ConcurrentHashMap<>();

    private final EntityManager entityManager;

    static {
	final Timer timer = new Timer(true);
	final long interval = TimeUnit.MINUTES.toMillis(15);
	timer.schedule(new EntityManagerMonitorTimerTask(), interval, interval);
    }

    /**
     * @deprecated do not invoke this constructor directly
     */
    @Deprecated
    public SqrlJpaPersistenceProvider() {
	entityManager = entityManagerFactory.createEntityManager();
	entityManager.getTransaction().begin();
	LAST_USED_TIME_TABLE.put(entityManager, System.currentTimeMillis());
	CREATED_BY_STACK_TABLE.put(entityManager, new Exception("create SqrlJpaPersistenceProvider trace"));
    }

    private void updateLastUsed(final EntityManager entityManger) {
	LAST_USED_TIME_TABLE.put(entityManger, System.currentTimeMillis());
    }

    @Override
    public boolean doesSqrlIdentityExistByIdk(final String sqrlIdk) {
	updateLastUsed(entityManager);
	return fetchSqrlIdentity(sqrlIdk) != null;
    }

    @Override
    public void updateIdkForSqrlIdentity(final String previousSqrlIdk, final String newSqrlIdk) {
	updateLastUsed(entityManager);
	final SqrlIdentity sqrlIdentity = fetchRequiredSqrlIdentity(previousSqrlIdk);
	sqrlIdentity.setIdk(newSqrlIdk);
    }

    private SqrlIdentity fetchSqrlIdentity(final String sqrlIdk) {
	updateLastUsed(entityManager);
	return (SqrlIdentity) returnOneOrNull(
		entityManager.createQuery("SELECT i FROM SqrlIdentity i WHERE i.idk = :sqrlIdk")
			.setParameter("sqrlIdk", sqrlIdk).getResultList());
    }

    private SqrlIdentity fetchRequiredSqrlIdentity(final String sqrlIdk) {
	updateLastUsed(entityManager);
	final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
	if (sqrlIdentity == null) {
	    throw new SqrlPersistenceException("SqrlIdentity does not exist for idk=" + sqrlIdk);
	} else {
	    return sqrlIdentity;
	}
    }

    @Override
    public SqrlIdentity fetchSqrlIdentityByUserXref(final String userXref) {
	updateLastUsed(entityManager);
	return (SqrlIdentity) entityManager
		.createQuery("SELECT i FROM SqrlIdentity i WHERE i.nativeUserXref = :userXref")
		.setParameter("userXref", userXref).getResultList();
    }

    @Override
    public void deleteSqrlIdentity(final String sqrlIdk) {
	updateLastUsed(entityManager);
	final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
	if (sqrlIdentity == null) {
	    logger.warn("Can't find idk " + sqrlIdk + " to delete");
	} else {
	    entityManager.remove(sqrlIdentity);
	}
    }

    @Override
    public void userAuthenticatedViaSqrl(final String sqrlIdk, final String correlatorString) {
	updateLastUsed(entityManager);
	// Find the sqrlIdentity and mark SQRL authentication as occurred
	final SqrlCorrelator sqrlCorrelator = fetchSqrlCorrelatorRequired(correlatorString);
	sqrlCorrelator.setAuthenticationStatus(SqrlAuthenticationStatus.AUTH_COMPLETE);
	final SqrlIdentity sqrlIdentity = fetchRequiredSqrlIdentity(sqrlIdk);
	sqrlCorrelator.setAuthenticatedIdentity(sqrlIdentity);
    }

    @Override
    public void updateNativeUserXref(final long sqrlIdentityDbId, final String nativeUserXref) {
	updateLastUsed(entityManager);
	final SqrlIdentity sqrlIdentity = entityManager.find(SqrlIdentity.class, sqrlIdentityDbId);
	sqrlIdentity.setNativeUserXref(nativeUserXref);
    }

    /* ************************ Sqrl Correlator methods *****************************/

    private SqrlCorrelator fetchSqrlCorrelator(final String sqrlCorrelatorString) {
	updateLastUsed(entityManager);
	return (SqrlCorrelator) returnOneOrNull(
		entityManager.createQuery("SELECT i FROM SqrlCorrelator i WHERE i.value = :correlator")
			.setParameter("correlator", sqrlCorrelatorString).getResultList());
    }

    @Override
    public SqrlCorrelator fetchSqrlCorrelatorRequired(final String sqrlCorrelatorString) {
	updateLastUsed(entityManager);
	final SqrlCorrelator sqrlCorrelator = fetchSqrlCorrelator(sqrlCorrelatorString);
	if (sqrlCorrelator == null) {
	    throw new SqrlPersistenceException("SqrlCorrelator does not exist for correlator=" + sqrlCorrelatorString);
	} else {
	    return sqrlCorrelator;
	}
    }

    @Override
    public void storeSqrlDataForSqrlIdentity(final String sqrlIdk, final Map<String, String> dataToStore) {
	updateLastUsed(entityManager);
	final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
	if (sqrlIdentity == null) {
	    throw new SqrlPersistenceException("SqrlIdentity not found for " + sqrlIdk);
	}
	storeSqrlDataForSqrlIdentity(sqrlIdentity, dataToStore);
    }

    private void storeSqrlDataForSqrlIdentity(final SqrlIdentity sqrlIdentity, final Map<String, String> dataToStore) {
	updateLastUsed(entityManager);
	// Update any SQRL specific data we have received from the SQRL client
	if (!dataToStore.isEmpty()) {
	    sqrlIdentity.getIdentityDataTable().putAll(dataToStore);
	}
	entityManager.persist(sqrlIdentity);
    }

    @Override
    public String fetchSqrlIdentityDataItem(final String sqrlIdk, final String toFetch) {
	updateLastUsed(entityManager);
	final SqrlIdentity sqrlIdentity = fetchSqrlIdentity(sqrlIdk);
	if (sqrlIdentity == null) {
	    throw new SqrlPersistenceException("Couldn't find SqrlIdentity for idk " + sqrlIdk);
	} else {
	    return sqrlIdentity.getIdentityDataTable().get(toFetch);
	}
    }

    @Override
    public boolean hasTokenBeenUsed(final String nutTokenString) {
	updateLastUsed(entityManager);
	final SqrlCorrelator sqrlCorrelator = (SqrlCorrelator) returnOneOrNull(entityManager
		.createQuery("SELECT i FROM SqrlCorrelator i WHERE :nutTokenString MEMBER OF  i.usedNutTokenList")
		.setParameter("nutTokenString", nutTokenString).getResultList());
	return sqrlCorrelator != null;
    }

    @Override
    public void markTokenAsUsed(final String correlatorString, final String nutTokenString, final Date expiryTime) {
	updateLastUsed(entityManager);
	final SqrlCorrelator sqrlCorrelator = fetchSqrlCorrelatorRequired(correlatorString);
	sqrlCorrelator.getUsedNutTokenList().add(nutTokenString);
	if (sqrlCorrelator.getExpiryTime() == null || sqrlCorrelator.getExpiryTime().before(expiryTime)) {
	    sqrlCorrelator.setExpiryTime(expiryTime);
	}
    }

    @Override
    public String fetchTransientAuthData(final String correlator, final String dataName) {
	updateLastUsed(entityManager);
	final SqrlCorrelator correlatorObject = fetchSqrlCorrelatorRequired(correlator);
	return correlatorObject.getTransientAuthDataTable().get(dataName);
    }

    private Object returnOneOrNull(@SuppressWarnings("rawtypes") final List resultList) {
	if (resultList == null || resultList.isEmpty()) {
	    return null;
	} else if (resultList.size() == 1) {
	    return resultList.get(0);
	} else {
	    throw new SqrlPersistenceException("Expected one, but found multiple results: " + resultList);
	}
    }

    @Override
    public void closeCommit() {
	closeServletTransaction(true);
    }

    @Override
    public void closeRollback() {
	closeServletTransaction(false);
    }

    private void closeServletTransaction(final boolean commit) {
	if (!entityManager.isOpen()) {
	    throw new SqrlPersistenceException("EntityManager is not open");
	}
	if (commit) {
	    entityManager.getTransaction().commit();
	} else {
	    entityManager.getTransaction().rollback();
	}
	entityManager.close();
    }

    @Override
    public Boolean fetchSqrlFlagForIdentity(final String sqrlIdk, final SqrlFlag flagToFetch) {
	return fetchRequiredSqrlIdentity(sqrlIdk).getFlagTable().get(flagToFetch);
    }

    @Override
    public void setSqrlFlagForIdentity(final String sqrlIdk, final SqrlFlag flagToSet, final boolean valueToSet) {
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

    @Override
    public boolean isClosed() {
	return !entityManager.isOpen();
    }

    @Override
    public void cleanUpExpiredEntries() {
	final int rowsDeleted = entityManager.createQuery("DELETE FROM SqrlCorrelator i WHERE i.expiryTime < :now")
		.setParameter("now", new Date(), TemporalType.TIMESTAMP).executeUpdate();
	if (rowsDeleted > 0) {
	    logger.info("Cleanup deleted {}", rowsDeleted);
	}
    }

    /**
     * A task which periodically checks the state of various {@link EntityManager} instances to ensure they are being
     * closed properly by the library
     * 
     * @author Dave Badia
     *
     */
    private static final class EntityManagerMonitorTimerTask extends TimerTask {
	private static final long ENTITY_MANAGER_IDLE_WARN_THRESHOLD_MINUTES = 5;
	private static final long ENTITY_MANAGER_IDLE_WARN_THRESHOLD_MS = TimeUnit.MINUTES
		.toMillis(ENTITY_MANAGER_IDLE_WARN_THRESHOLD_MINUTES);

	@Override
	public void run() {
	    try {
		logger.debug("Running EntityManagerMonitorTimerTask");
		final Iterator<EntityManager> iter = CREATED_BY_STACK_TABLE.keySet().iterator();
		while (iter.hasNext()) {
		    @SuppressWarnings("squid:HiddenFieldCheck") // false-positive, this is a static inner class
		    final EntityManager entityManager = iter.next();
		    if (!entityManager.isOpen()) {
			logger.debug("entityManager closed, removing from monitor table");
			iter.remove();
			LAST_USED_TIME_TABLE.remove(entityManager); // May or may not exist, ok
		    } else {
			final Long lastUsed = LAST_USED_TIME_TABLE.get(entityManager);
			if (lastUsed == null) {
			    logger.error(
				    "EntityManagerMonitorTask found null lastUsedTime for entityManager which was created at",
				    CREATED_BY_STACK_TABLE.get(entityManager));
			} else {
			    if (System.currentTimeMillis() - lastUsed > ENTITY_MANAGER_IDLE_WARN_THRESHOLD_MS) {
				logger.error("Entity Manager is still open and has not been used for "
					+ ENTITY_MANAGER_IDLE_WARN_THRESHOLD_MINUTES + " minutes.  Was created from",
					CREATED_BY_STACK_TABLE.get(entityManager));
			    }
			}
		    }
		}
	    } catch (final RuntimeException e) {
		// thread
		logger.error("Error running entity manager monitor check", e);
	    }
	}
    }
}
