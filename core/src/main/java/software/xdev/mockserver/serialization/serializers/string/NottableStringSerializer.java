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

import software.xdev.mockserver.model.NottableString;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


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
		final SerializationContext provider)
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
	{
		jgen.writeStartObject();
		if(Boolean.TRUE.equals(nottableString.isNot()))
		{
			jgen.writeBooleanProperty("not", true);
		}
		if(Boolean.TRUE.equals(nottableString.isOptional()))
		{
			jgen.writeBooleanProperty("optional", true);
		}
		if(nottableString.getParameterStyle() != null)
		{
			jgen.writePOJOProperty("parameterStyle", nottableString.getParameterStyle());
		}
		jgen.writePOJOProperty(valueFieldName, value);
		jgen.writeEndObject();
	}
}
