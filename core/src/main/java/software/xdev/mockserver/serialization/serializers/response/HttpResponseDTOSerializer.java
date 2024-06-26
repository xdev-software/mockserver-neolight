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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import software.xdev.mockserver.serialization.model.BinaryBodyDTO;
import software.xdev.mockserver.serialization.model.BodyWithContentTypeDTO;
import software.xdev.mockserver.serialization.model.HttpResponseDTO;
import software.xdev.mockserver.serialization.model.StringBodyDTO;


public class HttpResponseDTOSerializer extends StdSerializer<HttpResponseDTO>
{
	public HttpResponseDTOSerializer()
	{
		super(HttpResponseDTO.class);
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
	@Override
	public void serialize(
		final HttpResponseDTO httpResponseDTO,
		final JsonGenerator jgen,
		final SerializerProvider provider)
		throws IOException
	{
		jgen.writeStartObject();
		if(httpResponseDTO.getStatusCode() != null)
		{
			jgen.writeObjectField("statusCode", httpResponseDTO.getStatusCode());
		}
		if(httpResponseDTO.getReasonPhrase() != null)
		{
			jgen.writeObjectField("reasonPhrase", httpResponseDTO.getReasonPhrase());
		}
		if(httpResponseDTO.getHeaders() != null && !httpResponseDTO.getHeaders().isEmpty())
		{
			jgen.writeObjectField("headers", httpResponseDTO.getHeaders());
		}
		if(httpResponseDTO.getCookies() != null && !httpResponseDTO.getCookies().isEmpty())
		{
			jgen.writeObjectField("cookies", httpResponseDTO.getCookies());
		}
		final BodyWithContentTypeDTO body = httpResponseDTO.getBody();
		if(body != null)
		{
			if(body instanceof StringBodyDTO && !((StringBodyDTO)body).getString().isEmpty())
			{
				jgen.writeObjectField("body", body);
			}
			else if(body instanceof BinaryBodyDTO)
			{
				jgen.writeObjectField("body", body);
			}
		}
		if(httpResponseDTO.getDelay() != null)
		{
			jgen.writeObjectField("delay", httpResponseDTO.getDelay());
		}
		if(httpResponseDTO.getConnectionOptions() != null)
		{
			jgen.writeObjectField("connectionOptions", httpResponseDTO.getConnectionOptions());
		}
		jgen.writeEndObject();
	}
}
