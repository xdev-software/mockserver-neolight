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


public class BooleanMatcher implements Matcher<Boolean>
{
	private final Boolean matcher;
	
	BooleanMatcher(final Boolean matcher)
	{
		this.matcher = matcher;
	}
	
	@Override
	public boolean matches(final MatchDifference context, final Boolean matched)
	{
		boolean result = false;
		
		if(this.matcher == null)
		{
			result = true;
		}
		else if(matched != null)
		{
			result = matched == this.matcher;
		}
		
		if(!result && context != null)
		{
			context.addDifference("boolean match failed expected:{}found:{}", this.matcher, matched);
		}
		
		return result;
	}
	
	@Override
	public boolean isBlank()
	{
		return this.matcher == null;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final BooleanMatcher that))
		{
			return false;
		}
		return Objects.equals(this.matcher, that.matcher);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.matcher);
	}
}
