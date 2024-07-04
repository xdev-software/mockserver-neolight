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

import software.xdev.mockserver.model.BinaryBody;
import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.ParameterBody;
import software.xdev.mockserver.model.StringBody;


public class HttpResponseSerializer extends StdSerializer<HttpResponse>
{
	public HttpResponseSerializer()
	{
		super(HttpResponse.class);
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
	@Override
	public void serialize(final HttpResponse httpResponse, final JsonGenerator jgen, final SerializerProvider provider)
		throws IOException
	{
		jgen.writeStartObject();
		if(httpResponse.getStatusCode() != null)
		{
			jgen.writeObjectField("statusCode", httpResponse.getStatusCode());
		}
		if(httpResponse.getReasonPhrase() != null)
		{
			jgen.writeObjectField("reasonPhrase", httpResponse.getReasonPhrase());
		}
		if(httpResponse.getHeaderList() != null && !httpResponse.getHeaderList().isEmpty())
		{
			jgen.writeObjectField("headers", httpResponse.getHeaders());
		}
		if(httpResponse.getCookieList() != null && !httpResponse.getCookieList().isEmpty())
		{
			jgen.writeObjectField("cookies", httpResponse.getCookies());
		}
		final Body<?> body = httpResponse.getBody();
		if(body != null)
		{
			if(body instanceof final StringBody stringBody && !stringBody.getValue().isEmpty())
			{
				jgen.writeObjectField("body", body);
			}
			else if(body instanceof final BinaryBody binaryBody && binaryBody.getValue().length > 0)
			{
				jgen.writeObjectField("body", body);
			}
			else if(body instanceof final ParameterBody parameterBody && !parameterBody.getValue().isEmpty())
			{
				jgen.writeObjectField("body", body);
			}
		}
		if(httpResponse.getDelay() != null)
		{
			jgen.writeObjectField("delay", httpResponse.getDelay());
		}
		if(httpResponse.getConnectionOptions() != null)
		{
			jgen.writeObjectField("connectionOptions", httpResponse.getConnectionOptions());
		}
		jgen.writeEndObject();
	}
}
