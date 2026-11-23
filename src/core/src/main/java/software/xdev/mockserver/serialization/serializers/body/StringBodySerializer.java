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

import software.xdev.mockserver.model.StringBody;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


public class StringBodySerializer extends StdSerializer<StringBody>
{
	private final boolean serialiseDefaultValues;
	
	public StringBodySerializer(final boolean serialiseDefaultValues)
	{
		super(StringBody.class);
		this.serialiseDefaultValues = serialiseDefaultValues;
	}
	
	@SuppressWarnings("PMD.NPathComplexity")
	@Override
	public void serialize(final StringBody value, final JsonGenerator gen, final SerializationContext provider)
	{
		final boolean notFieldSetAndNotDefault = value.getNot() != null && value.getNot();
		final boolean optionalFieldSetAndNotDefault = value.getOptional() != null && value.getOptional();
		final boolean subStringFieldNotDefault = value.isSubString();
		final boolean contentTypeFields = value.getContentType() != null;
		if(this.serialiseDefaultValues || notFieldSetAndNotDefault || optionalFieldSetAndNotDefault
			|| contentTypeFields
			|| subStringFieldNotDefault)
		{
			gen.writeStartObject();
			if(notFieldSetAndNotDefault)
			{
				gen.writeBooleanProperty("not", true);
			}
			if(optionalFieldSetAndNotDefault)
			{
				gen.writeBooleanProperty("optional", true);
			}
			gen.writeStringProperty("type", value.getType().name());
			gen.writeStringProperty("string", value.getValue());
			if(subStringFieldNotDefault)
			{
				gen.writeBooleanProperty("subString", true);
			}
			if(contentTypeFields)
			{
				gen.writeStringProperty("contentType", value.getContentType());
			}
			gen.writeEndObject();
		}
		else
		{
			gen.writeString(value.getValue());
		}
	}
}
