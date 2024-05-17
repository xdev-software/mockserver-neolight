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

import software.xdev.mockserver.model.ExpectationId;


public class ExpectationIdSerializer implements Serializer<ExpectationId>
{
	private final ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
	private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
	private final JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
	
	@Override
	public String serialize(final ExpectationId expectationId)
	{
		try
		{
			return this.objectWriter.writeValueAsString(expectationId);
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing ExpectationId to JSON with value " + expectationId,
				e);
		}
	}
	
	public String serialize(final List<? extends ExpectationId> expectationIds)
	{
		return this.serialize(expectationIds.toArray(new ExpectationId[0]));
	}
	
	public String serialize(final ExpectationId... expectationIds)
	{
		try
		{
			if(expectationIds != null && expectationIds.length > 0)
			{
				return this.objectWriter.writeValueAsString(expectationIds);
			}
			return "[]";
		}
		catch(final Exception e)
		{
			throw new IllegalStateException(
				"Exception while serializing ExpectationId to JSON with value " + Arrays.asList(expectationIds),
				e);
		}
	}
	
	@Override
	public ExpectationId deserialize(String jsonExpectationId)
	{
		try
		{
			if(jsonExpectationId.contains("\"httpRequest\""))
			{
				final JsonNode jsonNode = this.objectMapper.readTree(jsonExpectationId);
				if(jsonNode.has("httpRequest"))
				{
					jsonExpectationId = jsonNode.get("httpRequest").toString();
				}
			}
			else if(jsonExpectationId.contains("\"openAPIDefinition\""))
			{
				final JsonNode jsonNode = this.objectMapper.readTree(jsonExpectationId);
				if(jsonNode.has("openAPIDefinition"))
				{
					jsonExpectationId = jsonNode.get("openAPIDefinition").toString();
				}
			}
			return this.objectMapper.readValue(jsonExpectationId, ExpectationId.class);
		}
		catch(final Exception ex)
		{
			throw new IllegalArgumentException(
				"exception while parsing [" + jsonExpectationId + "] for ExpectationId",
				ex);
		}
	}
	
	@Override
	public Class<ExpectationId> supportsType()
	{
		return ExpectationId.class;
	}
	
	public ExpectationId[] deserializeArray(final String jsonExpectationIds)
	{
		final List<ExpectationId> expectationIds = new ArrayList<>();
		if(isBlank(jsonExpectationIds))
		{
			throw new IllegalArgumentException(
				"1 error:" + NEW_LINE + " - a request or request array is required but value was \""
					+ jsonExpectationIds + "\"");
		}
		else
		{
			final List<String> jsonRequestList = this.jsonArraySerializer.splitJSONArray(jsonExpectationIds);
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
						expectationIds.add(this.deserialize(jsonRequest));
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
		return expectationIds.toArray(new ExpectationId[0]);
	}
}
