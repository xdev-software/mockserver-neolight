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

import static software.xdev.mockserver.model.NottableString.string;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class KeysAndValues<T extends KeyAndValue, K extends KeysAndValues> extends ObjectWithJsonToString
{
	private final Map<NottableString, NottableString> map;
	
	protected KeysAndValues()
	{
		this.map = new LinkedHashMap<>();
	}
	
	protected KeysAndValues(final Map<NottableString, NottableString> map)
	{
		this.map = new LinkedHashMap<>(map);
	}
	
	public abstract T build(NottableString name, NottableString value);
	
	public K withEntries(final List<T> entries)
	{
		this.map.clear();
		if(entries != null)
		{
			for(final T cookie : entries)
			{
				this.withEntry(cookie);
			}
		}
		return (K)this;
	}
	
	public K withEntries(final T... entries)
	{
		if(entries != null)
		{
			this.withEntries(Arrays.asList(entries));
		}
		return (K)this;
	}
	
	public K withEntry(final T entry)
	{
		if(entry != null)
		{
			this.map.put(entry.getName(), entry.getValue());
		}
		return (K)this;
	}
	
	public K withEntry(final String name, final String value)
	{
		this.map.put(string(name), string(value));
		return (K)this;
	}
	
	public K withEntry(final NottableString name, final NottableString value)
	{
		this.map.put(name, value);
		return (K)this;
	}
	
	public K replaceEntryIfExists(final T entry)
	{
		if(entry != null)
		{
			if(this.remove(entry.getName()))
			{
				this.map.put(entry.getName(), entry.getValue());
			}
		}
		return (K)this;
	}
	
	public List<T> getEntries()
	{
		if(!this.map.isEmpty())
		{
			final ArrayList<T> cookies = new ArrayList<>();
			for(final NottableString nottableString : this.map.keySet())
			{
				cookies.add(this.build(nottableString, this.map.get(nottableString)));
			}
			return cookies;
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	public Map<NottableString, NottableString> getMap()
	{
		return this.map;
	}
	
	public boolean isEmpty()
	{
		return this.map.isEmpty();
	}
	
	public boolean remove(final NottableString name)
	{
		return this.remove(name.getValue());
	}
	
	public boolean remove(final String name)
	{
		if(isNotBlank(name))
		{
			return this.map.remove(string(name)) != null;
		}
		return false;
	}
	
	@Override
	public abstract K clone();
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final KeysAndValues<?, ?> that))
		{
			return false;
		}
		return Objects.equals(this.getMap(), that.getMap());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.getMap());
	}
}
