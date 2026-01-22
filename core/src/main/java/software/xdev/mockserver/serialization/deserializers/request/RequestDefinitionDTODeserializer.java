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
package software.xdev.mockserver.serialization.deserializers.request;

import static software.xdev.mockserver.model.NottableString.string;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.Parameters;
import software.xdev.mockserver.model.Protocol;
import software.xdev.mockserver.model.SocketAddress;
import software.xdev.mockserver.serialization.model.BodyDTO;
import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import software.xdev.mockserver.serialization.model.RequestDefinitionDTO;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


public class RequestDefinitionDTODeserializer extends StdDeserializer<RequestDefinitionDTO>
{
	private static final Logger LOG = LoggerFactory.getLogger(RequestDefinitionDTODeserializer.class);
	
	public RequestDefinitionDTODeserializer()
	{
		super(RequestDefinitionDTO.class);
	}
	
	@Override
	public RequestDefinitionDTO deserialize(
		final JsonParser p,
		final DeserializationContext ctxt)
	{
		if(p.currentToken() != JsonToken.START_OBJECT)
		{
			return null;
		}
		
		Boolean not = null;
		NottableString method = string("");
		NottableString path = string("");
		Parameters pathParameters = null;
		Parameters queryStringParameters = null;
		BodyDTO body = null;
		Cookies cookies = null;
		Headers headers = null;
		Boolean keepAlive = null;
		Protocol protocol = null;
		SocketAddress socketAddress = null;
		while(p.nextToken() != JsonToken.END_OBJECT)
		{
			final String fieldName = p.currentName();
			if(fieldName != null)
			{
				switch(fieldName)
				{
					case "not":
					{
						p.nextToken();
						not = p.getBooleanValue();
						break;
					}
					case "method":
					{
						p.nextToken();
						method = ctxt.readValue(p, NottableString.class);
						break;
					}
					case "path":
					{
						p.nextToken();
						path = ctxt.readValue(p, NottableString.class);
						break;
					}
					case "pathParameters":
					{
						p.nextToken();
						pathParameters = ctxt.readValue(p, Parameters.class);
						break;
					}
					case "queryStringParameters":
					{
						p.nextToken();
						queryStringParameters = ctxt.readValue(p, Parameters.class);
						break;
					}
					case "body":
					{
						p.nextToken();
						body = ctxt.readValue(p, BodyDTO.class);
						break;
					}
					case "cookies":
					{
						p.nextToken();
						cookies = ctxt.readValue(p, Cookies.class);
						break;
					}
					case "headers":
					{
						p.nextToken();
						headers = ctxt.readValue(p, Headers.class);
						break;
					}
					case "keepAlive":
					{
						p.nextToken();
						keepAlive = ctxt.readValue(p, Boolean.class);
						break;
					}
					case "socketAddress":
					{
						p.nextToken();
						socketAddress = ctxt.readValue(p, SocketAddress.class);
						break;
					}
					case "protocol":
					{
						p.nextToken();
						try
						{
							protocol = Protocol.valueOf(ctxt.readValue(p, String.class));
						}
						catch(final Exception ex)
						{
							LOG.error("Exception while parsing protocol value for RequestDefinitionDTO", ex);
						}
						break;
					}
					default:
						LOG.trace("Ignoring field '{}'", fieldName);
				}
			}
		}
		return (RequestDefinitionDTO)new HttpRequestDTO()
			.setMethod(method)
			.setPath(path)
			.setPathParameters(pathParameters)
			.setQueryStringParameters(queryStringParameters)
			.setBody(body)
			.setCookies(cookies)
			.setHeaders(headers)
			.setKeepAlive(keepAlive)
			.setProtocol(protocol)
			.setSocketAddress(socketAddress)
			.setNot(not);
	}
}
