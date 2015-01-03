/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.karaf.jaas.modules.mongo.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.security.auth.spi.LoginModule;

import com.mongodb.MongoClient;

/**
 * 
 * NOTE: borrowed from the 1.x branch of the Apache MINA project to add a very
 * simple cache to the mongo login module without creating other external
 * dependencies. This is required to ensure the MongoClient is propery closed as
 * the JAAS framework does not have any proper mechanism to deal with
 * {@link LoginModule} resources. Failure to close the {@link MongoClient} will
 * result in thread leakage for the mongo cursor cleaner thread. The
 * {@link MongoClient} cached object itself is thread safe.
 * 
 * A map with expiration. This class contains a worker thread that will
 * periodically check this class in order to determine if any objects should be
 * removed based on the provided time-to-live value.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * 
 *         A newer version of this class can be found on the <a href=
 *         "https://github.com/apache/mina/blob/trunk/coap/src/main/java/org/apache/mina/coap/retry/ExpiringMap.java"
 *         >GitHub MINA project</a>
 * 
 */
public class ExpiringMap<K, V> implements Map<K, V> {

	/**
	 * The default value, 60
	 */
	public static final int DEFAULT_TIME_TO_LIVE = 60;

	/**
	 * The default value, 1
	 */
	public static final int DEFAULT_EXPIRATION_INTERVAL = 1;

	private static volatile int expirerCount = 1;

	private final ConcurrentHashMap<K, ExpiringObject> delegate;

	private final CopyOnWriteArrayList<ExpirationListener<V>> expirationListeners;

	private final Expirer expirer;

	/**
	 * Creates a new instance of ExpiringMap using the default values
	 * DEFAULT_TIME_TO_LIVE and DEFAULT_EXPIRATION_INTERVAL
	 *
	 */
	public ExpiringMap() {
		this(DEFAULT_TIME_TO_LIVE, DEFAULT_EXPIRATION_INTERVAL);
	}

	/**
	 * Creates a new instance of ExpiringMap using the supplied time-to-live
	 * value and the default value for DEFAULT_EXPIRATION_INTERVAL
	 *
	 * @param timeToLive
	 *            The time-to-live value (seconds)
	 */
	public ExpiringMap(int timeToLive) {
		this(timeToLive, DEFAULT_EXPIRATION_INTERVAL);
	}

	/**
	 * Creates a new instance of ExpiringMap using the supplied values and a
	 * {@link ConcurrentHashMap} for the internal data structure.
	 *
	 * @param timeToLive
	 *            The time-to-live value (seconds)
	 * @param expirationInterval
	 *            The time between checks to see if a value should be removed
	 *            (seconds)
	 */
	public ExpiringMap(int timeToLive, int expirationInterval) {
		this(new ConcurrentHashMap<K, ExpiringObject>(),
				new CopyOnWriteArrayList<ExpirationListener<V>>(), timeToLive,
				expirationInterval);
	}

	private ExpiringMap(ConcurrentHashMap<K, ExpiringObject> delegate,
			CopyOnWriteArrayList<ExpirationListener<V>> expirationListeners,
			int timeToLive, int expirationInterval) {
		this.delegate = delegate;
		this.expirationListeners = expirationListeners;

		this.expirer = new Expirer();
		expirer.setTimeToLive(timeToLive);
		expirer.setExpirationInterval(expirationInterval);
	}

	public V put(K key, V value) {
		ExpiringObject answer = delegate.put(key, new ExpiringObject(key,
				value, System.currentTimeMillis()));
		if (answer == null) {
			return null;
		}

		return answer.getValue();
	}

	public V get(Object key) {
		ExpiringObject object = delegate.get(key);

		if (object != null) {
			object.setLastAccessTime(System.currentTimeMillis());

			return object.getValue();
		}

		return null;
	}

