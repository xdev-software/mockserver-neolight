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

import software.xdev.mockserver.collections.NottableStringMultiMap;
import software.xdev.mockserver.model.KeyMatchStyle;
import software.xdev.mockserver.model.KeyToMultiValue;
import software.xdev.mockserver.model.KeysToMultiValues;


@SuppressWarnings("rawtypes")
public class MultiValueMapMatcher
	extends NotMatcher<KeysToMultiValues<? extends KeyToMultiValue, ? extends KeysToMultiValues>>
{
	private final NottableStringMultiMap matcher;
	private final KeysToMultiValues keysToMultiValues;
	private final boolean controlPlaneMatcher;
	private Boolean allKeysNotted;
	private Boolean allKeysOptional;
	
	MultiValueMapMatcher(
		final KeysToMultiValues<? extends KeyToMultiValue, ? extends KeysToMultiValues> keysToMultiValues,
		final boolean controlPlaneMatcher)
	{
		this.keysToMultiValues = keysToMultiValues;
		this.controlPlaneMatcher = controlPlaneMatcher;
		if(keysToMultiValues != null)
		{
			this.matcher = new NottableStringMultiMap(this.controlPlaneMatcher,
				keysToMultiValues.getKeyMatchStyle(),
				keysToMultiValues.getEntries());
		}
		else
		{
			this.matcher = null;
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public boolean matches(
		final MatchDifference context,
		final KeysToMultiValues<? extends KeyToMultiValue, ? extends KeysToMultiValues> matched)
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
			result = new NottableStringMultiMap(
				this.controlPlaneMatcher,
				matched.getKeyMatchStyle(),
				matched.getEntries()).containsAll(context, this.matcher);
		}
		
		if(!result && context != null)
		{
			context.addDifference(
				"multimap match failed expected:{}found:{}failed because:{}",
				this.keysToMultiValues,
				matched != null ? matched : "none",
				matched != null
					? (this.matcher.getKeyMatchStyle() == KeyMatchStyle.SUB_SET
					? "multimap is not a subset"
					: "multimap values don't match")
					: "none is not a subset");
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
		if(!(o instanceof final MultiValueMapMatcher that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return this.controlPlaneMatcher == that.controlPlaneMatcher
			&& Objects.equals(this.matcher, that.matcher)
			&& Objects.equals(this.keysToMultiValues, that.keysToMultiValues)
			&& Objects.equals(this.allKeysNotted, that.allKeysNotted)
			&& Objects.equals(this.allKeysOptional, that.allKeysOptional);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			super.hashCode(),
			this.matcher,
			this.keysToMultiValues,
			this.controlPlaneMatcher,
			this.allKeysNotted,
			this.allKeysOptional);
	}
}
