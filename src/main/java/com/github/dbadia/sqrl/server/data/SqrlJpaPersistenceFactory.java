package com.github.dbadia.sqrl.server.data;

import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlPersistenceFactory;

public class SqrlJpaPersistenceFactory implements SqrlPersistenceFactory {

	@Override
	public SqrlPersistence createSqrlPersistence() {
		return new SqrlJpaPersistenceProvider();
	}

}
