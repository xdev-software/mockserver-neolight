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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;


public class JsonArraySerializer
{
	private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();
	
	public List<String> splitJSONArray(final String jsonArray)
	{
		return this.splitJSONArrayToJSONNodes(jsonArray).stream()
			.map(JacksonUtils::prettyPrint)
			.collect(Collectors.toList());
	}
	
	public List<JsonNode> splitJSONArrayToJSONNodes(final String jsonArray)
	{
		final List<JsonNode> arrayItems = new ArrayList<>();
		try
		{
			final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonArray);
			if(jsonNode instanceof ArrayNode)
			{
				for(final JsonNode arrayElement : jsonNode)
				{
					arrayItems.add(arrayElement);
				}
			}
			else
			{
				arrayItems.add(jsonNode);
			}
		}
		catch(final IOException e)
		{
			throw new IllegalArgumentException(e);
		}
		return arrayItems;
	}
}
