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
package software.xdev.mockserver.matchers;

import java.util.Objects;

import software.xdev.mockserver.collections.NottableStringHashMap;
import software.xdev.mockserver.model.KeyAndValue;
import software.xdev.mockserver.model.KeysAndValues;


@SuppressWarnings("rawtypes")
public class HashMapMatcher extends NotMatcher<KeysAndValues<? extends KeyAndValue, ? extends KeysAndValues>>
{
	private final NottableStringHashMap matcher;
	private final KeysAndValues keysAndValues;
	private final boolean controlPlaneMatcher;
	private Boolean allKeysNotted;
	private Boolean allKeysOptional;
	
	HashMapMatcher(
		final KeysAndValues<? extends KeyAndValue, ? extends KeysAndValues> keysAndValues,
		final boolean controlPlaneMatcher)
	{
		this.keysAndValues = keysAndValues;
		this.controlPlaneMatcher = controlPlaneMatcher;
		if(keysAndValues != null)
		{
			this.matcher = new NottableStringHashMap(this.controlPlaneMatcher, keysAndValues.getEntries());
		}
		else
		{
			this.matcher = null;
		}
	}
	
	@Override
	public boolean matches(
		final MatchDifference context,
		final KeysAndValues<? extends KeyAndValue, ? extends KeysAndValues> matched)
	{
		final boolean result;
		
		if(this.matcher == null || this.matcher.isEmpty())
		{
			result = true;
		}
		else if(matched == null || matched.isEmpty())
		{
			if(this.allKeysNotted == null)
			{
				this.allKeysNotted = this.matcher.allKeysNotted();
			}
			if(this.allKeysOptional == null)
			{
				this.allKeysOptional = this.matcher.allKeysOptional();
			}
			result = this.allKeysNotted || this.allKeysOptional;
		}
		else
		{
			result = new NottableStringHashMap(this.controlPlaneMatcher, matched.getEntries()).containsAll(
				context,
				this.matcher);
		}
		
		if(!result && context != null)
		{
			context.addDifference(
				"map subset match failed expected:{}found:{}failed because:{}",
				this.keysAndValues,
				matched != null ? matched : "none",
				matched != null ? "map is not a subset" : "none is not a subset");
		}
		
		return this.not != result;
	}
	
	@Override
	public boolean isBlank()
	{
		return this.matcher == null || this.matcher.isEmpty();
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final HashMapMatcher that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return this.controlPlaneMatcher == that.controlPlaneMatcher && Objects.equals(this.matcher, that.matcher)
			&& Objects.equals(this.keysAndValues, that.keysAndValues) && Objects.equals(
			this.allKeysNotted,
			that.allKeysNotted) && Objects.equals(this.allKeysOptional, that.allKeysOptional);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			super.hashCode(),
			this.matcher,
			this.keysAndValues,
			this.controlPlaneMatcher,
			this.allKeysNotted,
			this.allKeysOptional);
	}
}
