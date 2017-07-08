package com.github.sqrlserverjava.persistence;

import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.SqrlPersistenceFactory;
import com.github.sqrlserverjava.persistence.SqrlJpaPersistenceProvider.SqrlJpaEntityManagerMonitorTask;

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
