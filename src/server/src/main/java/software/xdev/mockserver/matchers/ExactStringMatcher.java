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

import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.util.StringUtils;


public class ExactStringMatcher extends BodyMatcher<NottableString>
{
	private final NottableString matcher;
	
	ExactStringMatcher(final NottableString matcher)
	{
		this.matcher = matcher;
	}
	
	public static boolean matches(final String matcher, final String matched, final boolean ignoreCase)
	{
		if(StringUtils.isBlank(matcher))
		{
			return true;
		}
		else if(matched != null)
		{
			if(matched.equals(matcher))
			{
				return true;
			}
			// case-insensitive comparison is mainly to improve matching in web containers like Tomcat that convert
			// header names to lower case
			if(ignoreCase)
			{
				return matched.equalsIgnoreCase(matcher);
			}
		}
		
		return false;
	}
	
	@Override
	public boolean matches(final MatchDifference context, final NottableString matched)
	{
		if(this.matcher == null)
		{
			return true;
		}
		
		final boolean result = matched != null && matches(this.matcher.getValue(), matched.getValue(), false);
		
		if(!result && context != null)
		{
			context.addDifference("exact string match failed expected:{}found:{}", this.matcher, matched);
		}
		
		if(matched == null)
		{
			return false;
		}
		
		return matched.isNot() == (this.matcher.isNot() == (this.not != result));
	}
	
	@Override
	public boolean isBlank()
	{
		return this.matcher == null || StringUtils.isBlank(this.matcher.getValue());
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final ExactStringMatcher that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.matcher, that.matcher);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.matcher);
	}
}
