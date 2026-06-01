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
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.util.StringUtils;


public class RegexStringMatcher extends BodyMatcher<NottableString>
{
	private static final Logger LOG = LoggerFactory.getLogger(RegexStringMatcher.class);
	
	private final NottableString matcher;
	private final boolean controlPlaneMatcher;
	
	public RegexStringMatcher(final boolean controlPlaneMatcher)
	{
		this.controlPlaneMatcher = controlPlaneMatcher;
		this.matcher = null;
	}
	
	RegexStringMatcher(final NottableString matcher, final boolean controlPlaneMatcher)
	{
		this.controlPlaneMatcher = controlPlaneMatcher;
		this.matcher = matcher;
	}
	
	@Override
	public boolean matches(final MatchDifference context, final NottableString matched)
	{
		final boolean result = this.matcher == null || this.matches(context, this.matcher, matched);
		return this.not != result;
	}
	
	public boolean matches(final NottableString matcher, final NottableString matched)
	{
		return this.matches(null, matcher, matched);
	}
	
	public boolean matches(final MatchDifference context, final NottableString matcher, final NottableString matched)
	{
		return this.matchesByNottedStrings(context, matcher, matched);
	}
	
	private boolean matchesByNottedStrings(
		final MatchDifference context,
		final NottableString matcher,
		final NottableString matched)
	{
		if(matcher.isNot() && matched.isNot())
		{
			// mutual notted control plane match
			return this.matchesByStrings(context, matcher, matched);
		}
		else
		{
			// data plane & control plan match
			return (matcher.isNot() || matched.isNot()) ^ this.matchesByStrings(context, matcher, matched);
		}
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity"})
	private boolean matchesByStrings(
		final MatchDifference context,
		final NottableString matcher,
		final NottableString matched)
	{
		if(matcher == null)
		{
			return true;
		}
		final String matcherValue = matcher.getValue();
		if(StringUtils.isBlank(matcherValue))
		{
			return true;
		}
		else
		{
			if(matched != null)
			{
				final String matchedValue = matched.getValue();
				if(matchedValue != null)
				{
					// match as exact string
					if(matchedValue.equals(matcherValue) || matchedValue.equalsIgnoreCase(matcherValue))
					{
						return true;
					}
					
					// match as regex - matcher -> matched (data plane or control plane)
					try
					{
						if(matcher.matches(matchedValue))
						{
							return true;
						}
					}
					catch(final PatternSyntaxException pse)
					{
						if(LOG.isDebugEnabled())
						{
							LOG.debug("Error while matching regex [{}] for string [{}]", matcher, matched, pse);
						}
					}
					// match as regex - matched -> matcher (control plane only)
					try
					{
						if(this.controlPlaneMatcher && matched.matches(matcherValue))
						{
							return true;
						}
						else if(LOG.isDebugEnabled() && matched.matches(matcherValue))
						{
							LOG.debug(
								"Matcher {} would match {} if matcher was used for control plane",
								matcher,
								matched);
						}
					}
					catch(final PatternSyntaxException pse)
					{
						if(this.controlPlaneMatcher
							&& LOG.isDebugEnabled())
						{
							LOG.debug("Error while matching regex [{}] for string [{}]", matcher, matched, pse);
						}
					}
				}
			}
		}
		if(context != null)
		{
			context.addDifference("string or regex match failed expected:{}found:{}", matcher, matched);
		}
		
		return false;
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
		if(!(o instanceof final RegexStringMatcher that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return this.controlPlaneMatcher == that.controlPlaneMatcher && Objects.equals(this.matcher, that.matcher);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.matcher, this.controlPlaneMatcher);
	}
}
