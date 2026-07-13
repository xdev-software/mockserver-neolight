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

import software.xdev.mockserver.model.HttpRequest;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;


public class HttpRequestSerializer extends StdSerializer<HttpRequest>
{
	public HttpRequestSerializer()
	{
		super(HttpRequest.class);
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
	@Override
	public void serialize(final HttpRequest value, final JsonGenerator gen, final SerializationContext provider)
	{
		gen.writeStartObject();
		if(value.getNot() != null && value.getNot())
		{
			gen.writeBooleanProperty("not", value.getNot());
		}
		if(value.getMethod() != null && !value.getMethod().isBlank())
		{
			gen.writePOJOProperty("method", value.getMethod());
		}
		if(value.getPath() != null && !value.getPath().isBlank())
		{
			gen.writePOJOProperty("path", value.getPath());
		}
		if(value.getPathParameters() != null && !value.getPathParameters().isEmpty())
		{
			gen.writePOJOProperty("pathParameters", value.getPathParameters());
		}
		if(value.getQueryStringParameterList() != null && !value.getQueryStringParameterList().isEmpty())
		{
			gen.writePOJOProperty("queryStringParameters", value.getQueryStringParameters());
		}
		if(value.getHeaderList() != null && !value.getHeaderList().isEmpty())
		{
			gen.writePOJOProperty("headers", value.getHeaders());
		}
		if(value.getCookieList() != null && !value.getCookieList().isEmpty())
		{
			gen.writePOJOProperty("cookies", value.getCookies());
		}
		if(value.isKeepAlive() != null)
		{
			gen.writeBooleanProperty("keepAlive", value.isKeepAlive());
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
		if(value.getBody() != null && isNotBlank(String.valueOf(value.getBody().getValue())))
		{
			gen.writePOJOProperty("body", value.getBody());
		}
		gen.writeEndObject();
	}
}
