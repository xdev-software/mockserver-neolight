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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import software.xdev.mockserver.model.HttpRequest;


public class HttpRequestSerializer extends StdSerializer<HttpRequest>
{
	public HttpRequestSerializer()
	{
		super(HttpRequest.class);
	}
	
	@Override
	public void serialize(final HttpRequest httpRequest, final JsonGenerator jgen, final SerializerProvider provider)
		throws IOException
	{
		jgen.writeStartObject();
		if(httpRequest.getNot() != null && httpRequest.getNot())
		{
			jgen.writeBooleanField("not", httpRequest.getNot());
		}
		if(httpRequest.getMethod() != null && !httpRequest.getMethod().isBlank())
		{
			jgen.writeObjectField("method", httpRequest.getMethod());
		}
		if(httpRequest.getPath() != null && !httpRequest.getPath().isBlank())
		{
			jgen.writeObjectField("path", httpRequest.getPath());
		}
		if(httpRequest.getPathParameters() != null && !httpRequest.getPathParameters().isEmpty())
		{
			jgen.writeObjectField("pathParameters", httpRequest.getPathParameters());
		}
		if(httpRequest.getQueryStringParameterList() != null && !httpRequest.getQueryStringParameterList().isEmpty())
		{
			jgen.writeObjectField("queryStringParameters", httpRequest.getQueryStringParameters());
		}
		if(httpRequest.getHeaderList() != null && !httpRequest.getHeaderList().isEmpty())
		{
			jgen.writeObjectField("headers", httpRequest.getHeaders());
		}
		if(httpRequest.getCookieList() != null && !httpRequest.getCookieList().isEmpty())
		{
			jgen.writeObjectField("cookies", httpRequest.getCookies());
		}
		if(httpRequest.isKeepAlive() != null)
		{
			jgen.writeBooleanField("keepAlive", httpRequest.isKeepAlive());
		}
		if(httpRequest.getSocketAddress() != null)
		{
			jgen.writeObjectField("socketAddress", httpRequest.getSocketAddress());
		}
		if(httpRequest.getProtocol() != null)
		{
			jgen.writeStringField("protocol", httpRequest.getProtocol().name());
		}
		if(isNotBlank(httpRequest.getLocalAddress()))
		{
			jgen.writeObjectField("localAddress", httpRequest.getLocalAddress());
		}
		if(isNotBlank(httpRequest.getRemoteAddress()))
		{
			jgen.writeObjectField("remoteAddress", httpRequest.getRemoteAddress());
		}
		if(httpRequest.getBody() != null && isNotBlank(String.valueOf(httpRequest.getBody().getValue())))
		{
			jgen.writeObjectField("body", httpRequest.getBody());
		}
		jgen.writeEndObject();
	}
}
