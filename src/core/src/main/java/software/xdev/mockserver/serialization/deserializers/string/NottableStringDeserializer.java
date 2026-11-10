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
package software.xdev.mockserver.serialization.deserializers.string;

import static software.xdev.mockserver.model.NottableOptionalString.optional;
import static software.xdev.mockserver.model.NottableString.string;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.ParameterStyle;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


public class NottableStringDeserializer extends StdDeserializer<NottableString>
{
	public NottableStringDeserializer()
	{
		super(NottableString.class);
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public NottableString deserialize(final JsonParser p, final DeserializationContext ctxt)
	{
		if(p.currentToken() == JsonToken.START_OBJECT)
		{
			Boolean not = null;
			Boolean optional = null;
			String value = null;
			ParameterStyle parameterStyle = null;
			
			while(p.nextToken() != JsonToken.END_OBJECT)
			{
				final String fieldName = p.currentName();
				if("not".equals(fieldName))
				{
					p.nextToken();
					not = p.getBooleanValue();
				}
				else if("optional".equals(fieldName))
				{
					p.nextToken();
					optional = p.getBooleanValue();
				}
				else if("value".equals(fieldName))
				{
					p.nextToken();
					value = ctxt.readValue(p, String.class);
				}
				else if("parameterStyle".equals(fieldName))
				{
					p.nextToken();
					parameterStyle = ctxt.readValue(p, ParameterStyle.class);
				}
			}
			
			NottableString result = null;
			if(Boolean.TRUE.equals(optional))
			{
				result = optional(value, not);
			}
			else if(isNotBlank(value))
			{
				result = string(value, not);
			}
			
			if(result != null && parameterStyle != null)
			{
				result.withStyle(parameterStyle);
			}
			
			return result;
		}
		else if(p.currentToken() == JsonToken.VALUE_STRING
			|| p.currentToken() == JsonToken.PROPERTY_NAME)
		{
			return string(ctxt.readValue(p, String.class));
		}
		return null;
	}
}
