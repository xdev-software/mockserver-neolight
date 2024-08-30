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


public abstract class NotMatcher<MatchedType> implements Matcher<MatchedType>
{
	boolean not;
	
	public static <MatcherType extends NotMatcher<?>> MatcherType notMatcher(final MatcherType matcher)
	{
		matcher.not = true;
		return matcher;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final NotMatcher<?> that))
		{
			return false;
		}
		return this.not == that.not;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.not);
	}
}
