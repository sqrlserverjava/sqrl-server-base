package com.github.dbadia.sqrl.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.junit.Test;

import com.github.dbadia.sqrl.server.persistence.SqrlAutoCloseablePersistence;
import com.github.dbadia.sqrl.server.persistence.SqrlCorrelator;


public class SqrlAuthStateEventServletTest {

	@Test
	public void testFetchSqrlCorrelatorsDetached() throws NoSuchFieldException {
		final String abc = "abc";
		final String def = "def";
		final String ghi = "ghi";

		SqrlCorrelator corrAbc = null;
		SqrlCorrelator corrDef = null;
		try (SqrlAutoCloseablePersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence()) {
			corrAbc = sqrlPersistence.createCorrelator(abc, minutesFromNow(3));
			corrDef = sqrlPersistence.createCorrelator(def, minutesFromNow(3));
			sqrlPersistence.createCorrelator(ghi, minutesFromNow(3));
			sqrlPersistence.closeCommit();
		}

		final Set<String> correlatorsToCheck = new HashSet<>();
		correlatorsToCheck.add(abc);
		correlatorsToCheck.add(def);

		Map<String, SqrlCorrelator> correlatorTable = null;
		try (SqrlAutoCloseablePersistence sqrlPersistence = TCUtil.createSqrlPersistence()) {
			correlatorTable = sqrlPersistence.fetchSqrlCorrelatorsDetached(correlatorsToCheck);
			sqrlPersistence.closeCommit();
		}
		assertNotNull(correlatorTable);
		assertEquals(2, correlatorTable.size());
		assertEquals(corrAbc, correlatorTable.get(abc));
		assertEquals(corrDef, correlatorTable.get(def));
	}

	@Test
	public void testFetchSqrlCorrelatorsStatusChanged_NoChange() throws NoSuchFieldException {
		final String abc = "abc";
		final String def = "def";

		SqrlCorrelator corrAbc = null;
		SqrlCorrelator corrDef = null;
		try (SqrlAutoCloseablePersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence()) {
			corrAbc = sqrlPersistence.createCorrelator(abc, minutesFromNow(3));
			corrDef = sqrlPersistence.createCorrelator(def, minutesFromNow(3));
			sqrlPersistence.closeCommit();
		}

		final Map<String, SqrlAuthenticationStatus> correlatorToCurrentStatusTable = new ConcurrentHashMap<>();
		correlatorToCurrentStatusTable.put(abc, corrAbc.getAuthenticationStatus());
		correlatorToCurrentStatusTable.put(def, corrDef.getAuthenticationStatus());

		Map<String, SqrlAuthenticationStatus> statusChangedTable = null;
		try (SqrlAutoCloseablePersistence sqrlPersistence = TCUtil.createSqrlPersistence()) {
			statusChangedTable = sqrlPersistence.fetchSqrlCorrelatorStatusUpdates(correlatorToCurrentStatusTable);
			sqrlPersistence.closeCommit();
		}

		// Should be empty but not null
		assertNotNull(statusChangedTable);
		assertEquals(0, statusChangedTable.size());
	}

	public void testIt() throws NoSuchFieldException {
		final String abc = "abc";
		final String def = "def";
		final String ghi = "ghi";

		final List<String> stringList = Arrays.asList(abc, def, ghi, "xyz");

		try (SqrlAutoCloseablePersistence sqrlPersistence = TCUtil.createEmptySqrlPersistence()) {
			sqrlPersistence.createCorrelator(abc, minutesFromNow(3));
			sqrlPersistence.createCorrelator(def, minutesFromNow(3));
			sqrlPersistence.createCorrelator(ghi, minutesFromNow(3));
			sqrlPersistence.closeCommit();
		}

		final List<SqrlMiniCorrelator> correlatorsToCheck = fetchCorrelatorIdsAndState(stringList);
		// Correlaotr that got expired
		correlatorsToCheck.add(new SqrlMiniCorrelator(5, "xyz", SqrlAuthenticationStatus.CORRELATOR_ISSUED));
		try (SqrlAutoCloseablePersistence sqrlPersistence = TCUtil.createSqrlPersistence()) {
			final SqrlCorrelator correlator = sqrlPersistence.fetchSqrlCorrelator(def);
			correlator.setAuthenticationStatus(SqrlAuthenticationStatus.ERROR_BAD_REQUEST);
			sqrlPersistence.closeCommit();
		}
		monitorCorrelatorsForStateChange(correlatorsToCheck);
	}

