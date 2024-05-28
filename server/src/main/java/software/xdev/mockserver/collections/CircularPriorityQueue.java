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
package software.xdev.mockserver.collections;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CircularPriorityQueue<K, V, SLK extends Keyed<K>>
{
	private int maxSize;
	private final Function<V, SLK> skipListKeyFunction;
	private final Function<V, K> mapKeyFunction;
	private final ConcurrentSkipListSet<SLK> sortOrderSkipList;
	private final ConcurrentLinkedQueue<V> insertionOrderQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentMap<K, V> byKey = new ConcurrentHashMap<>();
	
	public CircularPriorityQueue(
		final int maxSize,
		final Comparator<? super SLK> skipListComparator,
		final Function<V, SLK> skipListKeyFunction,
		final Function<V, K> mapKeyFunction)
	{
		this.sortOrderSkipList = new ConcurrentSkipListSet<>(skipListComparator);
		this.maxSize = maxSize;
		this.skipListKeyFunction = skipListKeyFunction;
		this.mapKeyFunction = mapKeyFunction;
	}
	
	public void setMaxSize(final int maxSize)
	{
		this.maxSize = maxSize;
	}
	
	public void removePriorityKey(final V element)
	{
		this.sortOrderSkipList.remove(this.skipListKeyFunction.apply(element));
	}
	
	public void addPriorityKey(final V element)
	{
		this.sortOrderSkipList.add(this.skipListKeyFunction.apply(element));
	}
	
	public void add(final V element)
	{
		if(this.maxSize > 0 && element != null)
		{
			this.insertionOrderQueue.offer(element);
			this.sortOrderSkipList.add(this.skipListKeyFunction.apply(element));
			this.byKey.put(this.mapKeyFunction.apply(element), element);
			while(this.insertionOrderQueue.size() > this.maxSize)
			{
				final V elementToRemove = this.insertionOrderQueue.poll();
				this.sortOrderSkipList.remove(this.skipListKeyFunction.apply(elementToRemove));
				this.byKey.remove(this.mapKeyFunction.apply(elementToRemove));
			}
		}
	}
	
	public boolean remove(final V element)
	{
		if(element != null)
		{
			this.insertionOrderQueue.remove(element);
			this.byKey.remove(this.mapKeyFunction.apply(element));
			return this.sortOrderSkipList.remove(this.skipListKeyFunction.apply(element));
		}
		else
		{
			return false;
		}
	}
	
	public int size()
	{
		return this.insertionOrderQueue.size();
	}
	
	public Stream<V> stream()
	{
		return this.sortOrderSkipList.stream().map(item -> this.byKey.get(item.getKey())).filter(Objects::nonNull);
	}
	
	public Optional<V> getByKey(final K key)
	{
		if(key != null && !"".equals(key))
		{
			return Optional.ofNullable(this.byKey.get(key));
		}
		else
		{
			return Optional.empty();
		}
	}
	
	public Map<K, V> keyMap()
	{
		return new HashMap<>(this.byKey);
	}
	
	public boolean isEmpty()
	{
		return this.insertionOrderQueue.isEmpty();
	}
	
	public List<V> toSortedList()
	{
		return this.stream().collect(Collectors.toList());
	}
}
