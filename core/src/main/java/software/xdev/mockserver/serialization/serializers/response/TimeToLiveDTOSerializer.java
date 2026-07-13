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
package software.xdev.mockserver.serialization.serializers.response;

import software.xdev.mockserver.serialization.model.TimeToLiveDTO;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


public class TimeToLiveDTOSerializer extends StdSerializer<TimeToLiveDTO>
{
	public TimeToLiveDTOSerializer()
	{
		super(TimeToLiveDTO.class);
	}
	
	@Override
	public void serialize(final TimeToLiveDTO value, final JsonGenerator gen, final SerializationContext provider)
	{
		gen.writeStartObject();
		if(!value.isUnlimited())
		{
			gen.writePOJOProperty("timeUnit", value.getTimeUnit());
			gen.writeNumberProperty("timeToLive", value.getTimeToLive());
		}
		else
		{
			gen.writeBooleanProperty("unlimited", value.isUnlimited());
		}
		gen.writeEndObject();
	}
}
