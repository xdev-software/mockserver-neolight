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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import software.xdev.mockserver.model.Cookie;
import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.Parameter;
import software.xdev.mockserver.model.RequestDefinition;


public class HttpRequestPrettyPrintedDTO extends RequestDefinition
{
	private int hashCode;
	private String method = "";
	private String path = "";
	private final Map<String, List<String>> queryStringParameters = new HashMap<>();
	private BodyDTO body;
	private final Map<String, String> cookies = new HashMap<>();
	private final Map<String, List<String>> headers = new HashMap<>();
	private Boolean keepAlive;
	
	public HttpRequestPrettyPrintedDTO(final HttpRequest httpRequest)
	{
		if(httpRequest != null)
		{
			this.method = httpRequest.getMethod().getValue();
			this.path = httpRequest.getPath().getValue();
			for(final Header header : httpRequest.getHeaderList())
			{
				this.headers.put(
					header.getName().getValue(),
					header.getValues().stream().map(NottableString::getValue).collect(Collectors.toList()));
			}
			for(final Cookie cookie : httpRequest.getCookieList())
			{
				this.cookies.put(cookie.getName().getValue(), cookie.getValue().getValue());
			}
			for(final Parameter parameter : httpRequest.getQueryStringParameterList())
			{
				this.queryStringParameters.put(
					parameter.getName().getValue(),
					parameter.getValues().stream().map(NottableString::getValue).collect(Collectors.toList()));
			}
			this.body = BodyDTO.createDTO(httpRequest.getBody());
			this.keepAlive = httpRequest.isKeepAlive();
			this.setNot(httpRequest.getNot());
		}
	}
	
	public String getMethod()
	{
		return this.method;
	}
	
	public String getPath()
	{
		return this.path;
	}
	
	public Map<String, List<String>> getQueryStringParameters()
	{
		return this.queryStringParameters;
	}
	
	public BodyDTO getBody()
	{
		return this.body;
	}
	
	public Map<String, List<String>> getHeaders()
	{
		return this.headers;
	}
	
	public Map<String, String> getCookies()
	{
		return this.cookies;
	}
	
	public Boolean getKeepAlive()
	{
		return this.keepAlive;
	}
	
	@Override
	public HttpRequestPrettyPrintedDTO shallowClone()
	{
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		if(this.hashCode() != o.hashCode())
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		final HttpRequestPrettyPrintedDTO that = (HttpRequestPrettyPrintedDTO)o;
		return Objects.equals(this.method, that.method)
			&& Objects.equals(this.path, that.path)
			&& Objects.equals(this.queryStringParameters, that.queryStringParameters)
			&& Objects.equals(this.body, that.body)
			&& Objects.equals(this.cookies, that.cookies)
			&& Objects.equals(this.headers, that.headers)
			&& Objects.equals(this.keepAlive, that.keepAlive);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode =
				Objects.hash(super.hashCode(),
					this.method, this.path, this.queryStringParameters, this.body, this.cookies, this.headers,
					this.keepAlive);
		}
		return this.hashCode;
	}
}
