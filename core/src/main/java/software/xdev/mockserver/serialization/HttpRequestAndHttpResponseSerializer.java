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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.serialization.model.HttpRequestAndHttpResponseDTO;


public class HttpRequestAndHttpResponseSerializer implements Serializer<HttpRequestAndHttpResponse>
{
	private final ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
	private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
	private final JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
	
	@Override
	public String serialize(final HttpRequestAndHttpResponse httpRequestAndHttpResponse)
	{
		try
		{
			return this.objectWriter.writeValueAsString(new HttpRequestAndHttpResponseDTO(httpRequestAndHttpResponse));
		}
		catch(final Exception e)
		{
			throw new IllegalStateException("Exception while serializing HttpRequestAndHttpResponse to JSON with "
				+ "value "
				+ httpRequestAndHttpResponse, e);
		}
	}
	
	public String serialize(final List<HttpRequestAndHttpResponse> httpRequests)
	{
		return this.serialize(httpRequests.toArray(new HttpRequestAndHttpResponse[0]));
	}
	
	public String serialize(final HttpRequestAndHttpResponse... httpRequests)
	{
		try
		{
			if(httpRequests != null && httpRequests.length > 0)
			{
				final HttpRequestAndHttpResponseDTO[] httpRequestDTOs =
					new HttpRequestAndHttpResponseDTO[httpRequests.length];
				for(int i = 0; i < httpRequests.length; i++)
				{
					httpRequestDTOs[i] = new HttpRequestAndHttpResponseDTO(httpRequests[i]);
				}
				return this.objectWriter.writeValueAsString(httpRequestDTOs);
			}
			else
			{
				return "[]";
			}
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing HttpRequestAndHttpResponse to JSON with value " + Arrays.asList(
					httpRequests), e);
		}
	}
	
	@SuppressWarnings("checkstyle:FinalParameters")
	@Override
	public HttpRequestAndHttpResponse deserialize(String jsonHttpRequest)
	{
		if(jsonHttpRequest.contains("\"httpRequestAndHttpResponse\""))
		{
			try
			{
				final JsonNode jsonNode = this.objectMapper.readTree(jsonHttpRequest);
				if(jsonNode.has("httpRequestAndHttpResponse"))
				{
					jsonHttpRequest = jsonNode.get("httpRequestAndHttpResponse").toString();
				}
			}
			catch(final Exception ex)
			{
				throw new IllegalArgumentException(
					"exception while parsing [" + jsonHttpRequest + "] for HttpRequestAndHttpResponse", ex);
			}
		}
		HttpRequestAndHttpResponse httpRequestAndHttpResponse = null;
		try
		{
			final HttpRequestAndHttpResponseDTO httpRequestDTO =
				this.objectMapper.readValue(jsonHttpRequest, HttpRequestAndHttpResponseDTO.class);
			if(httpRequestDTO != null)
			{
				httpRequestAndHttpResponse = httpRequestDTO.buildObject();
			}
		}
		catch(final Exception ex)
		{
			throw new IllegalArgumentException(
				"exception while parsing [" + jsonHttpRequest + "] for HttpRequestAndHttpResponse", ex);
		}
		return httpRequestAndHttpResponse;
	}
	
	@Override
	public Class<HttpRequestAndHttpResponse> supportsType()
	{
		return HttpRequestAndHttpResponse.class;
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	public HttpRequestAndHttpResponse[] deserializeArray(final String jsonHttpRequests)
	{
		final List<HttpRequestAndHttpResponse> httpRequests = new ArrayList<>();
		if(isBlank(jsonHttpRequests))
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - a request or request array is required but value was \"" + jsonHttpRequests
					+ "\"");
		}
		else
		{
			final List<String> jsonRequests = this.jsonArraySerializer.splitJSONArray(jsonHttpRequests);
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
					httpRequests.add(this.deserialize(jsonRequest));
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
		}
		return httpRequests.toArray(new HttpRequestAndHttpResponse[0]);
	}
}
