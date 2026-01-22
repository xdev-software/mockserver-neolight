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

import static software.xdev.mockserver.model.NottableString.string;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.matchers.RegexStringMatcher;
import software.xdev.mockserver.model.NottableString;


public final class SubSetMatcher
{
	static boolean containsSubset(
		final MatchDifference context,
		final RegexStringMatcher regexStringMatcher,
		final List<ImmutableEntry> subset,
		final List<ImmutableEntry> superset)
	{
		boolean result = true;
		final Set<Integer> matchingIndexes = new HashSet<>();
		for(final ImmutableEntry subsetItem : subset)
		{
			final Set<Integer> subsetItemMatchingIndexes =
				matchesIndexes(context, regexStringMatcher, subsetItem, superset);
			final boolean optionalAndNotPresent =
				subsetItem.isOptional() && !containsKey(regexStringMatcher, subsetItem, superset);
			final boolean nottedAndPresent = nottedAndPresent(regexStringMatcher, subsetItem, superset);
			if(!optionalAndNotPresent && subsetItemMatchingIndexes.isEmpty() || nottedAndPresent)
			{
				result = false;
				break;
			}
			matchingIndexes.addAll(subsetItemMatchingIndexes);
		}
		
		if(result)
		{
			final long subsetNonOptionalSize = subset.stream().filter(ImmutableEntry::isNotOptional).count();
			// this prevents multiple items in the subset from being matched by a single item in the superset
			result = matchingIndexes.size() >= subsetNonOptionalSize;
		}
		return result;
	}
	
	private static Set<Integer> matchesIndexes(
		final MatchDifference context,
		final RegexStringMatcher regexStringMatcher,
		final ImmutableEntry matcherItem,
		final List<ImmutableEntry> matches)
	{
		final Set<Integer> matchingIndexes = new HashSet<>();
		for(int i = 0; i < matches.size(); i++)
		{
			final ImmutableEntry matchedItem = matches.get(i);
			final boolean keyMatches = regexStringMatcher.matches(context, matcherItem.getKey(), matchedItem.getKey());
			final boolean valueMatches =
				regexStringMatcher.matches(context, matcherItem.getValue(), matchedItem.getValue());
			if(keyMatches && valueMatches)
			{
				matchingIndexes.add(i);
			}
		}
		return matchingIndexes;
	}
	
	private static boolean containsKey(
		final RegexStringMatcher regexStringMatcher,
		final ImmutableEntry matcherItem,
		final List<ImmutableEntry> matches)
	{
		for(final ImmutableEntry matchedItem : matches)
		{
			if(regexStringMatcher.matches(matcherItem.getKey(), matchedItem.getKey()))
			{
				return true;
			}
		}
		return false;
	}
	
	private static boolean nottedAndPresent(
		final RegexStringMatcher regexStringMatcher,
		final ImmutableEntry matcherItem,
		final List<ImmutableEntry> matches)
	{
		if(matcherItem.getKey().isNot())
		{
			final NottableString unNottedMatcherItemKey = string(matcherItem.getKey().getValue());
			for(final ImmutableEntry matchedItem : matches)
			{
				if(!matchedItem.getKey().isNot() && regexStringMatcher.matches(
					unNottedMatcherItemKey,
					matchedItem.getKey()))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private SubSetMatcher()
	{
	}
}
