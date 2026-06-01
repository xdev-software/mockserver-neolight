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

import static software.xdev.mockserver.collections.ImmutableEntry.entry;
import static software.xdev.mockserver.collections.SubSetMatcher.containsSubset;
import static software.xdev.mockserver.model.NottableString.string;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.matchers.RegexStringMatcher;
import software.xdev.mockserver.model.KeyAndValue;
import software.xdev.mockserver.model.NottableString;


public class NottableStringHashMap
{
	private final Map<NottableString, NottableString> keyValues = new LinkedHashMap<>();
	private final RegexStringMatcher regexStringMatcher;
	
	public NottableStringHashMap(final boolean controlPlaneMatcher, final List<? extends KeyAndValue> entries)
	{
		this.regexStringMatcher = new RegexStringMatcher(controlPlaneMatcher);
		for(final KeyAndValue keyToMultiValue : entries)
		{
			this.put(keyToMultiValue.getName(), keyToMultiValue.getValue());
		}
	}
	
	public boolean containsAll(final MatchDifference context, final NottableStringHashMap subset)
	{
		return containsSubset(context, this.regexStringMatcher, subset.entryList(), this.entryList());
	}
	
	public boolean allKeysNotted()
	{
		for(final NottableString key : this.keyValues.keySet())
		{
			if(!key.isNot())
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean allKeysOptional()
	{
		for(final NottableString key : this.keyValues.keySet())
		{
			if(!key.isOptional())
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean isEmpty()
	{
		return this.keyValues.isEmpty();
	}
	
	private void put(final NottableString key, final NottableString value)
	{
		this.keyValues.put(key, value != null ? value : string(""));
	}
	
	private List<ImmutableEntry> entryList()
	{
		if(!this.keyValues.isEmpty())
		{
			final List<ImmutableEntry> entries = new ArrayList<>();
			for(final Map.Entry<NottableString, NottableString> entry : this.keyValues.entrySet())
			{
				entries.add(entry(this.regexStringMatcher, entry.getKey(), entry.getValue()));
			}
			return entries;
		}
		else
		{
			return Collections.emptyList();
		}
	}
}
