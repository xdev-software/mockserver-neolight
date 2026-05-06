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
package software.xdev.mockserver.serialization.serializers.matcher;

import software.xdev.mockserver.matchers.HttpRequestPropertiesMatcher;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


public class HttpRequestPropertiesMatcherSerializer extends StdSerializer<HttpRequestPropertiesMatcher>
{
	public HttpRequestPropertiesMatcherSerializer()
	{
		super(HttpRequestPropertiesMatcher.class);
	}
	
	@Override
	public void serialize(
		final HttpRequestPropertiesMatcher value,
		final JsonGenerator gen,
		final SerializationContext provider)
	{
		if(value.getHttpRequest() != null)
		{
			gen.writePOJO(value.getHttpRequest());
		}
	}
}
