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
package software.xdev.mockserver.serialization.model;

import static software.xdev.mockserver.model.NottableString.string;

import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.Not;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.Parameters;
import software.xdev.mockserver.model.Protocol;
import software.xdev.mockserver.model.SocketAddress;


@SuppressWarnings("UnusedReturnValue")
public class HttpRequestDTO extends RequestDefinitionDTO implements DTO<HttpRequest>
{
	private NottableString method = string("");
	private NottableString path = string("");
	private Parameters pathParameters;
	private Parameters queryStringParameters;
	private BodyDTO body;
	private Cookies cookies;
	private Headers headers;
	private Boolean keepAlive;
	private Protocol protocol;
	private SocketAddress socketAddress;
	private String localAddress;
	private String remoteAddress;
	
	public HttpRequestDTO()
	{
		super(null);
	}
	
	public HttpRequestDTO(final HttpRequest httpRequest)
	{
		super(httpRequest != null ? httpRequest.getNot() : null);
		if(httpRequest != null)
		{
			this.method = httpRequest.getMethod();
			this.path = httpRequest.getPath();
			this.headers = httpRequest.getHeaders();
			this.cookies = httpRequest.getCookies();
			this.pathParameters = httpRequest.getPathParameters();
			this.queryStringParameters = httpRequest.getQueryStringParameters();
			this.body = BodyDTO.createDTO(httpRequest.getBody());
			this.keepAlive = httpRequest.isKeepAlive();
			this.protocol = httpRequest.getProtocol();
			this.socketAddress = httpRequest.getSocketAddress();
			this.localAddress = httpRequest.getLocalAddress();
			this.remoteAddress = httpRequest.getRemoteAddress();
		}
	}
	
	@Override
	public HttpRequest buildObject()
	{
		return (HttpRequest)new HttpRequest()
			.withMethod(this.method)
			.withPath(this.path)
			.withPathParameters(this.pathParameters)
			.withQueryStringParameters(this.queryStringParameters)
			.withBody(this.body != null ? Not.not(this.body.buildObject(), this.body.getNot()) : null)
			.withHeaders(this.headers)
			.withCookies(this.cookies)
			.withProtocol(this.protocol)
			.withKeepAlive(this.keepAlive)
			.withSocketAddress(this.socketAddress)
			.withLocalAddress(this.localAddress)
			.withRemoteAddress(this.remoteAddress)
			.withNot(this.getNot());
	}
	
	public NottableString getMethod()
	{
		return this.method;
	}
	
	public HttpRequestDTO setMethod(final NottableString method)
	{
		this.method = method;
		return this;
	}
	
	public NottableString getPath()
	{
		return this.path;
	}
	
	public HttpRequestDTO setPath(final NottableString path)
	{
		this.path = path;
		return this;
	}
	
	public Parameters getPathParameters()
	{
		return this.pathParameters;
	}
	
	public HttpRequestDTO setPathParameters(final Parameters pathParameters)
	{
		this.pathParameters = pathParameters;
		return this;
	}
	
	public Parameters getQueryStringParameters()
	{
		return this.queryStringParameters;
	}
	
	public HttpRequestDTO setQueryStringParameters(final Parameters queryStringParameters)
	{
		this.queryStringParameters = queryStringParameters;
		return this;
	}
	
	public BodyDTO getBody()
	{
		return this.body;
	}
	
	public HttpRequestDTO setBody(final BodyDTO body)
	{
		this.body = body;
		return this;
	}
	
	public Headers getHeaders()
	{
		return this.headers;
	}
	
	public HttpRequestDTO setHeaders(final Headers headers)
	{
		this.headers = headers;
		return this;
	}
	
	public Cookies getCookies()
	{
		return this.cookies;
	}
	
	public HttpRequestDTO setCookies(final Cookies cookies)
	{
		this.cookies = cookies;
		return this;
	}
	
	public Boolean getKeepAlive()
	{
		return this.keepAlive;
	}
	
	public HttpRequestDTO setKeepAlive(final Boolean keepAlive)
	{
		this.keepAlive = keepAlive;
		return this;
	}
	
	public Protocol getProtocol()
	{
		return this.protocol;
	}
	
	public HttpRequestDTO setProtocol(final Protocol protocol)
	{
		this.protocol = protocol;
		return this;
	}
	
	public SocketAddress getSocketAddress()
	{
		return this.socketAddress;
	}
	
	public HttpRequestDTO setSocketAddress(final SocketAddress socketAddress)
	{
		this.socketAddress = socketAddress;
		return this;
	}
	
	public String getLocalAddress()
	{
		return this.localAddress;
	}
	
	public HttpRequestDTO setLocalAddress(final String localAddress)
	{
		this.localAddress = localAddress;
		return this;
	}
	
	public String getRemoteAddress()
	{
		return this.remoteAddress;
	}
	
	public HttpRequestDTO setRemoteAddress(final String remoteAddress)
	{
		this.remoteAddress = remoteAddress;
		return this;
	}
}
