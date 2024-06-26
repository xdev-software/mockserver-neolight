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

import software.xdev.mockserver.codec.ExpandedParameterDecoder;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.model.Parameters;


public class ParameterStringMatcher extends BodyMatcher<String>
{
	private final MultiValueMapMatcher matcher;
	private final ExpandedParameterDecoder formParameterParser;
	private final Parameters matcherParameters;
	private final ExpandedParameterDecoder expandedParameterDecoder;
	
	ParameterStringMatcher(
		final ServerConfiguration configuration, final Parameters matcherParameters,
		final boolean controlPlaneMatcher)
	{
		this.matcherParameters = matcherParameters;
		this.matcher = new MultiValueMapMatcher(matcherParameters, controlPlaneMatcher);
		this.formParameterParser = new ExpandedParameterDecoder(configuration);
		this.expandedParameterDecoder = new ExpandedParameterDecoder(configuration);
	}
	
	@Override
	public boolean matches(final MatchDifference context, final String matched)
	{
		boolean result = false;
		
		final Parameters matchedParameters =
			this.formParameterParser.retrieveFormParameters(matched, matched != null && matched.contains("?"));
		this.expandedParameterDecoder.splitParameters(this.matcherParameters, matchedParameters);
		if(this.matcher.matches(context, matchedParameters))
		{
			result = true;
		}
		
		return this.not != result;
	}
	
	@Override
	public boolean isBlank()
	{
		return this.matcher.isBlank();
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final ParameterStringMatcher that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.matcher, that.matcher)
			&& Objects.equals(this.formParameterParser, that.formParameterParser)
			&& Objects.equals(this.matcherParameters, that.matcherParameters)
			&& Objects.equals(this.expandedParameterDecoder, that.expandedParameterDecoder);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			super.hashCode(),
			this.matcher,
			this.formParameterParser,
			this.matcherParameters,
			this.expandedParameterDecoder);
	}
}
