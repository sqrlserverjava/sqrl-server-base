package com.github.dbadia.sqrl.server;

/**
 * Helps test cases in other packages create objects from this one
 * 
 * @author Dave Badia
 *
 */
public class TestHelper {

	public static SqrlConfigOperations newSqrlConfigOperations(final SqrlConfig sqrlConfig) {
		return new SqrlConfigOperations(sqrlConfig);
	}

}
