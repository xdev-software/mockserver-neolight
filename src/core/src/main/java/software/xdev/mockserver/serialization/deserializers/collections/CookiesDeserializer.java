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
package software.xdev.mockserver.serialization.deserializers.collections;

import static software.xdev.mockserver.model.NottableString.string;

import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.NottableString;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.JsonTokenId;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


public class CookiesDeserializer extends StdDeserializer<Cookies>
{
	public CookiesDeserializer()
	{
		super(Cookies.class);
	}
	
	@Override
	public Cookies deserialize(final JsonParser p, final DeserializationContext ctxt)
	{
		if(p.isExpectedStartArrayToken())
		{
			return this.deserializeArray(p, ctxt);
		}
		else if(p.isExpectedStartObjectToken())
		{
			return this.deserializeObject(p, ctxt);
		}
		else
		{
			return (Cookies)ctxt.handleUnexpectedToken(Cookies.class, p);
		}
	}
	
	private Cookies deserializeObject(
		final JsonParser jsonParser,
		final DeserializationContext ctxt)
	{
		final Cookies cookies = new Cookies();
		NottableString key = string("");
		while(true)
		{
			final JsonToken t = jsonParser.nextToken();
			switch(t.id())
			{
				case JsonTokenId.ID_PROPERTY_NAME:
					key = string(jsonParser.getString());
					break;
				case JsonTokenId.ID_STRING:
					cookies.withEntry(key, ctxt.readValue(jsonParser, NottableString.class));
					break;
				case JsonTokenId.ID_START_OBJECT:
					cookies.withEntry(key, ctxt.readValue(jsonParser, NottableString.class));
					break;
				case JsonTokenId.ID_END_OBJECT:
					return cookies;
				default:
					throw new RuntimeException(
						"Unexpected token: \"" + t + "\" id: \"" + t.id() + "\" text: \"" + jsonParser.getString());
			}
		}
	}
	
	private Cookies deserializeArray(
		final JsonParser jsonParser,
		final DeserializationContext ctxt)
	{
		final Cookies headers = new Cookies();
		NottableString key = null;
		NottableString value = null;
		String fieldName = null;
		while(true)
		{
			final JsonToken t = jsonParser.nextToken();
			switch(t.id())
			{
				case JsonTokenId.ID_END_ARRAY:
					return headers;
				case JsonTokenId.ID_START_OBJECT:
					key = null;
					value = null;
					break;
				case JsonTokenId.ID_PROPERTY_NAME:
					fieldName = jsonParser.getString();
					break;
				case JsonTokenId.ID_STRING:
					if("name".equals(fieldName))
					{
						key = string(ctxt.readValue(jsonParser, String.class));
					}
					else if("value".equals(fieldName))
					{
						value = ctxt.readValue(jsonParser, NottableString.class);
					}
					break;
				case JsonTokenId.ID_END_OBJECT:
					headers.withEntry(key, value);
					break;
				default:
					throw new RuntimeException(
						"Unexpected token: \"" + t + "\" id: \"" + t.id() + "\" text: \"" + jsonParser.getString());
			}
		}
	}
}
