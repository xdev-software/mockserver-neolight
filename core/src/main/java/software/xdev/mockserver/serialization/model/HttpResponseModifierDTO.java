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

import software.xdev.mockserver.model.HttpResponseModifier;


public class HttpResponseModifierDTO implements DTO<HttpResponseModifier>
{
	private HeadersModifierDTO headers;
	private CookiesModifierDTO cookies;
	
	public HttpResponseModifierDTO()
	{
	}
	
	public HttpResponseModifierDTO(final HttpResponseModifier httpResponseModifier)
	{
		if(httpResponseModifier != null)
		{
			this.headers = httpResponseModifier.getHeaders() != null
				? new HeadersModifierDTO(httpResponseModifier.getHeaders())
				: null;
			this.cookies = httpResponseModifier.getCookies() != null
				? new CookiesModifierDTO(httpResponseModifier.getCookies())
				: null;
		}
	}
	
	@Override
	public HttpResponseModifier buildObject()
	{
		return new HttpResponseModifier()
			.withHeaders(this.headers != null ? this.headers.buildObject() : null)
			.withCookies(this.cookies != null ? this.cookies.buildObject() : null);
	}
	
	public HeadersModifierDTO getHeaders()
	{
		return this.headers;
	}
	
	public HttpResponseModifierDTO setHeaders(final HeadersModifierDTO headers)
	{
		this.headers = headers;
		return this;
	}
	
	public CookiesModifierDTO getCookies()
	{
		return this.cookies;
	}
	
	public HttpResponseModifierDTO setCookies(final CookiesModifierDTO cookies)
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
		if(!(o instanceof final HttpResponseModifierDTO that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.getHeaders(), that.getHeaders()) && Objects.equals(
			this.getCookies(),
			that.getCookies());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.getHeaders(), this.getCookies());
	}
}
