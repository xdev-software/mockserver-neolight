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

import static software.xdev.mockserver.formatting.StringFormatter.formatLogMessage;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import software.xdev.mockserver.model.RequestDefinition;


public class MatchDifference
{
	public enum Field
	{
		METHOD("method"),
		PATH("path"),
		PATH_PARAMETERS("pathParameters"),
		QUERY_PARAMETERS("queryParameters"),
		COOKIES("cookies"),
		HEADERS("headers"),
		BODY("body"),
		SECURE("secure"),
		PROTOCOL("protocol"),
		KEEP_ALIVE("keep-alive"),
		OPERATION("operation"),
		OPENAPI("openapi");
		
		private final String name;
		
		Field(final String name)
		{
			this.name = name;
		}
		
		public String getName()
		{
			return this.name;
		}
	}
	
	
	private final boolean detailedMatchFailures;
	private final RequestDefinition httpRequest;
	private final Map<Field, List<String>> differences = new ConcurrentHashMap<>();
	private Field fieldName;
	
	public MatchDifference(final boolean detailedMatchFailures, final RequestDefinition httpRequest)
	{
		this.detailedMatchFailures = detailedMatchFailures;
		this.httpRequest = httpRequest;
	}
	
	public MatchDifference addDifference(final Field fieldName, final String messageFormat, final Object... arguments)
	{
		if(this.detailedMatchFailures
			&& isNotBlank(messageFormat)
			&& arguments != null
			&& fieldName != null)
		{
			this.differences
				.computeIfAbsent(fieldName, key -> new ArrayList<>())
				.add(formatLogMessage(1, messageFormat, arguments));
		}
		
		return this;
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public MatchDifference addDifference(final String messageFormat, final Object... arguments)
	{
		return this.addDifference(this.fieldName, messageFormat, arguments);
	}
	
	public RequestDefinition getHttpRequest()
	{
		return this.httpRequest;
	}
	
	public String getLogCorrelationId()
	{
		return this.httpRequest.getLogCorrelationId();
	}
	
	@SuppressWarnings("UnusedReturnValue")
	protected MatchDifference currentField(final Field fieldName)
	{
		this.fieldName = fieldName;
		return this;
	}
	
	public List<String> getDifferences(final Field fieldName)
	{
		return this.differences.get(fieldName);
	}
	
	public Map<Field, List<String>> getAllDifferences()
	{
		return this.differences;
	}
	
	public void addDifferences(final Map<Field, List<String>> differences)
	{
		for(final Field field : differences.keySet())
		{
			this.differences
				.computeIfAbsent(field, key -> new ArrayList<>())
				.addAll(differences.get(field));
		}
	}
}
