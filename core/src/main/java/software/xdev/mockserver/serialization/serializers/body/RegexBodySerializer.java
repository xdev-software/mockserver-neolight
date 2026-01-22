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

import software.xdev.mockserver.model.RegexBody;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


public class RegexBodySerializer extends StdSerializer<RegexBody>
{
	public RegexBodySerializer()
	{
		super(RegexBody.class);
	}
	
	@Override
	public void serialize(final RegexBody regexBody, final JsonGenerator jgen, final SerializationContext provider)
	{
		jgen.writeStartObject();
		if(regexBody.getNot() != null && regexBody.getNot())
		{
			jgen.writeBooleanProperty("not", regexBody.getNot());
		}
		if(regexBody.getOptional() != null && regexBody.getOptional())
		{
			jgen.writeBooleanProperty("optional", regexBody.getOptional());
		}
		jgen.writeStringProperty("type", regexBody.getType().name());
		jgen.writeStringProperty("regex", regexBody.getValue());
		jgen.writeEndObject();
	}
}
