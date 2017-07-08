package com.github.sqrlserverjava;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * A SecureRandom which isn't random at all and always returns zero. It's used for test cases to </br>
 * 1) be fast as there is no init</br>
 * 2) generate reproducible test data
 * 
 * @author Dave Badia
 */
@Deprecated
class TestSecureRandom extends SecureRandom {
	private static final long	serialVersionUID	= 1L;
	private final byte[]		bytesToReturn;

	public TestSecureRandom(final byte[] bytesToReturn) {
		this.bytesToReturn = bytesToReturn;
	}

	@Override
	synchronized public void nextBytes(final byte[] bytes) {
		if (bytesToReturn == null) {
			Arrays.fill(bytes, (byte) 0);
		} else {
			System.arraycopy(bytesToReturn, 0, bytes, 0, bytes.length);
		}
	}
}