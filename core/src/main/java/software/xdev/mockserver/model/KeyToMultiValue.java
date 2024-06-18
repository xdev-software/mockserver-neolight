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
import static software.xdev.mockserver.model.NottableString.string;
import static software.xdev.mockserver.model.NottableString.strings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


public class KeyToMultiValue extends ObjectWithJsonToString
{
	private final NottableString name;
	private final List<NottableString> values;
	private Integer hashCode;
	
	KeyToMultiValue(final String name, final String... values)
	{
		this(string(name), strings(values));
	}
	
	KeyToMultiValue(final NottableString name, final String... values)
	{
		this(name, strings(values));
	}
	
	@SuppressWarnings({"UseBulkOperation", "ManualArrayToCollectionCopy"})
	KeyToMultiValue(final NottableString name, final NottableString... values)
	{
		if(name == null)
		{
			throw new IllegalArgumentException("key must not be null");
		}
		this.name = name;
		if(values == null || values.length == 0)
		{
			this.values = Collections.singletonList(string(".*"));
		}
		else if(values.length == 1)
		{
			this.values = Collections.singletonList(values[0]);
		}
		else
		{
			this.values = new LinkedList<>();
			for(final NottableString value : values)
			{
				this.values.add(value);
			}
		}
	}
	
	KeyToMultiValue(final String name, final Collection<String> values)
	{
		this(string(name), strings(values));
	}
	
	KeyToMultiValue(final NottableString name, final Collection<NottableString> values)
	{
		this.name = name;
		if(values == null || values.isEmpty())
		{
			this.values = Collections.singletonList(string(".*"));
		}
		else
		{
			this.values = new LinkedList<>(values);
		}
		this.hashCode = Objects.hash(this.name, this.values);
	}
	
	public NottableString getName()
	{
		return this.name;
	}
	
	public List<NottableString> getValues()
	{
		return this.values;
	}
	
	public void replaceValues(final List<NottableString> values)
	{
		if(this.values != values)
		{
			this.values.clear();
			this.values.addAll(values);
		}
	}
	
	public void addValue(final String value)
	{
		this.addValue(string(value));
	}
	
	private void addValue(final NottableString value)
	{
		if(this.values != null && !this.values.contains(value))
		{
			this.values.add(value);
		}
		this.hashCode = Objects.hash(this.name, this.values);
	}
	
	private void addValues(final List<String> values)
	{
		this.addNottableValues(deserializeNottableStrings(values));
	}
	
	private void addNottableValues(final List<NottableString> values)
	{
		if(this.values != null)
		{
			for(final NottableString value : values)
			{
				if(!this.values.contains(value))
				{
					this.values.add(value);
				}
			}
		}
	}
	
	public void addValues(final String... values)
	{
		this.addValues(Arrays.asList(values));
	}
	
	public void addValues(final NottableString... values)
	{
		this.addNottableValues(Arrays.asList(values));
	}
	
	@Override
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
		final KeyToMultiValue that = (KeyToMultiValue)o;
		return Objects.equals(this.name, that.name) && Objects.equals(this.values, that.values);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == null)
		{
			this.hashCode = Objects.hash(this.name, this.values);
		}
		return this.hashCode;
	}
}
