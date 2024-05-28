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

import static software.xdev.mockserver.model.NottableString.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.Parameters;
import software.xdev.mockserver.util.StringUtils;


public class PathParametersDecoder
{
	private static final Pattern PATH_VARIABLE_NAME_PATTERN = Pattern.compile("\\{[.;]?([^*]+)\\*?}");
	
	public NottableString normalisePathWithParametersForMatching(final HttpRequest matcher)
	{
		if(matcher.getPath() == null)
		{
			// Unable to load API spec attribute paths.'/pets/{petId}'. Declared path parameter petId needs to be
			// defined
			// as a path parameter in path or operation level
			return null;
		}
		if(matcher.getPathParameters() != null && !matcher.getPathParameters().isEmpty())
		{
			final String value = matcher.getPath().getValue();
			if(value.contains("{"))
			{
				final List<String> pathParts = new ArrayList<>();
				for(final String pathPart : matcher.getPath().getValue().split("/"))
				{
					final Matcher pathParameterName = PATH_VARIABLE_NAME_PATTERN.matcher(pathPart);
					if(pathParameterName.matches())
					{
						pathParts.add(".*");
					}
					else
					{
						pathParts.add(pathPart);
					}
				}
				return string(String.join("/", pathParts) + (value.endsWith("/") ? "/" : ""));
			}
			else
			{
				return matcher.getPath();
			}
		}
		else
		{
			return matcher.getPath();
		}
	}
	
	public Parameters extractPathParameters(final HttpRequest matcher, final HttpRequest matched)
	{
		final Parameters parsedParameters =
			matched.getPathParameters() != null
				// https://github.com/mock-server/mockserver/pull/1791
				? matched.getPathParameters().clone()
				: new Parameters();
		if(matcher.getPathParameters() != null && !matcher.getPathParameters().isEmpty())
		{
			final String[] matcherPathParts = this.getPathParts(matcher.getPath());
			final String[] matchedPathParts = this.getPathParts(matched.getPath());
			if(matcherPathParts.length != matchedPathParts.length)
			{
				throw new IllegalArgumentException(
					"expected path " + matcher.getPath().getValue() + " has " + matcherPathParts.length
						+ " parts but path " + matched.getPath().getValue() + " has " + matchedPathParts.length
						+ " part" + (matchedPathParts.length > 1 ? "s " : " "));
			}
			for(int i = 0; i < matcherPathParts.length; i++)
			{
				final Matcher pathParameterName = PATH_VARIABLE_NAME_PATTERN.matcher(matcherPathParts[i]);
				if(pathParameterName.matches())
				{
					final String parameterName = pathParameterName.group(1);
					final List<String> parameterValues = new ArrayList<>();
					final Matcher pathParameterValue =
						Pattern.compile("[.;]?(?:" + parameterName + "=)?([^,]++)[.,;]?").matcher(matchedPathParts[i]);
					while(pathParameterValue.find())
					{
						parameterValues.add(pathParameterValue.group(1));
					}
					parsedParameters.withEntry(parameterName, parameterValues);
				}
			}
		}
		return parsedParameters;
	}
	
	private String[] getPathParts(final NottableString path)
	{
		return path != null
			? Arrays.stream(StringUtils.removeStart(path.getValue(), "/").split("/"))
				.filter(StringUtils::isNotBlank)
			.toArray(String[]::new)
			: new String[0];
	}
}
