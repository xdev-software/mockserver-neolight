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

import java.util.Objects;

import software.xdev.mockserver.model.HttpRequestModifier;
import software.xdev.mockserver.model.PathModifier;


public class HttpRequestModifierDTO implements DTO<HttpRequestModifier>
{
	private PathModifier path;
	private QueryParametersModifierDTO queryStringParameters;
	private HeadersModifierDTO headers;
	private CookiesModifierDTO cookies;
	
	public HttpRequestModifierDTO()
	{
	}
	
	public HttpRequestModifierDTO(final HttpRequestModifier httpRequestModifier)
	{
		if(httpRequestModifier != null)
		{
			this.path = httpRequestModifier.getPath();
			this.queryStringParameters = httpRequestModifier.getQueryStringParameters() != null
				? new QueryParametersModifierDTO(httpRequestModifier.getQueryStringParameters())
				: null;
			this.headers = httpRequestModifier.getHeaders() != null
				? new HeadersModifierDTO(httpRequestModifier.getHeaders())
				: null;
			this.cookies = httpRequestModifier.getCookies() != null
				? new CookiesModifierDTO(httpRequestModifier.getCookies())
				: null;
		}
	}
	
	@Override
	public HttpRequestModifier buildObject()
	{
		return new HttpRequestModifier()
			.withPath(this.path)
			.withQueryStringParameters(this.queryStringParameters != null
				? this.queryStringParameters.buildObject()
				: null)
			.withHeaders(this.headers != null ? this.headers.buildObject() : null)
			.withCookies(this.cookies != null ? this.cookies.buildObject() : null);
	}
	
	public PathModifier getPath()
	{
		return this.path;
	}
	
	public HttpRequestModifierDTO setPath(final PathModifier path)
	{
		this.path = path;
		return this;
	}
	
	public QueryParametersModifierDTO getQueryStringParameters()
	{
		return this.queryStringParameters;
	}
	
	public HttpRequestModifierDTO setQueryStringParameters(final QueryParametersModifierDTO queryStringParameters)
	{
		this.queryStringParameters = queryStringParameters;
		return this;
	}
	
	public HeadersModifierDTO getHeaders()
	{
		return this.headers;
	}
	
	public HttpRequestModifierDTO setHeaders(final HeadersModifierDTO headers)
	{
		this.headers = headers;
		return this;
	}
	
	public CookiesModifierDTO getCookies()
	{
		return this.cookies;
	}
	
	public HttpRequestModifierDTO setCookies(final CookiesModifierDTO cookies)
	{
		this.cookies = cookies;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final HttpRequestModifierDTO that))
		{
			return false;
		}
		return Objects.equals(this.getPath(), that.getPath())
			&& Objects.equals(this.getQueryStringParameters(), that.getQueryStringParameters())
			&& Objects.equals(this.getHeaders(), that.getHeaders())
			&& Objects.equals(this.getCookies(), that.getCookies());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getPath(), this.getQueryStringParameters(), this.getHeaders(), this.getCookies());
	}
}
