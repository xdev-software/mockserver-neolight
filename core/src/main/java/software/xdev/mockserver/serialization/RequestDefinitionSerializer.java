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
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import software.xdev.mockserver.serialization.model.HttpRequestPrettyPrintedDTO;
import software.xdev.mockserver.serialization.model.RequestDefinitionDTO;


public class RequestDefinitionSerializer implements Serializer<RequestDefinition>
{
	private final ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
	private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
	private final JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
	
	@Override
	public String serialize(final RequestDefinition requestDefinition)
	{
		return this.serialize(false, requestDefinition);
	}
	
	public String serialize(final boolean prettyPrint, final RequestDefinition requestDefinition)
	{
		try
		{
			if(requestDefinition instanceof final HttpRequest request)
			{
				return this.objectWriter.writeValueAsString(prettyPrint
					? new HttpRequestPrettyPrintedDTO(request)
					: new HttpRequestDTO(request));
			}
			return "";
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing RequestDefinition to JSON with value " + requestDefinition,
				e);
		}
	}
	
	public String serialize(final List<? extends RequestDefinition> requestDefinitions)
	{
		return this.serialize(false, requestDefinitions);
	}
	
	public String serialize(final boolean prettyPrint, final List<? extends RequestDefinition> requestDefinitions)
	{
		return this.serialize(prettyPrint, requestDefinitions.toArray(new RequestDefinition[0]));
	}
	
	public String serialize(final RequestDefinition... requestDefinitions)
	{
		return this.serialize(false, requestDefinitions);
	}
	
	public String serialize(final boolean prettyPrint, final RequestDefinition... requestDefinitions)
	{
		try
		{
			if(requestDefinitions != null && requestDefinitions.length > 0)
			{
				final Object[] requestDefinitionDTOs = new Object[requestDefinitions.length];
				for(int i = 0; i < requestDefinitions.length; i++)
				{
					if(requestDefinitions[i] instanceof final HttpRequest request)
					{
						requestDefinitionDTOs[i] = prettyPrint
							? new HttpRequestPrettyPrintedDTO(request)
							: new HttpRequestDTO(request);
					}
				}
				return this.objectWriter.writeValueAsString(requestDefinitionDTOs);
			}
			else
			{
				return "[]";
			}
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing RequestDefinition to JSON with value " + Arrays.asList(requestDefinitions),
				e);
		}
	}
	
	@SuppressWarnings("checkstyle:FinalParameters")
	@Override
	public RequestDefinition deserialize(String jsonRequestDefinition)
	{
		try
		{
			if(jsonRequestDefinition.contains("\"httpRequest\""))
			{
				final JsonNode jsonNode = this.objectMapper.readTree(jsonRequestDefinition);
				if(jsonNode.has("httpRequest"))
				{
					jsonRequestDefinition = jsonNode.get("httpRequest").toString();
				}
			}
			else if(jsonRequestDefinition.contains("\"openAPIDefinition\""))
			{
				final JsonNode jsonNode = this.objectMapper.readTree(jsonRequestDefinition);
				if(jsonNode.has("openAPIDefinition"))
				{
					jsonRequestDefinition = jsonNode.get("openAPIDefinition").toString();
				}
			}
			final RequestDefinitionDTO requestDefinitionDTO =
				this.objectMapper.readValue(jsonRequestDefinition, RequestDefinitionDTO.class);
			if(requestDefinitionDTO != null)
			{
				return requestDefinitionDTO.buildObject();
			}
			return null;
		}
		catch(final Exception ex)
		{
			throw new IllegalArgumentException(
				"exception while parsing [" + jsonRequestDefinition + "] for RequestDefinition",
				ex);
		}
	}
	
	@Override
	public Class<RequestDefinition> supportsType()
	{
		return RequestDefinition.class;
	}
	
	public RequestDefinition[] deserializeArray(final String jsonRequestDefinitions)
	{
		final List<RequestDefinition> requestDefinitions = new ArrayList<>();
		if(isBlank(jsonRequestDefinitions))
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - a request or request array is required but value was \""
					+ jsonRequestDefinitions + "\"");
		}
		else
		{
			final List<String> jsonRequestList = this.jsonArraySerializer.splitJSONArray(jsonRequestDefinitions);
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
						requestDefinitions.add(this.deserialize(jsonRequest));
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
		return requestDefinitions.toArray(new RequestDefinition[0]);
	}
}
