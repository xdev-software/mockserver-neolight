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
package software.xdev.mockserver.model;

import java.util.List;
import java.util.Objects;


public class HttpResponseModifier extends ObjectWithJsonToString
{
	private int hashCode;
	private HeadersModifier headers;
	private CookiesModifier cookies;
	
	public static HttpResponseModifier responseModifier()
	{
		return new HttpResponseModifier();
	}
	
	public HeadersModifier getHeaders()
	{
		return this.headers;
	}
	
	public HttpResponseModifier withHeaders(final HeadersModifier headers)
	{
		this.headers = headers;
		this.hashCode = 0;
		return this;
	}
	
	public HttpResponseModifier withHeaders(
		final List<Header> add,
		final List<Header> replace,
		final List<String> remove)
	{
		this.headers = new HeadersModifier()
			.withAdd(new Headers(add))
			.withReplace(new Headers(replace))
			.withRemove(remove);
		this.hashCode = 0;
		return this;
	}
	
	public CookiesModifier getCookies()
	{
		return this.cookies;
	}
	
	public HttpResponseModifier withCookies(final CookiesModifier cookies)
	{
		this.cookies = cookies;
		this.hashCode = 0;
		return this;
	}
	
	public HttpResponseModifier withCookies(
		final List<Cookie> add,
		final List<Cookie> replace,
		final List<String> remove)
	{
		this.cookies = new CookiesModifier()
			.withAdd(new Cookies(add))
			.withReplace(new Cookies(replace))
			.withRemove(remove);
		this.hashCode = 0;
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
		final HttpResponseModifier that = (HttpResponseModifier)o;
		return Objects.equals(this.headers, that.headers)
			&& Objects.equals(this.cookies, that.cookies);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(this.headers, this.cookies);
		}
		return this.hashCode;
	}
}
