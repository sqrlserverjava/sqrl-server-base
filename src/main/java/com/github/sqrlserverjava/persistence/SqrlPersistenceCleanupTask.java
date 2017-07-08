package com.github.sqrlserverjava.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlPersistenceFactory;

public class SqrlPersistenceCleanupTask implements Runnable {
	private static final Logger				logger	= LoggerFactory.getLogger(SqrlPersistenceCleanupTask.class);
	private final SqrlPersistenceFactory	persistenceFactory;

	public SqrlPersistenceCleanupTask(final SqrlPersistenceFactory persistenceFactory) {
		super();
		this.persistenceFactory = persistenceFactory;
	}

	@Override
	public void run() {
		try (SqrlAutoCloseablePersistence sqrlPersistence = new SqrlAutoCloseablePersistence(
				persistenceFactory.createSqrlPersistence())) {
			sqrlPersistence.cleanUpExpiredEntries();
			sqrlPersistence.closeCommit();
		} catch (final RuntimeException e) {
			logger.error("Error during execution cleanup tasks", e);
		}
	}

}
