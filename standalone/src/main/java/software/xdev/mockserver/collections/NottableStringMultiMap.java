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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.matchers.RegexStringMatcher;
import software.xdev.mockserver.model.KeyMatchStyle;
import software.xdev.mockserver.model.KeyToMultiValue;
import software.xdev.mockserver.model.NottableString;


public class NottableStringMultiMap
{
	private final Map<NottableString, List<NottableString>> backingMap = new LinkedHashMap<>();
	private final RegexStringMatcher regexStringMatcher;
	private final KeyMatchStyle keyMatchStyle;
	
	public NottableStringMultiMap(
		final boolean controlPlaneMatcher,
		final KeyMatchStyle keyMatchStyle,
		final List<? extends KeyToMultiValue> entries)
	{
		this.keyMatchStyle = keyMatchStyle;
		this.regexStringMatcher = new RegexStringMatcher(controlPlaneMatcher);
		for(final KeyToMultiValue keyToMultiValue : entries)
		{
			this.backingMap.put(keyToMultiValue.getName(), keyToMultiValue.getValues());
		}
	}
	
	public NottableStringMultiMap(
		final boolean controlPlaneMatcher,
		final KeyMatchStyle keyMatchStyle,
		final NottableString[]... keyAndValues)
	{
		this.keyMatchStyle = keyMatchStyle;
		this.regexStringMatcher = new RegexStringMatcher(controlPlaneMatcher);
		for(final NottableString[] keyAndValue : keyAndValues)
		{
			if(keyAndValue.length > 0)
			{
				this.backingMap.put(
					keyAndValue[0],
					keyAndValue.length > 1
						? Arrays.asList(keyAndValue).subList(1, keyAndValue.length)
						: Collections.emptyList());
			}
		}
	}
	
	public KeyMatchStyle getKeyMatchStyle()
	{
		return this.keyMatchStyle;
	}
	
	public boolean containsAll(final MatchDifference context, final NottableStringMultiMap subset)
	{
		switch(subset.keyMatchStyle)
		{
			case SUB_SET:
			{
				final boolean isSubset = containsSubset(context,
					this.regexStringMatcher, subset.entryList(), this.entryList());
				if(!isSubset && context != null)
				{
					context.addDifference(
						"multimap subset match failed subset:{}was not a subset of:{}",
						subset.entryList(),
						this.entryList());
				}
				return isSubset;
			}
			case MATCHING_KEY:
			{
				for(final NottableString matcherKey : subset.backingMap.keySet())
				{
					final List<NottableString> matchedValuesForKey = this.getAll(matcherKey);
					if(matchedValuesForKey.isEmpty() && !matcherKey.isOptional())
					{
						if(context != null)
						{
							context.addDifference(
								"multimap subset match failed subset:{}did not have expected key:{}",
								subset,
								matcherKey);
						}
						return false;
					}
					
					final List<NottableString> matcherValuesForKey = subset.getAll(matcherKey);
					for(final NottableString matchedValue : matchedValuesForKey)
					{
						boolean matchesValue = false;
						for(final NottableString matcherValue : matcherValuesForKey)
						{
							// match item by item
							if(this.regexStringMatcher.matches(context, matcherValue, matchedValue))
							{
								matchesValue = true;
								break;
							}
							else
							{
								if(context != null)
								{
									context.addDifference("multimap matching key match failed for key:{}", matcherKey);
								}
							}
						}
						if(!matchesValue)
						{
							return false;
						}
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public boolean allKeysNotted()
	{
		if(!this.isEmpty())
		{
			for(final NottableString key : this.backingMap.keySet())
			{
				if(!key.isNot())
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean allKeysOptional()
	{
		if(!this.isEmpty())
		{
			for(final NottableString key : this.backingMap.keySet())
			{
				if(!key.isOptional())
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean isEmpty()
	{
		return this.backingMap.isEmpty();
	}
	
	private List<NottableString> getAll(final NottableString key)
	{
		if(!this.isEmpty())
		{
			final List<NottableString> values = new ArrayList<>();
			for(final Map.Entry<NottableString, List<NottableString>> entry : this.backingMap.entrySet())
			{
				if(this.regexStringMatcher.matches(key, entry.getKey()))
				{
					values.addAll(entry.getValue());
				}
			}
			return values;
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	private List<ImmutableEntry> entryList()
	{
		if(!this.isEmpty())
		{
			final List<ImmutableEntry> entrySet = new ArrayList<>();
			for(final Map.Entry<NottableString, List<NottableString>> entry : this.backingMap.entrySet())
			{
				for(final NottableString value : entry.getValue())
				{
					entrySet.add(entry(this.regexStringMatcher, entry.getKey(), value));
				}
			}
			return entrySet;
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final NottableStringMultiMap that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.backingMap, that.backingMap)
			&& Objects.equals(this.regexStringMatcher, that.regexStringMatcher)
			&& this.getKeyMatchStyle() == that.getKeyMatchStyle();
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.backingMap, this.regexStringMatcher, this.getKeyMatchStyle());
	}
}



