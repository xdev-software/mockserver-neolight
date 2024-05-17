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

import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import software.xdev.mockserver.serialization.model.HttpRequestPrettyPrintedDTO;


public class HttpRequestSerializer implements Serializer<HttpRequest>
{
	private final ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
	private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
	private final JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
	
	@Override
	public String serialize(final HttpRequest httpRequest)
	{
		return this.serialize(false, httpRequest);
	}
	
	public String serialize(final boolean prettyPrint, final HttpRequest httpRequest)
	{
		try
		{
			if(prettyPrint)
			{
				return this.objectWriter.writeValueAsString(new HttpRequestPrettyPrintedDTO(httpRequest));
			}
			else
			{
				return this.objectWriter.writeValueAsString(new HttpRequestDTO(httpRequest));
			}
		}
		catch(final Exception e)
		{
			throw new RuntimeException("Exception while serializing HttpRequest to JSON with value " + httpRequest, e);
		}
	}
	
	public String serialize(final List<HttpRequest> httpRequests)
	{
		return this.serialize(false, httpRequests);
	}
	
	public String serialize(final boolean prettyPrint, final List<HttpRequest> httpRequests)
	{
		return this.serialize(prettyPrint, httpRequests.toArray(new HttpRequest[0]));
	}
	
	public String serialize(final HttpRequest... httpRequests)
	{
		return this.serialize(false, httpRequests);
	}
	
	public String serialize(final boolean prettyPrint, final HttpRequest... httpRequests)
	{
		try
		{
			if(httpRequests != null && httpRequests.length > 0)
			{
				if(prettyPrint)
				{
					final HttpRequestPrettyPrintedDTO[] httpRequestTemplateObjects =
						new HttpRequestPrettyPrintedDTO[httpRequests.length];
					for(int i = 0; i < httpRequests.length; i++)
					{
						httpRequestTemplateObjects[i] = new HttpRequestPrettyPrintedDTO(httpRequests[i]);
					}
					return this.objectWriter.writeValueAsString(httpRequestTemplateObjects);
				}
				else
				{
					final HttpRequestDTO[] httpRequestDTOs = new HttpRequestDTO[httpRequests.length];
					for(int i = 0; i < httpRequests.length; i++)
					{
						httpRequestDTOs[i] = new HttpRequestDTO(httpRequests[i]);
					}
					return this.objectWriter.writeValueAsString(httpRequestDTOs);
				}
			}
			else
			{
				return "[]";
			}
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing HttpRequest to JSON with value " + Arrays.asList(httpRequests),
				e);
		}
	}
	
	@Override
	public HttpRequest deserialize(String jsonHttpRequest)
	{
		if(jsonHttpRequest.contains("\"httpRequest\""))
		{
			try
			{
				final JsonNode jsonNode = this.objectMapper.readTree(jsonHttpRequest);
				if(jsonNode.has("httpRequest"))
				{
					jsonHttpRequest = jsonNode.get("httpRequest").toString();
				}
			}
			catch(final Exception ex)
			{
				throw new IllegalArgumentException(
					"exception while parsing [" + jsonHttpRequest + "] for HttpRequest",
					ex);
			}
		}
		HttpRequest httpRequest = null;
		try
		{
			final HttpRequestDTO httpRequestDTO = this.objectMapper.readValue(jsonHttpRequest, HttpRequestDTO.class);
			if(httpRequestDTO != null)
			{
				httpRequest = httpRequestDTO.buildObject();
			}
		}
		catch(final Exception ex)
		{
			throw new IllegalArgumentException(
				"exception while parsing [" + jsonHttpRequest + "] for HttpRequest",
				ex);
		}
		return httpRequest;
	}
	
	@Override
	public Class<HttpRequest> supportsType()
	{
		return HttpRequest.class;
	}
	
	public HttpRequest[] deserializeArray(final String jsonHttpRequests)
	{
		final List<HttpRequest> httpRequests = new ArrayList<>();
		if(isBlank(jsonHttpRequests))
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - a request or request array is required but value was \"" + jsonHttpRequests
					+ "\"");
		}
		else
		{
			final List<String> jsonRequestList = this.jsonArraySerializer.splitJSONArray(jsonHttpRequests);
			if(jsonRequestList.isEmpty())
			{
				throw new IllegalArgumentException(
					"1 error:" + NEW_LINE + " - a request or array of request is required");
			}
			else
			{
				final List<String> validationErrorsList = new ArrayList<>();
				for(final String jsonRequest : jsonRequestList)
				{
					try
					{
						httpRequests.add(this.deserialize(jsonRequest));
					}
					catch(final IllegalArgumentException iae)
					{
						validationErrorsList.add(iae.getMessage());
					}
				}
				if(!validationErrorsList.isEmpty())
				{
					throw new IllegalArgumentException((validationErrorsList.size() > 1 ? "[" : "")
						+ String.join("," + NEW_LINE, validationErrorsList)
						+ (validationErrorsList.size() > 1 ? "]" : ""));
				}
			}
		}
		return httpRequests.toArray(new HttpRequest[0]);
	}
}
