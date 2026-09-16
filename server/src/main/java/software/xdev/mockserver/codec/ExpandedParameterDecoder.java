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
package software.xdev.mockserver.codec;

import static software.xdev.mockserver.model.NottableOptionalString.optional;
import static software.xdev.mockserver.model.NottableString.string;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.Parameter;
import software.xdev.mockserver.model.ParameterStyle;
import software.xdev.mockserver.model.Parameters;
import software.xdev.mockserver.util.StringUtils;


public class ExpandedParameterDecoder
{
	private static final Logger LOG = LoggerFactory.getLogger(ExpandedParameterDecoder.class);
	
	private static final Predicate<String> QUOTED_PARAMETER_VALUE_PRE_CHECK =
		s -> s.contains("\"") || s.contains("'");
	@SuppressWarnings("java:S5852")
	private static final Pattern QUOTED_PARAMETER_VALUE = Pattern.compile("^\\s*[\"']+(.*)[\"']+\\s*$");
	private static final Predicate<String> JSON_VALUE_PRE_CHECK =
		s -> s.contains("{") || s.contains("[");
	@SuppressWarnings("java:S5852")
	private static final Pattern JSON_VALUE = Pattern.compile("(?s)^\\s*[{\\[].*[}\\]]\\s*$");
	
	private final ServerConfiguration configuration;
	
	public ExpandedParameterDecoder(final ServerConfiguration configuration)
	{
		this.configuration = configuration;
	}
	
	@SuppressWarnings("checkstyle:FinalParameters")
	public Parameters retrieveFormParameters(final String parameterString, boolean hasPath)
	{
		final Parameters parameters = new Parameters();
		final Map<String, List<String>> parameterValues = new HashMap<>();
		if(isNotBlank(parameterString))
		{
			try
			{
				hasPath = parameterString.startsWith("/") || parameterString.contains("?") || hasPath;
				parameterValues.putAll(new QueryStringDecoder(
					parameterString,
					HttpConstants.DEFAULT_CHARSET,
					hasPath,
					Integer.MAX_VALUE,
					!this.configuration.useSemicolonAsQueryParameterSeparator()).parameters());
			}
			catch(final IllegalArgumentException iae)
			{
				LOG.error("Exception while parsing query string {}", parameterString, iae);
			}
		}
		return parameters.withEntries(parameterValues);
	}
	
	@SuppressWarnings("checkstyle:FinalParameters")
	public Parameters retrieveQueryParameters(final String parameterString, boolean hasPath)
	{
		if(isNotBlank(parameterString))
		{
			final String rawParameterString =
				parameterString.contains("?") ? StringUtils.substringAfter(parameterString, "?") : parameterString;
			final Map<String, List<String>> parameterValues = new HashMap<>();
			try
			{
				hasPath = parameterString.startsWith("/") || parameterString.contains("?") || hasPath;
				parameterValues.putAll(new QueryStringDecoder(
					parameterString,
					HttpConstants.DEFAULT_CHARSET,
					parameterString.contains("/") || hasPath,
					Integer.MAX_VALUE,
					true).parameters());
			}
			catch(final IllegalArgumentException iae)
			{
				LOG.error("Exception while parsing query string {}", parameterString, iae);
			}
			return new Parameters().withEntries(parameterValues).withRawParameterString(rawParameterString);
		}
		return null;
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	public void splitParameters(final Parameters matcher, final Parameters matched)
	{
		if(matcher != null && matched != null)
		{
			for(final Parameter matcherEntry : matcher.getEntries())
			{
				if(matcherEntry.getName().getParameterStyle() != null && matcherEntry.getName()
					.getParameterStyle()
					.isExploded())
				{
					for(final Parameter matchedEntry : matched.getEntries())
					{
						if(matcherEntry.getName().getValue().equals(matchedEntry.getName().getValue())
							|| matchedEntry.getName().getValue().matches(matcherEntry.getName().getValue()))
						{
							matchedEntry.replaceValues(new ExpandedParameterDecoder(this.configuration)
								.splitOnDelimiter(
									matcherEntry.getName().getParameterStyle(),
									matcherEntry.getName().getValue(),
									matchedEntry.getValues()));
							matched.replaceEntry(matchedEntry);
						}
					}
				}
			}
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	public List<NottableString> splitOnDelimiter(
		final ParameterStyle style,
		final String name,
		final List<NottableString> values)
	{
		if(isNotBlank(style.getRegex()))
		{
			final List<NottableString> splitValues = new ArrayList<>();
			for(final NottableString value : values)
			{
				final String actualValue = value.getValue();
				
				final Matcher quotedValue = QUOTED_PARAMETER_VALUE_PRE_CHECK.test(actualValue)
					? QUOTED_PARAMETER_VALUE.matcher(actualValue)
					: null;
				if(quotedValue != null && quotedValue.matches())
				{
					if(value.isOptional())
					{
						splitValues.add(optional(quotedValue.group(1), value.isNot()));
					}
					else
					{
						splitValues.add(string(quotedValue.group(1), value.isNot()));
					}
				}
				else if(!(JSON_VALUE_PRE_CHECK.test(actualValue) && JSON_VALUE.matcher(actualValue).matches()))
				{
					for(final String splitValue : actualValue.split(style.getRegex().replace("<name>", name)))
					{
						if(value.isOptional())
						{
							splitValues.add(optional(splitValue, value.isNot()));
						}
						else
						{
							splitValues.add(string(splitValue, value.isNot()));
						}
					}
				}
			}
			return splitValues;
		}
		else
		{
			return values;
		}
	}
}
