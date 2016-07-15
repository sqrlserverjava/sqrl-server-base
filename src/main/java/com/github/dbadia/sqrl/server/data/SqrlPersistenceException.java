package com.github.dbadia.sqrl.server.data;

import javax.persistence.PersistenceException;

import com.github.dbadia.sqrl.server.SqrlPersistence;

/**
 * Indicates a problem (such as data corruption, inaccessibility, etc) with the web apps {@link SqrlPersistence}; note
 * that, in keeping with the semantics of JPA, this is a {@link RuntimeException}
 * 
 * @author Dave Badia
 */
public class SqrlPersistenceException extends PersistenceException {
	private static final long serialVersionUID = 1650507051187559258L;

	public SqrlPersistenceException(final String message) {
		super(message);
	}

}
