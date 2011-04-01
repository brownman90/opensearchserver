/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008-2011 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of Jaeksoft OpenSearchServer.
 *
 * Jaeksoft OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.cache;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import com.jaeksoft.searchlib.util.ReadWriteLock;

public abstract class LRUCache<K extends CacheKeyInterface<K>, V> {

	final protected ReadWriteLock rwl = new ReadWriteLock();

	private class EvictionQueue extends LinkedHashMap<K, V> {

		private static final long serialVersionUID = -2384951296369306995L;

		protected final ReentrantLock queueLock = new ReentrantLock(true);

		protected EvictionQueue(int maxSize) {
			super(maxSize);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			if (size() <= maxSize)
				return false;
			tree.remove(eldest.getKey());
			evictions++;
			return true;
		}

		final private V promote(K key) {
			queueLock.lock();
			try {
				V value = queue.remove(key);
				queue.put(key, value);
				return value;
			} finally {
				queueLock.unlock();
			}

		}
	}

	private TreeMap<K, K> tree;
	private EvictionQueue queue;

	private final String name;

	private volatile int size;
	private volatile int maxSize;
	private volatile long evictions;
	private volatile long lookups;
	private volatile long hits;
	private volatile long inserts;

	protected LRUCache(String name, int maxSize) {
		this.name = name;
		queue = (maxSize == 0) ? null : new EvictionQueue(maxSize);
		tree = new TreeMap<K, K>();
		this.maxSize = maxSize;
		this.evictions = 0;
		this.lookups = 0;
		this.inserts = 0;
		this.hits = 0;
		this.size = 0;
	}

	public void setMaxSize(int newMaxSize) {
		if (queue == null)
			return;
		rwl.w.lock();
		try {
			maxSize = newMaxSize;
			if (newMaxSize < size)
				clear();
		} finally {
			rwl.w.unlock();
		}
	}

	final protected V getAndPromote(K key) {
		if (queue == null)
			return null;
		rwl.r.lock();
		try {
			lookups++;
			K key2 = tree.get(key);
			if (key2 == null)
				return null;
			hits++;
			return queue.promote(key2);
		} finally {
			rwl.r.unlock();
		}
	}

	final public void remove(K key) {
		if (queue == null)
			return;
		rwl.w.lock();
		try {
			K key2 = tree.remove(key);
			queue.remove(key2);
			evictions++;
		} finally {
			rwl.w.unlock();
		}

	}

	final protected void put(K key, V value) {
		if (queue == null)
			return;
		rwl.w.lock();
		try {
			inserts++;
			queue.put(key, value);
			tree.put(key, key);
			size = queue.size();
		} finally {
			rwl.w.unlock();
		}
	}

	final public void clear() {
		if (queue == null)
			return;
		rwl.w.lock();
		try {
			queue.clear();
			tree.clear();
			size = queue.size();
		} finally {
			rwl.w.unlock();
		}
	}

	@Override
	final public String toString() {
		return this.getClass().getName() + " " + size + "/" + maxSize;
	}

	final public void xmlInfo(PrintWriter writer) {
		float hitRatio = getHitRatio();
		writer.println("<cache class=\"" + this.getClass().getName()
				+ "\" maxSize=\"" + this.maxSize + "\" size=\"" + size
				+ "\" hitRatio=\"" + hitRatio + "\" lookups=\"" + lookups
				+ "\" hits=\"" + hits + "\" inserts=\"" + inserts
				+ "\" evictions=\"" + evictions + "\">");
		writer.println("</cache>");
	}

	final public String getName() {
		return name;
	}

	final public int getSize() {
		return size;
	}

	final public int getMaxSize() {
		return maxSize;
	}

	final public long getEvictions() {
		return evictions;
	}

	final public long getLookups() {
		return lookups;
	}

	final public long getHits() {
		return hits;
	}

	final public long getInserts() {
		return inserts;
	}

	final public float getHitRatio() {
		if (hits > 0 && lookups > 0)
			return (float) (((float) hits) / ((float) lookups));
		else
			return 0;
	}

	final public String getHitRatioPercent() {
		return NumberFormat.getPercentInstance().format(getHitRatio());
	}

}
