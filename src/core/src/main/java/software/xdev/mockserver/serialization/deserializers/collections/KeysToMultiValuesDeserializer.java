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

import software.xdev.mockserver.model.KeyMatchStyle;
import software.xdev.mockserver.model.KeysToMultiValues;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.ParameterStyle;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


public abstract class KeysToMultiValuesDeserializer<T extends KeysToMultiValues<?, ?>> extends StdDeserializer<T>
{
	KeysToMultiValuesDeserializer(final Class<T> valueClass)
	{
		super(valueClass);
	}
	
	public abstract T build();
	
	@Override
	public T deserialize(final JsonParser p, final DeserializationContext ctxt)
	{
		if(p.isExpectedStartArrayToken())
		{
			return this.deserializeArray(p, ctxt);
		}
		else if(p.isExpectedStartObjectToken())
		{
			return this.deserializeObject(p, ctxt);
		}
		return null;
	}
	
	private T deserializeObject(final JsonParser jsonParser, final DeserializationContext ctxt)
	{
		final T entries = this.build();
		NottableString key = string("");
		while(true)
		{
			JsonToken token = jsonParser.nextToken();
			switch(token)
			{
				case PROPERTY_NAME:
					key = string(jsonParser.getString());
					if("keyMatchStyle".equals(key.getValue()))
					{
						jsonParser.nextToken();
						entries.withKeyMatchStyle(ctxt.readValue(jsonParser, KeyMatchStyle.class));
					}
					break;
				case START_OBJECT:
					// parse parameterStyle and value
					jsonParser.nextToken();
					ParameterStyle parameterStyle = ParameterStyle.FORM_EXPLODED;
					NottableString[] values = null;
					while(token != JsonToken.END_OBJECT)
					{
						final String fieldName = jsonParser.currentName();
						if("values".equals(fieldName))
						{
							jsonParser.nextToken();
							values = ctxt.readValue(jsonParser, NottableString[].class);
						}
						else if("parameterStyle".equals(fieldName))
						{
							jsonParser.nextToken();
							parameterStyle = ctxt.readValue(jsonParser, ParameterStyle.class);
						}
						token = jsonParser.nextToken();
					}
					entries.withEntry(key.withStyle(parameterStyle), values);
					break;
				case START_ARRAY:
					entries.withEntry(key, ctxt.readValue(jsonParser, NottableString[].class));
					break;
				case VALUE_STRING:
					entries.withEntry(key, ctxt.readValue(jsonParser, NottableString.class));
					break;
				case END_OBJECT:
					return entries;
				default:
					throw new RuntimeException("Unexpected token: \"" + token + "\" id: \"" + token.id() + "\" text:"
						+ " \""
						+ jsonParser.getString());
			}
		}
	}
	
	private T deserializeArray(final JsonParser jsonParser, final DeserializationContext ctxt)
	{
		final T entries = this.build();
		NottableString key = string("");
		NottableString[] values = null;
		while(true)
		{
			final JsonToken token = jsonParser.nextToken();
			switch(token)
			{
				case START_ARRAY:
					values = ctxt.readValue(jsonParser, NottableString[].class);
					break;
				case END_ARRAY:
					return entries;
				case START_OBJECT:
					if(key != null)
					{
						key = null;
					}
					else
					{
						key = ctxt.readValue(jsonParser, NottableString.class);
					}
					values = null;
					break;
				case END_OBJECT:
					entries.withEntry(key, values);
					break;
				case PROPERTY_NAME:
					break;
				case VALUE_STRING:
					key = ctxt.readValue(jsonParser, NottableString.class);
					break;
				default:
					throw new RuntimeException("Unexpected token: \"" + token + "\" id: \"" + token.id() + "\" text:"
						+ " \""
						+ jsonParser.getString());
			}
		}
	}
}
