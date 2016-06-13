package com.github.dbadia.sqrl.server;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * A SecureRandom which isn't random at all and always returns zero. It's used for test cases to </br>
 * 1) be fast as there is no init</br>
 * 2) generate reproducable test data
 * 
 * @author Dave Badia
 * @deprecated Test case data use ONLY
 */
@Deprecated class TestSecureRandom extends SecureRandom {
	@Override
	synchronized public void nextBytes(final byte[] bytes) {
		Arrays.fill(bytes, (byte) 0);
	}
}