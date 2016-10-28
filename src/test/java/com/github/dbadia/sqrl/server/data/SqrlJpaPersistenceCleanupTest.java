package com.github.dbadia.sqrl.server.data;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Test;

import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.TCUtil;

import junitx.framework.ObjectAssert;
import junitx.util.PrivateAccessor;

public class SqrlJpaPersistenceCleanupTest {
	@After
	public void tearDown() throws NoSuchFieldException {
		/*
		 * This was found to be necessary becuase of this error: SQLTransactionRollbackException: A lock could not be
		 * obtained within the time requested Error Code: 30000 Call: SELECT id, authenticationStatus, expiryTime,
		 * value, authenticated_identity FROM sqrl_correlator Query: ReadAllQuery(referenceClass=SqrlCorrelator sql=
		 * "SELECT id, authenticationStatus, expiryTime, value, authenticated_identity FROM sqrl_correlator")
		 */
		EntityManagerFactory entityManagerFactory = (EntityManagerFactory) PrivateAccessor
				.getField(SqrlJpaPersistenceProvider.class, "entityManagerFactory");
		entityManagerFactory.close();
		entityManagerFactory = Persistence.createEntityManagerFactory(SqrlJpaPersistenceProvider.PERSISTENCE_UNIT_NAME);
		PrivateAccessor.setField(SqrlJpaPersistenceProvider.class, "entityManagerFactory", entityManagerFactory);
	}

	@Test
	public void testCleanupCorrelator() throws Throwable {
		SqrlPersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence();

		final long now = System.currentTimeMillis();
		final String keepCorrelator = "keep";
		sqrlPersistence.createCorrelator(keepCorrelator, new Date(now + 5000));

		final String deleteCorrelator = "delete";
		sqrlPersistence.createCorrelator(deleteCorrelator, new Date(now - 1000));
		sqrlPersistence.closeCommit();

		// Execute
		sqrlPersistence = TCUtil.createSqrlPersistence();
		sqrlPersistence.cleanUpExpiredEntries();
		sqrlPersistence.closeCommit();

		// Verify
		sqrlPersistence = TCUtil.createSqrlPersistence();
		assertNotNull(sqrlPersistence.fetchSqrlCorrelatorRequired(keepCorrelator));
		try {
			sqrlPersistence.fetchSqrlCorrelatorRequired(deleteCorrelator);
			fail("Exeption expected");
		} catch (final Exception e) {
			ObjectAssert.assertInstanceOf(SqrlPersistenceException.class, e);
		}
	}

	@Test
	public void testCleanupNutToken() throws Throwable {
		SqrlPersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence();

		final long now = System.currentTimeMillis();
		final Date expectedDate = new Date(now + 6000);
		final String keepToken = "nBuewGyan2u2Yx1McUXetQ";
		sqrlPersistence.markTokenAsUsed(keepToken, expectedDate);

		final String deleteToken = "qhDh85lYnwZzMYSrAEnkew";
		sqrlPersistence.markTokenAsUsed("nutTokenDelete1", new Date(now - 1000));
		sqrlPersistence.closeCommit();

		// Execute
		sqrlPersistence = TCUtil.createSqrlPersistence();
		sqrlPersistence.cleanUpExpiredEntries();
		sqrlPersistence.closeCommit();

		// Verify
		sqrlPersistence = TCUtil.createSqrlPersistence();
		assertTrue(sqrlPersistence.hasTokenBeenUsed(keepToken));
		assertFalse(sqrlPersistence.hasTokenBeenUsed(deleteToken));
		sqrlPersistence.closeCommit();
	}

}
