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
package software.xdev.mockserver.serialization;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.util.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import software.xdev.mockserver.model.LogEventRequestAndResponse;
import software.xdev.mockserver.serialization.model.LogEventRequestAndResponseDTO;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.ObjectWriter;


public class LogEventRequestAndResponseSerializer
{
	private final JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
	private static ObjectWriter objectWriterCached;
	
	private static ObjectWriter objectWriter()
	{
		if(objectWriterCached == null)
		{
			objectWriterCached = ObjectMappers.DEFAULT_MAPPER
				.writer()
				.with(
					new DefaultPrettyPrinter()
						.withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
						.withObjectIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE));
		}
		return objectWriterCached;
	}
	
	public String serialize(final LogEventRequestAndResponse httpRequestAndHttpResponse)
	{
		try
		{
			return objectWriter().writeValueAsString(new LogEventRequestAndResponseDTO(httpRequestAndHttpResponse));
		}
		catch(final Exception e)
		{
			throw new IllegalArgumentException(
				"Exception while serializing HttpRequestAndHttpResponse to JSON with value "
					+ httpRequestAndHttpResponse,
				e);
		}
	}
	
	public String serialize(final List<LogEventRequestAndResponse> httpRequestAndHttpResponses)
	{
		return this.serialize(httpRequestAndHttpResponses.toArray(new LogEventRequestAndResponse[0]));
	}
	
	public String serialize(final LogEventRequestAndResponse... httpRequestAndHttpResponses)
	{
		try
		{
			if(httpRequestAndHttpResponses != null && httpRequestAndHttpResponses.length > 0)
			{
				final LogEventRequestAndResponseDTO[] httpRequestAndHttpResponseDTOS =
					new LogEventRequestAndResponseDTO[httpRequestAndHttpResponses.length];
				for(int i = 0; i < httpRequestAndHttpResponses.length; i++)
				{
					httpRequestAndHttpResponseDTOS[i] =
						new LogEventRequestAndResponseDTO(httpRequestAndHttpResponses[i]);
				}
				return objectWriter()
					.withDefaultPrettyPrinter()
					.writeValueAsString(httpRequestAndHttpResponseDTOS);
			}
			return "[]";
		}
		catch(final Exception e)
		{
			throw new IllegalArgumentException(
				"Exception while serializing HttpRequestAndHttpResponse to JSON with value " + Arrays.asList(
					httpRequestAndHttpResponses),
				e);
		}
	}
	
	public LogEventRequestAndResponse deserialize(final String jsonHttpRequestAndHttpResponse)
	{
		if(isBlank(jsonHttpRequestAndHttpResponse))
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - a request is required but value was \"" + jsonHttpRequestAndHttpResponse
					+ "\"");
		}
		
		LogEventRequestAndResponse httpRequestAndHttpResponse = null;
		try
		{
			final LogEventRequestAndResponseDTO httpRequestAndHttpResponseDTO =
				ObjectMappers.DEFAULT_MAPPER.readValue(
					jsonHttpRequestAndHttpResponse,
					LogEventRequestAndResponseDTO.class);
			if(httpRequestAndHttpResponseDTO != null)
			{
				httpRequestAndHttpResponse = httpRequestAndHttpResponseDTO.buildObject();
			}
		}
		catch(final Exception ex)
		{
			throw new IllegalArgumentException(
				"exception while parsing [" + jsonHttpRequestAndHttpResponse + "] for HttpRequestAndHttpResponse",
				ex);
		}
		return httpRequestAndHttpResponse;
	}
	
	public LogEventRequestAndResponse[] deserializeArray(final String jsonHttpRequestAndHttpResponse)
	{
		final List<LogEventRequestAndResponse> httpRequestAndHttpResponses = new ArrayList<>();
		if(isBlank(jsonHttpRequestAndHttpResponse))
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - a request or request array is required but value was \""
					+ jsonHttpRequestAndHttpResponse + "\"");
		}
		final List<String> jsonRequests =
			this.jsonArraySerializer.splitJSONArray(jsonHttpRequestAndHttpResponse);
		if(jsonRequests.isEmpty())
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - a request or array of request is required");
		}
		
		final List<String> validationErrors = new ArrayList<>();
		for(final String jsonRequest : jsonRequests)
		{
			try
			{
				httpRequestAndHttpResponses.add(this.deserialize(jsonRequest));
			}
			catch(final IllegalArgumentException iae)
			{
				validationErrors.add(iae.getMessage());
			}
		}
		if(!validationErrors.isEmpty())
		{
			throw new IllegalArgumentException((validationErrors.size() > 1 ? "[" : "")
				+ String.join("," + NEW_LINE, validationErrors)
				+ (validationErrors.size() > 1 ? "]" : ""));
		}
		
		return httpRequestAndHttpResponses.toArray(new LogEventRequestAndResponse[0]);
	}
}
