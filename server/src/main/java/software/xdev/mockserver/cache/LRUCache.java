/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.mockserver.cache;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


@SuppressWarnings("unused")
public class LRUCache<K, V>
{
	private static boolean allCachesEnabled = true;
	private static int maxSizeOverride;
	
	private static final Set<LRUCache<?, ?>> ALL_CACHES =
		Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	
	private final long ttlInMillis;
	private final int maxSize;
	private final ConcurrentHashMap<K, Entry<V>> map;
	private final ConcurrentLinkedQueue<K> queue;
	
	public LRUCache(final int maxSize, final long ttlInMillis)
	{
		this.maxSize = maxSize;
		this.map = new ConcurrentHashMap<>(maxSize);
		this.queue = new ConcurrentLinkedQueue<>();
		this.ttlInMillis = ttlInMillis;
		LRUCache.ALL_CACHES.add(this);
	}
	
	public static void allCachesEnabled(final boolean enabled)
	{
		allCachesEnabled = enabled;
	}
	
	public static void clearAllCaches()
	{
		// using synchronized foreach instead of a for-loop
		ALL_CACHES.forEach(cache -> {
			if(cache != null)
			{
				cache.clear();
			}
		});
	}
	
	public void put(final K key, final V value)
	{
		this.put(key, value, this.ttlInMillis);
	}
	
	public void put(final K key, final V value, final long ttl)
	{
		if(allCachesEnabled && key != null)
		{
			if(this.map.containsKey(key))
			{
				// ensure the queue is in FIFO order
				this.queue.remove(key);
			}
			while(this.queue.size() >= this.maxSize || maxSizeOverride > 0 && this.queue.size() >= maxSizeOverride)
			{
				final K oldestKey = this.queue.poll();
				if(null != oldestKey)
				{
					this.map.remove(oldestKey);
				}
			}
			this.queue.add(key);
			this.map.put(key, new Entry<>(ttl, this.expiryInMillis(ttl), value));
		}
	}
	
	private long expiryInMillis(final long ttl)
	{
		return System.currentTimeMillis() + ttl;
	}
	
	public V get(final K key)
	{
		if(allCachesEnabled && key != null)
		{
			if(this.map.containsKey(key))
			{
				// remove from the queue and add it again in FIFO queue
				this.queue.remove(key);
				this.queue.add(key);
			}
			
			final Entry<V> entry = this.map.get(key);
			if(entry != null)
			{
				if(entry.getExpiryInMillis() > System.currentTimeMillis())
				{
					return entry.updateExpiryInMillis(this.expiryInMillis(entry.getTtlInMillis())).getValue();
				}
				else
				{
					this.delete(key);
				}
			}
		}
		return null;
	}
	
	public void delete(final K key)
	{
		if(allCachesEnabled && key != null && this.map.containsKey(key))
		{
			this.map.remove(key);
			this.queue.remove(key);
		}
	}
	
	private void clear()
	{
		this.map.clear();
		this.queue.clear();
	}
	
	public static void setMaxSizeOverride(final int maxSizeOverride)
	{
		LRUCache.maxSizeOverride = maxSizeOverride;
	}
}
