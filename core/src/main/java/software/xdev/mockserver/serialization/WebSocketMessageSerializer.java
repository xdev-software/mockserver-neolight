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
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.serialization.model.WebSocketMessageDTO;


@SuppressWarnings({"rawtypes", "unchecked", "PMD"})
public class WebSocketMessageSerializer
{
	private final ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
	private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
	private final Map<Class, Serializer> serializers;
	
	public WebSocketMessageSerializer()
	{
		this.serializers = Map.of(
			HttpRequest.class, new HttpRequestSerializer(),
			HttpResponse.class, new HttpResponseSerializer(),
			HttpRequestAndHttpResponse.class, new HttpRequestAndHttpResponseSerializer()
		);
	}
	
	public String serialize(final Object message) throws JsonProcessingException
	{
		if(this.serializers.containsKey(message.getClass()))
		{
			final WebSocketMessageDTO value = new WebSocketMessageDTO().setType(message.getClass().getName())
				.setValue(this.serializers.get(message.getClass()).serialize((message)));
			return this.objectWriter.writeValueAsString(value);
		}
		else
		{
			return this.objectWriter.writeValueAsString(new WebSocketMessageDTO().setType(message.getClass().getName())
				.setValue(this.objectMapper.writeValueAsString(message)));
		}
	}
	
	public Object deserialize(final String messageJson) throws ClassNotFoundException, IOException
	{
		final WebSocketMessageDTO webSocketMessageDTO =
			this.objectMapper.readValue(messageJson, WebSocketMessageDTO.class);
		if(webSocketMessageDTO.getType() != null && webSocketMessageDTO.getValue() != null)
		{
			final Class format = Class.forName(webSocketMessageDTO.getType());
			if(this.serializers.containsKey(format))
			{
				return this.serializers.get(format).deserialize(webSocketMessageDTO.getValue());
			}
			else
			{
				return this.objectMapper.readValue(webSocketMessageDTO.getValue(), format);
			}
		}
		else
		{
			return null;
		}
	}
}
