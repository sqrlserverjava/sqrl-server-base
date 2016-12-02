package com.github.dbadia.sqrl.server.persistence;

import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlPersistenceFactory;
import com.github.dbadia.sqrl.server.persistence.SqrlJpaPersistenceProvider.SqrlJpaEntityManagerMonitorTask;

public class SqrlJpaPersistenceFactory implements SqrlPersistenceFactory {

	@SuppressWarnings("deprecation")
	@Override
	public SqrlPersistence createSqrlPersistence() {
		return new SqrlJpaPersistenceProvider();
	}

	@Override
	public Class<? extends Runnable> getCleanupTaskClass() {
		return SqrlJpaEntityManagerMonitorTask.class;
	}

}
