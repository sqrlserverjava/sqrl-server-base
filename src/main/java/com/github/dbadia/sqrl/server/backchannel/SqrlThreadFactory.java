package com.github.dbadia.sqrl.server.backchannel;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The only reason for this is to give our threads meaningful names
 * @author Dave Badia
 *
 */
public class SqrlThreadFactory implements ThreadFactory {
	private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
	@Override
	public Thread newThread(final Runnable r) {
		return new Thread(r, "Sqrl Background #" + THREAD_COUNT.getAndIncrement());
	}
}
