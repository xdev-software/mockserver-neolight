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
package software.xdev.mockserver.serialization.serializers.body;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import software.xdev.mockserver.model.ParameterBody;


public class ParameterBodySerializer extends StdSerializer<ParameterBody>
{
	public ParameterBodySerializer()
	{
		super(ParameterBody.class);
	}
	
	@Override
	public void serialize(
		final ParameterBody parameterBody,
		final JsonGenerator jgen,
		final SerializerProvider provider)
		throws IOException
	{
		jgen.writeStartObject();
		if(parameterBody.getNot() != null && parameterBody.getNot())
		{
			jgen.writeBooleanField("not", parameterBody.getNot());
		}
		if(parameterBody.getOptional() != null && parameterBody.getOptional())
		{
			jgen.writeBooleanField("optional", parameterBody.getOptional());
		}
		jgen.writeStringField("type", parameterBody.getType().name());
		if(!parameterBody.getValue().isEmpty())
		{
			jgen.writeObjectField("parameters", parameterBody.getValue());
		}
		jgen.writeEndObject();
	}
}
