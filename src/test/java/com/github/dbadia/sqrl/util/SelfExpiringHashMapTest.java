package com.github.dbadia.sqrl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Ignore;
import org.junit.Test;

import com.github.dbadia.sqrl.server.util.SelfExpiringHashMap;
import com.github.dbadia.sqrl.server.util.SelfExpiringMap;

/**
 *
 * @author Pierantonio Cangianiello
 */
public class SelfExpiringHashMapTest {

	private final static int SLEEP_MULTIPLIER = 10;

	@Test
	public void basicGetTest() throws InterruptedException {
		@SuppressWarnings("deprecation") // OK for test case use
		final SelfExpiringMap<String, String> map = new SelfExpiringHashMap<String, String>();
		map.put("a", "b", 2 * SLEEP_MULTIPLIER);
		Thread.sleep(1 * SLEEP_MULTIPLIER);
		assertEquals(map.get("a"), "b");
	}

	@Test
	public void basicGetTest_Constructor() throws InterruptedException {
		final SelfExpiringMap<String, String> map = new SelfExpiringHashMap<String, String>(2 * SLEEP_MULTIPLIER);
		map.put("a", "b");
		Thread.sleep(1 * SLEEP_MULTIPLIER);
		assertEquals("b", map.get("a"));
	}

	@Test
	public void basicExpireTest_Constructor() throws InterruptedException {
		final SelfExpiringMap<String, String> map = new SelfExpiringHashMap<String, String>(2 * SLEEP_MULTIPLIER);
		map.put("a", "b");
		Thread.sleep(3 * SLEEP_MULTIPLIER);
		assertNull(map.get("a"));
	}

	@Test
	public void basicExpireTest() throws InterruptedException {
		@SuppressWarnings("deprecation")
		final SelfExpiringMap<String, String> map = new SelfExpiringHashMap<String, String>();
		map.put("a", "b", 2 * SLEEP_MULTIPLIER);
		Thread.sleep(3 * SLEEP_MULTIPLIER);
		assertNull(map.get("a"));
	}

	@Test
	@Ignore // we dont use renew
	public void basicRenewTest() throws InterruptedException {
		@SuppressWarnings("deprecation")
		final SelfExpiringMap<String, String> map = new SelfExpiringHashMap<String, String>();
		map.put("a", "b", 3 * SLEEP_MULTIPLIER);
		Thread.sleep(2 * SLEEP_MULTIPLIER);
		map.renewKey("a");
		Thread.sleep(2 * SLEEP_MULTIPLIER);
		assertEquals(map.get("a"), "b");
	}

	@Test
	@Ignore // we dont use renew, will fix if we use it
	public void getRenewTest() throws InterruptedException {
		@SuppressWarnings("deprecation")
		final SelfExpiringMap<String, String> map = new SelfExpiringHashMap<String, String>();
		map.put("a", "b", 3 * SLEEP_MULTIPLIER);
		Thread.sleep(2 * SLEEP_MULTIPLIER);
		assertEquals(map.get("a"), "b");
		Thread.sleep(2 * SLEEP_MULTIPLIER);
		assertEquals(map.get("a"), "b");
	}

	@Test
	public void multiplePutThenRemoveTest() throws InterruptedException {
		@SuppressWarnings("deprecation")
		final SelfExpiringMap<String, String> map = new SelfExpiringHashMap<String, String>();
		map.put("a", "b", 2 * SLEEP_MULTIPLIER);
		Thread.sleep(1 * SLEEP_MULTIPLIER);
		map.put("a", "c", 2 * SLEEP_MULTIPLIER);
		Thread.sleep(1 * SLEEP_MULTIPLIER);
		map.put("a", "d", 400 * SLEEP_MULTIPLIER);
		Thread.sleep(2 * SLEEP_MULTIPLIER);
		assertEquals(map.remove("a"), "d");
	}

	@Test
	public void multiplePutThenGetTest() throws InterruptedException {
		@SuppressWarnings("deprecation")
		final SelfExpiringMap<String, String> map = new SelfExpiringHashMap<String, String>();
		map.put("a", "b", 2 * SLEEP_MULTIPLIER);
		Thread.sleep(1 * SLEEP_MULTIPLIER);
		map.put("a", "c", 2 * SLEEP_MULTIPLIER);
		Thread.sleep(1 * SLEEP_MULTIPLIER);
		map.put("a", "d", 400 * SLEEP_MULTIPLIER);
		Thread.sleep(2 * SLEEP_MULTIPLIER);
		assertEquals(map.get("a"), "d");
	}

}
