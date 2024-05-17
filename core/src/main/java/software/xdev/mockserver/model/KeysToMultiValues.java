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

import static software.xdev.mockserver.model.NottableString.deserializeNottableStrings;
import static software.xdev.mockserver.model.NottableString.serialiseNottableStrings;
import static software.xdev.mockserver.model.NottableString.string;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class KeysToMultiValues<T extends KeyToMultiValue, K extends KeysToMultiValues>
	extends ObjectWithJsonToString
{
	private KeyMatchStyle keyMatchStyle = KeyMatchStyle.SUB_SET;
	
	private final Map<NottableString, List<NottableString>> multimap;
	private final K k = (K)this;
	
	protected KeysToMultiValues()
	{
		this.multimap = new LinkedHashMap<>();
	}
	
	protected KeysToMultiValues(final Map<NottableString, List<NottableString>> multimap)
	{
		this.multimap = new LinkedHashMap<>(multimap);
	}
	
	public abstract T build(final NottableString name, final Collection<NottableString> values);
	
	protected abstract void isModified();
	
	public KeyMatchStyle getKeyMatchStyle()
	{
		return this.keyMatchStyle;
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public KeysToMultiValues<T, K> withKeyMatchStyle(final KeyMatchStyle keyMatchStyle)
	{
		this.keyMatchStyle = keyMatchStyle;
		return this;
	}
	
	public K withEntries(final Map<String, List<String>> entries)
	{
		this.isModified();
		this.multimap.clear();
		for(final String name : entries.keySet())
		{
			for(final String value : entries.get(name))
			{
				this.withEntry(name, value);
			}
		}
		return this.k;
	}
	
	public K withEntries(final List<T> entries)
	{
		this.isModified();
		this.multimap.clear();
		if(entries != null)
		{
			for(final T entry : entries)
			{
				this.withEntry(entry);
			}
		}
		return this.k;
	}
	
	@SafeVarargs
	public final K withEntries(final T... entries)
	{
		if(arrayIsNotEmpty(entries))
		{
			this.withEntries(Arrays.asList(entries));
		}
		return this.k;
	}
	
	public K withEntry(final T entry)
	{
		if(entry != null)
		{
			this.isModified();
			if(entry.getValues().isEmpty())
			{
				this.multimap.put(entry.getName(), null);
			}
			else
			{
				this.multimap.put(entry.getName(), entry.getValues());
			}
		}
		return this.k;
	}
	
	public K withEntry(final String name, final String... values)
	{
		this.isModified();
		if(values == null || values.length == 0)
		{
			this.multimap.put(string(name), new ArrayList<>(List.of(string(""))));
		}
		else
		{
			this.multimap.put(string(name), deserializeNottableStrings(values));
		}
		return this.k;
	}
	
	public K withEntry(final String name, final List<String> values)
	{
		this.isModified();
		if(values == null || values.isEmpty())
		{
			this.multimap.remove(string(name));
		}
		else
		{
			this.multimap.put(string(name), deserializeNottableStrings(values));
		}
		return this.k;
	}
	
	public K withEntry(final NottableString name, final List<NottableString> values)
	{
		if(values != null)
		{
			this.isModified();
			this.multimap.put(name, values);
		}
		return this.k;
	}
	
	public K withEntry(final NottableString name, final NottableString... values)
	{
		if(arrayIsNotEmpty(values))
		{
			this.withEntry(name, Arrays.asList(values));
		}
		return this.k;
	}
	
	public boolean remove(final String name)
	{
		boolean exists = false;
		if(name != null)
		{
			this.isModified();
			for(final NottableString key : this.multimap.keySet().toArray(new NottableString[0]))
			{
				if(key.equalsIgnoreCase(name))
				{
					this.multimap.remove(key);
					exists = true;
				}
			}
		}
		return exists;
	}
	
	public boolean remove(final NottableString name)
	{
		boolean exists = false;
		if(name != null)
		{
			this.isModified();
			for(final NottableString key : this.multimap.keySet().toArray(new NottableString[0]))
			{
				if(key.equalsIgnoreCase(name))
				{
					this.multimap.remove(key);
					exists = true;
				}
			}
		}
		return exists;
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public K replaceEntry(final T entry)
	{
		if(entry != null)
		{
			this.isModified();
			this.remove(entry.getName());
			this.multimap.put(entry.getName(), entry.getValues());
		}
		return this.k;
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public K replaceEntryIfExists(final T entry)
	{
		if(entry != null)
		{
			this.isModified();
			if(this.remove(entry.getName()))
			{
				this.multimap.put(entry.getName(), entry.getValues());
			}
		}
		return this.k;
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public K replaceEntry(final String name, final String... values)
	{
		if(arrayIsNotEmpty(values))
		{
			this.isModified();
			this.remove(name);
			this.multimap.put(string(name), deserializeNottableStrings(values));
		}
		return this.k;
	}
	
	public List<T> getEntries()
	{
		if(!this.isEmpty())
		{
			final ArrayList<T> headers = new ArrayList<>();
			for(final NottableString nottableString : this.multimap.keySet().toArray(new NottableString[0]))
			{
				headers.add(this.build(nottableString, this.multimap.get(nottableString)));
			}
			return headers;
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	public Set<NottableString> keySet()
	{
		return this.multimap.keySet();
	}
	
	public Collection<NottableString> getValues(final NottableString key)
	{
		return this.multimap.get(key);
	}
	
	public Map<NottableString, List<NottableString>> getMultimap()
	{
		return this.multimap;
	}
	
	public List<String> getValues(final String name)
	{
		if(!this.isEmpty() && name != null)
		{
			final List<String> values = new ArrayList<>();
			for(final NottableString key : this.multimap.keySet().toArray(new NottableString[0]))
			{
				if(key != null && key.equalsIgnoreCase(name))
				{
					values.addAll(serialiseNottableStrings(this.multimap.get(key)));
				}
			}
			return values;
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	String getFirstValue(final String name)
	{
		if(!this.isEmpty())
		{
			for(final NottableString key : this.multimap.keySet().toArray(new NottableString[0]))
			{
				if(key != null && key.equalsIgnoreCase(name))
				{
					final Collection<NottableString> nottableStrings = this.multimap.get(key);
					if(!nottableStrings.isEmpty())
					{
						final NottableString next = nottableStrings.iterator().next();
						if(next != null)
						{
							return next.getValue();
						}
					}
				}
			}
		}
		return "";
	}
	
	public boolean containsEntry(final String name)
	{
		if(!this.isEmpty())
		{
			for(final NottableString key : this.multimap.keySet().toArray(new NottableString[0]))
			{
				if(key != null && key.equalsIgnoreCase(name))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsEntry(final String name, final String value)
	{
		return this.containsEntry(string(name), string(value));
	}
	
	boolean containsEntry(final NottableString name, final NottableString value)
	{
		if(!this.isEmpty() && name != null && value != null)
		{
			for(final NottableString entryKey : this.multimap.keySet().toArray(new NottableString[0]))
			{
				if(entryKey != null && entryKey.equalsIgnoreCase(name))
				{
					final Collection<NottableString> nottableStrings = this.multimap.get(entryKey);
					if(nottableStrings != null)
					{
						for(final NottableString entryValue : nottableStrings.toArray(new NottableString[0]))
						{
							if(value.equalsIgnoreCase(entryValue))
							{
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean isEmpty()
	{
		return this.multimap.isEmpty();
	}
	
	@SuppressWarnings("checkstyle:NoClone")
	@Override
	public abstract K clone();
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof KeysToMultiValues))
		{
			return false;
		}
		final KeysToMultiValues<?, ?> that = (KeysToMultiValues<?, ?>)o;
		return Objects.equals(this.multimap, that.multimap);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.multimap);
	}
	
	private static boolean arrayIsNotEmpty(final Object array)
	{
		return !arrayIsEmpty(array);
	}
	
	private static boolean arrayIsEmpty(final Object array)
	{
		return array == null || Array.getLength(array) == 0;
	}
}
