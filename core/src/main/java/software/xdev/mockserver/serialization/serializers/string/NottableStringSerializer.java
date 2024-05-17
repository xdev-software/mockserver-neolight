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
package software.xdev.mockserver.serialization.serializers.string;

import static software.xdev.mockserver.model.NottableString.serialiseNottableString;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import software.xdev.mockserver.model.NottableString;


public class NottableStringSerializer extends StdSerializer<NottableString>
{
	public NottableStringSerializer()
	{
		super(NottableString.class);
	}
	
	@Override
	public void serialize(
		final NottableString nottableString,
		final JsonGenerator jgen,
		final SerializerProvider provider)
		throws IOException
	{
		if(nottableString.getParameterStyle() != null)
		{
			this.writeObject(nottableString, jgen, "value", nottableString.getValue());
		}
		else
		{
			jgen.writeString(serialiseNottableString(nottableString));
		}
	}
	
	private void writeObject(
		final NottableString nottableString,
		final JsonGenerator jgen,
		final String valueFieldName,
		final Object value)
		throws IOException
	{
		jgen.writeStartObject();
		if(Boolean.TRUE.equals(nottableString.isNot()))
		{
			jgen.writeBooleanField("not", true);
		}
		if(Boolean.TRUE.equals(nottableString.isOptional()))
		{
			jgen.writeBooleanField("optional", true);
		}
		if(nottableString.getParameterStyle() != null)
		{
			jgen.writeObjectField("parameterStyle", nottableString.getParameterStyle());
		}
		jgen.writeObjectField(valueFieldName, value);
		jgen.writeEndObject();
	}
}
