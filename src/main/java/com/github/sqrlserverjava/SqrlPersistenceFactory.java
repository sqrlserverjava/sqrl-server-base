package com.github.sqrlserverjava;

public interface SqrlPersistenceFactory {
	public SqrlPersistence createSqrlPersistence();

	public Class<? extends Runnable> getCleanupTaskClass();
}
