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

import software.xdev.mockserver.model.StringBody;


public class StringBodySerializer extends StdSerializer<StringBody>
{
	private final boolean serialiseDefaultValues;
	
	public StringBodySerializer(final boolean serialiseDefaultValues)
	{
		super(StringBody.class);
		this.serialiseDefaultValues = serialiseDefaultValues;
	}
	
	@Override
	public void serialize(final StringBody stringBody, final JsonGenerator jgen, final SerializerProvider provider)
		throws IOException
	{
		final boolean notFieldSetAndNotDefault = stringBody.getNot() != null && stringBody.getNot();
		final boolean optionalFieldSetAndNotDefault = stringBody.getOptional() != null && stringBody.getOptional();
		final boolean subStringFieldNotDefault = stringBody.isSubString();
		final boolean contentTypeFieldSet = stringBody.getContentType() != null;
		if(this.serialiseDefaultValues || notFieldSetAndNotDefault || optionalFieldSetAndNotDefault
			|| contentTypeFieldSet
			|| subStringFieldNotDefault)
		{
			jgen.writeStartObject();
			if(notFieldSetAndNotDefault)
			{
				jgen.writeBooleanField("not", true);
			}
			if(optionalFieldSetAndNotDefault)
			{
				jgen.writeBooleanField("optional", true);
			}
			jgen.writeStringField("type", stringBody.getType().name());
			jgen.writeStringField("string", stringBody.getValue());
			if(subStringFieldNotDefault)
			{
				jgen.writeBooleanField("subString", true);
			}
			if(contentTypeFieldSet)
			{
				jgen.writeStringField("contentType", stringBody.getContentType());
			}
			jgen.writeEndObject();
		}
		else
		{
			jgen.writeString(stringBody.getValue());
		}
	}
}
