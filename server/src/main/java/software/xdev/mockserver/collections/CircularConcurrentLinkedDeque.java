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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;


public class CircularConcurrentLinkedDeque<E> extends ConcurrentLinkedDeque<E>
{
	private int maxSize;
	private final Consumer<E> onEvictCallback;
	
	public CircularConcurrentLinkedDeque(final int maxSize, final Consumer<E> onEvictCallback)
	{
		this.maxSize = maxSize;
		this.onEvictCallback = onEvictCallback;
	}
	
	public void setMaxSize(final int maxSize)
	{
		this.maxSize = maxSize;
	}
	
	@Override
	public boolean add(final E element)
	{
		if(this.maxSize > 0)
		{
			this.evictExcessElements();
			return super.add(element);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean addAll(final Collection<? extends E> collection)
	{
		if(this.maxSize > 0)
		{
			boolean result = false;
			for(final E element : collection)
			{
				if(this.add(element))
				{
					result = true;
				}
			}
			return result;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean offer(final E element)
	{
		if(this.maxSize > 0)
		{
			this.evictExcessElements();
			return super.offer(element);
		}
		else
		{
			return false;
		}
	}
	
	private void evictExcessElements()
	{
		if(this.onEvictCallback == null)
		{
			while(this.size() >= this.maxSize)
			{
				super.poll();
			}
		}
		else
		{
			while(this.size() >= this.maxSize)
			{
				this.onEvictCallback.accept(super.poll());
			}
		}
	}
	
	@Override
	public void clear()
	{
		if(this.onEvictCallback == null)
		{
			super.clear();
		}
		else
		{
			while(!this.isEmpty())
			{
				this.onEvictCallback.accept(super.poll());
			}
		}
	}
	
	/**
	 * @deprecated use removeItem instead
	 */
	@Override
	@Deprecated
	public boolean remove(final Object o)
	{
		return super.remove(o);
	}
	
	public boolean removeItem(final E e)
	{
		if(this.onEvictCallback != null)
		{
			this.onEvictCallback.accept(e);
		}
		return super.remove(e);
	}
}
