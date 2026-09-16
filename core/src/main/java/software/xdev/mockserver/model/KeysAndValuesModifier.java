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
package software.xdev.mockserver.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@SuppressWarnings("unchecked")
public abstract class KeysAndValuesModifier<T extends KeysAndValues<I, T>, K extends KeysAndValuesModifier<T, K, I>,
	I extends KeyAndValue>
{
	private int hashCode;
	private T add;
	private T replace;
	private List<String> remove;
	
	abstract T construct(List<I> list);
	
	abstract T construct(I... array);
	
	public T getAdd()
	{
		return this.add;
	}
	
	public K withAdd(final T add)
	{
		this.add = add;
		this.hashCode = 0;
		return (K)this;
	}
	
	public K add(final List<I> add)
	{
		return this.withAdd(this.construct(add));
	}
	
	public K add(final I... add)
	{
		return this.withAdd(this.construct(add));
	}
	
	public T getReplace()
	{
		return this.replace;
	}
	
	public K withReplace(final T replace)
	{
		this.replace = replace;
		this.hashCode = 0;
		return (K)this;
	}
	
	public K replace(final List<I> replace)
	{
		return this.withReplace(this.construct(replace));
	}
	
	public K replace(final I... replace)
	{
		return this.withReplace(this.construct(replace));
	}
	
	public List<String> getRemove()
	{
		return this.remove;
	}
	
	public K withRemove(final List<String> remove)
	{
		this.remove = remove;
		this.hashCode = 0;
		return (K)this;
	}
	
	public K remove(final List<String> remove)
	{
		return this.withRemove(remove);
	}
	
	public K remove(final String... remove)
	{
		return this.withRemove(Arrays.asList(remove));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		if(this.hashCode() != o.hashCode())
		{
			return false;
		}
		final KeysAndValuesModifier<T, K, I> that = (KeysAndValuesModifier<T, K, I>)o;
		return Objects.equals(this.add, that.add)
			&& Objects.equals(this.replace, that.replace)
			&& Objects.equals(this.remove, that.remove);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(this.add, this.replace, this.remove);
		}
		return this.hashCode;
	}
	
	public T update(final T keysAndValues)
	{
		if(this.replace != null && this.replace.getEntries() != null && keysAndValues != null)
		{
			this.replace.getEntries().forEach(keysAndValues::replaceEntryIfExists);
		}
		if(this.add != null && this.add.getEntries() != null)
		{
			if(keysAndValues != null)
			{
				this.add.getEntries().forEach(keysAndValues::withEntry);
			}
			else
			{
				return this.add.clone();
			}
		}
		if(this.remove != null && keysAndValues != null)
		{
			this.remove.forEach(keysAndValues::remove);
		}
		return keysAndValues;
	}
}