	public V remove(Object key) {
		ExpiringObject answer = delegate.remove(key);
		if (answer == null) {
			return null;
		}

		return answer.getValue();
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public void clear() {
		delegate.clear();
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	public Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public void putAll(Map<? extends K, ? extends V> inMap) {
		for (Entry<? extends K, ? extends V> e : inMap.entrySet()) {
			this.put(e.getKey(), e.getValue());
		}
	}

	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	public Set<Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

	public void addExpirationListener(ExpirationListener<V> listener) {
		expirationListeners.add(listener);
	}

	public void removeExpirationListener(ExpirationListener<V> listener) {
		expirationListeners.remove(listener);
	}

	public Expirer getExpirer() {
		return expirer;
	}

	public int getExpirationInterval() {
		return expirer.getExpirationInterval();
	}

	public int getTimeToLive() {
		return expirer.getTimeToLive();
	}

	public void setExpirationInterval(int expirationInterval) {
		expirer.setExpirationInterval(expirationInterval);
	}

	public void setTimeToLive(int timeToLive) {
		expirer.setTimeToLive(timeToLive);
	}

	@Override
	protected void finalize() throws Throwable {
		// make sure the expiration deamon thread is dumped when the map is
		// garbage collected
		if (expirer.isRunning()) {
			expirer.stopExpiring();
		}
	}

	private class ExpiringObject {
		private K key;

		private V value;

		private long lastAccessTime;

		private final ReadWriteLock lastAccessTimeLock = new ReentrantReadWriteLock();

		ExpiringObject(K key, V value, long lastAccessTime) {
			if (value == null) {
				throw new IllegalArgumentException(
						"An expiring object cannot be null.");
			}

			this.key = key;
			this.value = value;
			this.lastAccessTime = lastAccessTime;
		}

		public long getLastAccessTime() {
			lastAccessTimeLock.readLock().lock();

			try {
				return lastAccessTime;
			} finally {
				lastAccessTimeLock.readLock().unlock();
			}
		}

		public void setLastAccessTime(long lastAccessTime) {
			lastAccessTimeLock.writeLock().lock();

			try {
				this.lastAccessTime = lastAccessTime;
			} finally {
				lastAccessTimeLock.writeLock().unlock();
			}
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		@Override
		public boolean equals(Object obj) {
			return value.equals(obj);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}

	/**
	 * A Thread that monitors an {@link ExpiringMap} and will remove elements
	 * that have passed the threshold.
	 *
	 */
	public class Expirer implements Runnable {

		private final ReadWriteLock stateLock = new ReentrantReadWriteLock();

		private long timeToLiveMillis;

		private long expirationIntervalMillis;

		private boolean running = false;

		private final Thread expirerThread;

		/**
		 * Creates a new instance of Expirer.
		 *
		 */
		public Expirer() {
			expirerThread = new Thread(this, "MongoClientCacheCleaner-"
					+ expirerCount++);
			expirerThread.setDaemon(true);
		}

		public void run() {
			while (running) {
				processExpires();

				try {
					Thread.sleep(expirationIntervalMillis);
				} catch (InterruptedException e) {
				}
			}
		}

		private void processExpires() {
			long timeNow = System.currentTimeMillis();

			for (ExpiringObject o : delegate.values()) {

				if (timeToLiveMillis <= 0) {
					continue;
				}

				long timeIdle = timeNow - o.getLastAccessTime();

				if (timeIdle >= timeToLiveMillis) {
					delegate.remove(o.getKey());

					for (ExpirationListener<V> listener : expirationListeners) {
						listener.expired(o.getValue());
					}
				}
			}
		}

		/**
		 * Kick off this thread which will look for old objects and remove them.
		 *
		 */
		public void startExpiring() {
			stateLock.writeLock().lock();

			try {
				if (!running) {
					running = true;
					expirerThread.start();
				}
			} finally {
				stateLock.writeLock().unlock();
			}
		}

		/**
		 * If this thread has not started, then start it. Otherwise just return;
		 */
		public void startExpiringIfNotStarted() {
			stateLock.readLock().lock();
			try {
				if (running) {
					return;
				}
			} finally {
				stateLock.readLock().unlock();
			}

			stateLock.writeLock().lock();
			try {
				if (!running) {
					running = true;
					expirerThread.start();
				}
			} finally {
				stateLock.writeLock().unlock();
			}
		}

		/**
		 * Stop the thread from monitoring the map.
		 */
		public void stopExpiring() {
			stateLock.writeLock().lock();

			try {
				if (running) {
					running = false;
					expirerThread.interrupt();
				}
			} finally {
				stateLock.writeLock().unlock();
			}
		}

		/**
		 * Checks to see if the thread is running
		 *
		 * @return If the thread is running, true. Otherwise false.
		 */
		public boolean isRunning() {
			stateLock.readLock().lock();

			try {
				return running;
			} finally {
				stateLock.readLock().unlock();
			}
		}

		/**
		 * Returns the Time-to-live value.
		 *
		 * @return The time-to-live (seconds)
		 */
		public int getTimeToLive() {
			stateLock.readLock().lock();

			try {
				return (int) timeToLiveMillis / 1000;
			} finally {
				stateLock.readLock().unlock();
			}
		}

		/**
		 * Update the value for the time-to-live
		 *
		 * @param timeToLive
		 *            The time-to-live (seconds)
		 */
		public void setTimeToLive(long timeToLive) {
			stateLock.writeLock().lock();

			try {
				this.timeToLiveMillis = timeToLive * 1000;
			} finally {
				stateLock.writeLock().unlock();
			}
		}

		/**
		 * Get the interval in which an object will live in the map before it is
		 * removed.
		 *
		 * @return The time in seconds.
		 */
		public int getExpirationInterval() {
			stateLock.readLock().lock();

			try {
				return (int) expirationIntervalMillis / 1000;
			} finally {
				stateLock.readLock().unlock();
			}
		}

		/**
		 * Set the interval in which an object will live in the map before it is
		 * removed.
		 *
		 * @param expirationInterval
		 *            The time in seconds
		 */
		public void setExpirationInterval(long expirationInterval) {
			stateLock.writeLock().lock();

			try {
				this.expirationIntervalMillis = expirationInterval * 1000;
			} finally {
				stateLock.writeLock().unlock();
			}
		}
	}
}
