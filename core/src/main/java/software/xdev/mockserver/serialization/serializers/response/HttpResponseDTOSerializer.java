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

import software.xdev.mockserver.serialization.model.BinaryBodyDTO;
import software.xdev.mockserver.serialization.model.BodyWithContentTypeDTO;
import software.xdev.mockserver.serialization.model.HttpResponseDTO;
import software.xdev.mockserver.serialization.model.StringBodyDTO;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


public class HttpResponseDTOSerializer extends StdSerializer<HttpResponseDTO>
{
	public HttpResponseDTOSerializer()
	{
		super(HttpResponseDTO.class);
	}
	
	@SuppressWarnings({"PMD.NPathComplexity"})
	@Override
	public void serialize(final HttpResponseDTO value, final JsonGenerator gen, final SerializationContext provider)
	{
		gen.writeStartObject();
		if(value.getStatusCode() != null)
		{
			gen.writePOJOProperty("statusCode", value.getStatusCode());
		}
		if(value.getReasonPhrase() != null)
		{
			gen.writePOJOProperty("reasonPhrase", value.getReasonPhrase());
		}
		if(value.getHeaders() != null && !value.getHeaders().isEmpty())
		{
			gen.writePOJOProperty("headers", value.getHeaders());
		}
		if(value.getCookies() != null && !value.getCookies().isEmpty())
		{
			gen.writePOJOProperty("cookies", value.getCookies());
		}
		final BodyWithContentTypeDTO body = value.getBody();
		if(body != null)
		{
			if(body instanceof StringBodyDTO && !((StringBodyDTO)body).getString().isEmpty())
			{
				gen.writePOJOProperty("body", body);
			}
			else if(body instanceof BinaryBodyDTO)
			{
				gen.writePOJOProperty("body", body);
			}
		}
		if(value.getDelay() != null)
		{
			gen.writePOJOProperty("delay", value.getDelay());
		}
		if(value.getConnectionOptions() != null)
		{
			gen.writePOJOProperty("connectionOptions", value.getConnectionOptions());
		}
		gen.writeEndObject();
	}
}
