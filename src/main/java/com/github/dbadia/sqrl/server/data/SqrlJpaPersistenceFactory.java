package com.github.dbadia.sqrl.server.data;

import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlPersistenceFactory;
import com.github.dbadia.sqrl.server.data.SqrlJpaPersistenceProvider.SqrlJpaEntityManagerMonitorTimerTask;

public class SqrlJpaPersistenceFactory implements SqrlPersistenceFactory {

	@SuppressWarnings("deprecation")
	@Override
	public SqrlPersistence createSqrlPersistence() {
		return new SqrlJpaPersistenceProvider();
	}

	@Override
	public Class<? extends Runnable> getCleanupTaskClass() {
		return SqrlJpaEntityManagerMonitorTimerTask.class;
	}

}
