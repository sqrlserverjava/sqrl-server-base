package com.github.sqrlserverjava;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * A SecureRandom which isn't random at all and returns a predefined value or an incrmenting counter starting at zero.
 * It is used for test cases to:
 * <li/>be fast as there is no initialization
 * <li/>generate reproducible test data
 * 
 * @author Dave Badia
 */
public class TestSecureRandom extends SecureRandom {
	private static final long	serialVersionUID	= 1L;
	private byte				counter				= 0;
	private final byte[]		bytesToReturn;

	public TestSecureRandom(final byte[] bytesToReturn) {
		this.bytesToReturn = bytesToReturn;
	}

	public TestSecureRandom(final int intToReturn) {
		this.bytesToReturn = ByteBuffer.allocate(4).putInt(intToReturn).array();
	}

	@Override
	synchronized public void nextBytes(final byte[] bytes) {
		if (bytesToReturn == null) {
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = counter++;
			}
		} else {
			System.arraycopy(bytesToReturn, 0, bytes, 0, bytes.length);
		}
	}
}