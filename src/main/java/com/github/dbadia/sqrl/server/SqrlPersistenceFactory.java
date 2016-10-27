package com.github.dbadia.sqrl.server;

public interface SqrlPersistenceFactory {
	public SqrlPersistence createSqrlPersistence();

	public Class<? extends Runnable> getCleanupTaskClass();
}
