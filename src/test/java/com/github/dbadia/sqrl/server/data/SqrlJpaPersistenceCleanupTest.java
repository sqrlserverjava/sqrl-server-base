package com.github.dbadia.sqrl.server.data;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;

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
		// TODO: not sure why we need this to avoid mysql deadlock after first test case
		/*
		 * SQLTransactionRollbackException: A lock could not be obtained within the time requested Error Code: 30000
		 * Call: SELECT id, authenticationStatus, expiryTime, value, authenticated_identity FROM sqrl_correlator Query:
		 * ReadAllQuery(referenceClass=SqrlCorrelator sql=
		 * "SELECT id, authenticationStatus, expiryTime, value, authenticated_identity FROM sqrl_correlator")
		 */
		EntityManagerFactory entityManagerFactory = (EntityManagerFactory) PrivateAccessor
				.getField(SqrlJpaPersistenceProvider.class, "entityManagerFactory");
		entityManagerFactory.close();
		entityManagerFactory = Persistence.createEntityManagerFactory(SqrlJpaPersistenceProvider.PERSISTENCE_UNIT_NAME);
		PrivateAccessor.setField(SqrlJpaPersistenceProvider.class, "entityManagerFactory", entityManagerFactory);
	}

	@Test
	public void testDateSetToLongest_NutIsLongest() throws Throwable {
		// Setup
		SqrlPersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence();
		final long now = System.currentTimeMillis();
		final String keepCorrelator = "keep";
		SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator("keep", new Date(now + 5000));
		sqrlPersistence.closeCommit();

		// Execute
		sqrlPersistence = TCUtil.createSqrlPersistence();
		final Date expectedDate = new Date(now + 6000);
		sqrlPersistence.markTokenAsUsed(keepCorrelator, "nutTokenKeep1", expectedDate);
		sqrlPersistence.closeCommit();

		// Validate
		sqrlPersistence = TCUtil.createSqrlPersistence();
		sqrlCorrelator = sqrlPersistence.fetchSqrlCorrelatorRequired(keepCorrelator);
		assertEquals(expectedDate, sqrlCorrelator.getExpiryTime());
		sqrlPersistence.closeCommit();
	}

	@Test
	public void testDateSetToLongest_CorrelatorIsLongest() throws Throwable {
		// Setup
		SqrlPersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence();
		final long now = System.currentTimeMillis();
		final String keepCorrelator = "keep";
		final Date expectedDate = new Date(now + 5000);
		SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator("keep", expectedDate);
		sqrlPersistence.closeCommit();

		// Execute
		sqrlPersistence = TCUtil.createSqrlPersistence();
		sqrlPersistence.markTokenAsUsed(keepCorrelator, "nutTokenKeep1", expectedDate);
		sqrlPersistence.closeCommit();

		// Validate
		sqrlPersistence = TCUtil.createSqrlPersistence();
		sqrlCorrelator = sqrlPersistence.fetchSqrlCorrelatorRequired(keepCorrelator);
		assertEquals(expectedDate, sqrlCorrelator.getExpiryTime());
		sqrlPersistence.closeCommit();
	}

	@Test
	public void testCleanup() throws Throwable {
		SqrlPersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence();

		final long now = System.currentTimeMillis();
		final String keepCorrelator = "keep";
		sqrlPersistence.createCorrelator(keepCorrelator, new Date(now + 5000));
		final Date expectedDate = new Date(now + 6000);
		sqrlPersistence.markTokenAsUsed(keepCorrelator, "nutTokenKeep1", expectedDate);

		final String deleteCorrelator = "delete";
		sqrlPersistence.createCorrelator(deleteCorrelator, new Date(now - 1000));
		sqrlPersistence.markTokenAsUsed(deleteCorrelator, "nutTokenDelete1", new Date(now - 1000));
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

}
