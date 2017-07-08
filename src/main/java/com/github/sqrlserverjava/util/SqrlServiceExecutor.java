package com.github.sqrlserverjava.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlConfigOperations;
import com.github.sqrlserverjava.SqrlServerOperations;

@WebListener
public class SqrlServiceExecutor implements ServletContextListener {
	private static final Logger logger = LoggerFactory.getLogger(SqrlServiceExecutor.class);

	/**
	 * DB cleanup may be slow running, so ensure another thread is always available to check for status updates from
	 * SQRL clients
	 */
	private static final int						THREAD_COUNT		= 2;
	private static final ScheduledExecutorService	EXECUTOR_SERVICE	= Executors.newScheduledThreadPool(THREAD_COUNT,
			new SqrlThreadFactory());

	@SuppressWarnings("rawtypes")
	private static List<ScheduledFuture> backgroundTaskList = new ArrayList<>();

	@Override
	public void contextInitialized(final ServletContextEvent servletContextEvent) {
		// Perform dependency injection
		SqrlServerOperations.setExecutor(this);
		SqrlConfigOperations.setExecutor(this);
	}

	public void scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period,
			final TimeUnit unit) {
		@SuppressWarnings("rawtypes")
		final ScheduledFuture future = EXECUTOR_SERVICE.scheduleAtFixedRate(runnable, initialDelay, period, unit);
		backgroundTaskList.add(future);
	}

	@Override
	public void contextDestroyed(final ServletContextEvent arg0) {
		logger.info("Shutting down background tasks and executor service");
		for (@SuppressWarnings("rawtypes")
		final ScheduledFuture backgroundTask : backgroundTaskList) {
			backgroundTask.cancel(false);
		}
		EXECUTOR_SERVICE.shutdown();
	}

	/**
	 * The only reason for this is to give our threads meaningful names
	 *
	 * @author Dave Badia
	 *
	 */
	private static class SqrlThreadFactory implements ThreadFactory {
		private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

		@Override
		public Thread newThread(final Runnable r) {
			return new Thread(r, "Sqrl Background #" + THREAD_COUNTER.getAndIncrement());
		}
	}
}