	private void monitorCorrelatorsForStateChange(final List<SqrlMiniCorrelator> correaltorList)
			throws NoSuchFieldException {
		try (SqrlAutoCloseablePersistence sqrlPersistence = TCUtil.createSqrlPersistence()) {
			final EntityManagerFactory entityManagerFactory = TCUtil.extractEntityManagerFactory(sqrlPersistence);
			final EntityManager entityManager = entityManagerFactory.createEntityManager();
			final StringBuilder buf = new StringBuilder(
					"SELECT c.id, c.value, c.authenticationStatus FROM SqrlCorrelator AS c WHERE ");
			for (final SqrlMiniCorrelator correlator : correaltorList) {
				buf.append(" (c.id = ").append(correlator.id).append(" AND c.authenticationStatus != :as")
						.append(correlator.id).append(" )").append(" OR ");
			}
			final String queryString = buf.substring(0, buf.length() - 3);

			final TypedQuery<Object[]> query = entityManager.createQuery(queryString, Object[].class);
			for (final SqrlMiniCorrelator correlator : correaltorList) {
				query.setParameter("as" + correlator.id, correlator.status);
			}
			final List<Object[]> results = query.getResultList();
			System.err.println("Results:");
			for (final Object[] result : results) {
				System.err.println(
						"id: " + result[0] + ", value = " + result[1] + ", authenticationStatus: " + result[2]);
			}
			System.err.println(buf.toString());

			sqrlPersistence.closeCommit();
			sqrlPersistence.close();
			entityManager.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static List<SqrlMiniCorrelator> fetchCorrelatorIdsAndState(final List<String> correaltorStringList)
			throws NoSuchFieldException {
		if (correaltorStringList.isEmpty()) {
			return Collections.emptyList();
		}
		try (SqrlAutoCloseablePersistence sqrlPersistence = TCUtil.createSqrlPersistence()) {
			final EntityManagerFactory entityManagerFactory = TCUtil.extractEntityManagerFactory(sqrlPersistence);
			final EntityManager entityManager = entityManagerFactory.createEntityManager();

			final StringBuilder buf = new StringBuilder(
					"SELECT c.id, c.value, c.authenticationStatus FROM SqrlCorrelator AS c WHERE ");
			for (final String correlatorString : correaltorStringList) {
				buf.append(" c.value = '").append(correlatorString).append("' OR ");
			}
			final String queryString = buf.substring(0, buf.length() - 3);
			final TypedQuery<SqrlMiniCorrelator> query = entityManager.createQuery(queryString,
					SqrlMiniCorrelator.class);

			@SuppressWarnings("rawtypes") // need to map to SqrlMiniCorrelator or Object[] below
			final List results = query.getResultList();
			List<SqrlMiniCorrelator> toReturn = null;
			// Work around a JPA bug?
			if (results.isEmpty()) {
				toReturn = Collections.emptyList();
			} else {
				final Object firstOne = results.get(0);
				if (firstOne instanceof SqrlMiniCorrelator) {
					toReturn = results;
				} else if (firstOne.getClass().isArray()) {
					// We got List<Object[]>
					toReturn = new ArrayList<>();
					for (int i = 0; i < results.size(); i++) {
						final Object[] objectArray = (Object[]) results.get(i);
						toReturn.add(new SqrlMiniCorrelator((long) objectArray[0], (String) objectArray[1],
								(SqrlAuthenticationStatus) objectArray[2]));
					}
				}
			}
			sqrlPersistence.closeCommit();
			sqrlPersistence.close();
			entityManager.close();
			return toReturn;
		}
	}

	private static class SqrlMiniCorrelator {
		private final long						id;
		private final String					value;
		private final SqrlAuthenticationStatus	status;

		public SqrlMiniCorrelator(final long id, final String value, final SqrlAuthenticationStatus status) {
			super();
			this.id = id;
			this.value = value;
			this.status = status;
		}
	}

	private static Date minutesFromNow(final int i) {
		return new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(i));
	}
}
