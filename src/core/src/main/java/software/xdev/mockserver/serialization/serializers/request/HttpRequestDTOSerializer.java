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
package software.xdev.mockserver.serialization.serializers.request;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


public class HttpRequestDTOSerializer extends StdSerializer<HttpRequestDTO>
{
	public HttpRequestDTOSerializer()
	{
		super(HttpRequestDTO.class);
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
	@Override
	public void serialize(final HttpRequestDTO value, final JsonGenerator gen, final SerializationContext provider)
	{
		gen.writeStartObject();
		if(value.getNot() != null && value.getNot())
		{
			gen.writeBooleanProperty("not", value.getNot());
		}
		if(value.getMethod() != null && isNotBlank(value.getMethod().getValue()))
		{
			gen.writePOJOProperty("method", value.getMethod());
		}
		if(value.getPath() != null && isNotBlank(value.getPath().getValue()))
		{
			gen.writePOJOProperty("path", value.getPath());
		}
		if(value.getPathParameters() != null && !value.getPathParameters().isEmpty())
		{
			gen.writePOJOProperty("pathParameters", value.getPathParameters());
		}
		if(value.getQueryStringParameters() != null && !value.getQueryStringParameters().isEmpty())
		{
			gen.writePOJOProperty("queryStringParameters", value.getQueryStringParameters());
		}
		if(value.getHeaders() != null && !value.getHeaders().isEmpty())
		{
			gen.writePOJOProperty("headers", value.getHeaders());
		}
		if(value.getCookies() != null && !value.getCookies().isEmpty())
		{
			gen.writePOJOProperty("cookies", value.getCookies());
		}
		if(value.getKeepAlive() != null)
		{
			gen.writeBooleanProperty("keepAlive", value.getKeepAlive());
		}
		if(value.getSocketAddress() != null)
		{
			gen.writePOJOProperty("socketAddress", value.getSocketAddress());
		}
		if(value.getProtocol() != null)
		{
			gen.writeStringProperty("protocol", value.getProtocol().name());
		}
		if(isNotBlank(value.getLocalAddress()))
		{
			gen.writePOJOProperty("localAddress", value.getLocalAddress());
		}
		if(isNotBlank(value.getRemoteAddress()))
		{
			gen.writePOJOProperty("remoteAddress", value.getRemoteAddress());
		}
		if(value.getBody() != null)
		{
			gen.writePOJOProperty("body", value.getBody());
		}
		gen.writeEndObject();
	}
}
