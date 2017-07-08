package com.github.sqrlserverjava.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * As described in http://stackoverflow.com/a/30681018/2863942
 * https://gist.github.com/pcan/16faf4e59942678377e0#file-selfexpiringhashmap-java
 *
 * A thread-safe implementation of a HashMap which entries expires after the specified life time. The life-time can be
 * defined on a per-key basis, or using a default one, that is passed to the constructor.
 *
 * @author Pierantonio Cangianiello
 * @param <K>
 *            the Key type
 * @param <V>
 *            the Value type
 */
public class SelfExpiringHashMap<K, V> implements SelfExpiringMap<K, V> {
	private static final Logger logger = LoggerFactory.getLogger(SelfExpiringHashMap.class);

	private final Map<K, V> internalMap;

	private final Map<K, ExpiringKey<K>> expiringKeys;

	/**
	 * Holds the map keys using the given life time for expiration.
	 */
	@SuppressWarnings("rawtypes")
	private final DelayQueue<ExpiringKey> delayQueue = new DelayQueue<ExpiringKey>();

	/**
	 * The default max life time in milliseconds.
	 */
	private final long maxLifeTimeMillis;

	/**
	 * @author Dave Badia
	 * @deprecated use a constructor that specifies a timeout or don't use this class
	 */
	@Deprecated
	public SelfExpiringHashMap() {
		internalMap = new ConcurrentHashMap<K, V>();
		expiringKeys = new WeakHashMap<K, ExpiringKey<K>>();
		this.maxLifeTimeMillis = Long.MAX_VALUE;
	}

	public SelfExpiringHashMap(final long defaultMaxLifeTimeMillis) {
		internalMap = new ConcurrentHashMap<K, V>();
		expiringKeys = new WeakHashMap<K, ExpiringKey<K>>();
		this.maxLifeTimeMillis = defaultMaxLifeTimeMillis;
	}

	public SelfExpiringHashMap(final long defaultMaxLifeTimeMillis, final int initialCapacity) {
		internalMap = new ConcurrentHashMap<K, V>(initialCapacity);
		expiringKeys = new WeakHashMap<K, ExpiringKey<K>>(initialCapacity);
		this.maxLifeTimeMillis = defaultMaxLifeTimeMillis;
	}

	public SelfExpiringHashMap(final long defaultMaxLifeTimeMillis, final int initialCapacity, final float loadFactor) {
		internalMap = new ConcurrentHashMap<K, V>(initialCapacity, loadFactor);
		expiringKeys = new WeakHashMap<K, ExpiringKey<K>>(initialCapacity, loadFactor);
		this.maxLifeTimeMillis = defaultMaxLifeTimeMillis;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		cleanup();
		return internalMap.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		cleanup();
		return internalMap.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsKey(final Object key) {
		cleanup();
		return internalMap.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsValue(final Object value) {
		cleanup();
		return internalMap.containsValue(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(final Object key) {
		cleanup();
		renewKey((K) key);
		return internalMap.get(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V put(final K key, final V value) {
		return this.put(key, value, maxLifeTimeMillis);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public V put(final K key, final V value, final long lifeTimeMillis) {
		cleanup();
		final ExpiringKey delayedKey = new ExpiringKey(key, lifeTimeMillis);
		final ExpiringKey oldKey = expiringKeys.put(key, delayedKey);
		if (oldKey != null) {
			expireKey(oldKey);
			expiringKeys.put(key, delayedKey);
		}
		delayQueue.offer(delayedKey);
		return internalMap.put(key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V remove(final Object key) {
		final V removedValue = internalMap.remove(key);
		expireKey(expiringKeys.remove(key));
		return removedValue;
	}

	/**
	 * Not supported.
	 */
	@Override
	public void putAll(final Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean renewKey(final K key) {
		final ExpiringKey<K> delayedKey = expiringKeys.get(key);
		if (delayedKey != null) {
			delayedKey.renew();
			return true;
		}
		return false;
	}

	private void expireKey(final ExpiringKey<K> delayedKey) {
		if (delayedKey != null) {
			delayedKey.expire();
			cleanup();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		delayQueue.clear();
		expiringKeys.clear();
		internalMap.clear();
	}

	/**
	 * Originally threw UnsupportedOperationException. Modified by Dave
	 */
	@Override
	public Set<K> keySet() {
		return internalMap.keySet();
	}

	/**
	 * Originally threw UnsupportedOperationException. Modified by Dave
	 */
	@Override
	public Collection<V> values() {
		return internalMap.values();
	}

	/**
	 * Not supported.
	 */
	@Override
	public Set<Entry<K, V>> entrySet() {
		return internalMap.entrySet();
	}

	@SuppressWarnings("unchecked")
	private void cleanup() {
		ExpiringKey<K> delayedKey = delayQueue.poll();
		while (delayedKey != null) {
			if(delayedKey.getKey() != null) {
				logger.debug("SelfExpiringHashMap cleanup, removing " + delayedKey.getKey());
				internalMap.remove(delayedKey.getKey());
				expiringKeys.remove(delayedKey.getKey());
			}
			delayedKey = delayQueue.poll();
		}
	}

	private class ExpiringKey<L> implements Delayed {

		private long		startTime	= System.currentTimeMillis();
		private final long	maxLifeTimeMillis;
		private final L		key;

		public ExpiringKey(final L key, final long maxLifeTimeMillis) {
			this.maxLifeTimeMillis = maxLifeTimeMillis;
			this.key = key;
		}

		public L getKey() {
			return key;
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(final Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ExpiringKey<K> other = (ExpiringKey<K>) obj;
			if (this.key != other.key && (this.key == null || !this.key.equals(other.key))) {
				return false;
			}
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			int hash = 7;
			hash = 31 * hash + (this.key != null ? this.key.hashCode() : 0);
			return hash;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public long getDelay(final TimeUnit unit) {
			return unit.convert(getDelayMillis(), TimeUnit.MILLISECONDS);
		}

		private long getDelayMillis() {
			return (startTime + maxLifeTimeMillis) - System.currentTimeMillis();
		}

		public void renew() {
			startTime = System.currentTimeMillis();
		}

		public void expire() {
			startTime = System.currentTimeMillis() - maxLifeTimeMillis - 1;
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public int compareTo(final Delayed that) {
			return Long.compare(this.getDelayMillis(), ((ExpiringKey) that).getDelayMillis());
		}
	}

	@Override
	public String toString() {
		return internalMap.toString();
	}
}