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


public class HttpRequestModifier extends ObjectWithJsonToString
{
	private int hashCode;
	private PathModifier path;
	private QueryParametersModifier queryStringParameters;
	private HeadersModifier headers;
	private CookiesModifier cookies;
	
	public static HttpRequestModifier requestModifier()
	{
		return new HttpRequestModifier();
	}
	
	public PathModifier getPath()
	{
		return this.path;
	}
	
	public HttpRequestModifier withPath(final PathModifier path)
	{
		this.path = path;
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * <p>
	 * The regex and substitution values to use to modify matching substrings, if multiple matches are found they will
	 * all be modified with the substitution for full details of supported regex syntax see:
	 * http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html
	 * </p>
	 * <p>
	 * The substitution can specify matching groups using $ followed by the group number for example $1
	 * </p>
	 * <p>
	 * If a null or empty substitution string is provided the regex pattern will be used to remove any substring
	 * matching the regex
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * <pre>
	 * regex: ^/(.+)/(.+)$
	 * substitution: /prefix/$1/infix/$2/postfix
	 * then: /some/path &#61;&gt; /prefix/some/infix/path/postfix
	 * or: /some/longer/path &#61;&gt; /prefix/some/infix/longer/path/postfix
	 * </pre>
	 *
	 * @param regex        regex value to match on
	 * @param substitution the value to substitute for the regex
	 */
	public HttpRequestModifier withPath(final String regex, final String substitution)
	{
		this.path = new PathModifier()
			.withRegex(regex)
			.withSubstitution(substitution);
		this.hashCode = 0;
		return this;
	}
	
	public QueryParametersModifier getQueryStringParameters()
	{
		return this.queryStringParameters;
	}
	
	public HttpRequestModifier withQueryStringParameters(final QueryParametersModifier queryStringParameters)
	{
		this.queryStringParameters = queryStringParameters;
		this.hashCode = 0;
		return this;
	}
	
	public HttpRequestModifier withQueryStringParameters(
		final Parameters add,
		final Parameters replace,
		final List<String> remove)
	{
		this.queryStringParameters = new QueryParametersModifier()
			.withAdd(add)
			.withReplace(replace)
			.withRemove(remove);
		this.hashCode = 0;
		return this;
	}
	
	public HttpRequestModifier withQueryStringParameters(
		final List<Parameter> add,
		final List<Parameter> replace,
		final List<String> remove)
	{
		this.queryStringParameters = new QueryParametersModifier()
			.withAdd(new Parameters(add))
			.withReplace(new Parameters(replace))
			.withRemove(remove);
		this.hashCode = 0;
		return this;
	}
	
	public HeadersModifier getHeaders()
	{
		return this.headers;
	}
	
	public HttpRequestModifier withHeaders(final HeadersModifier headers)
	{
		this.headers = headers;
		this.hashCode = 0;
		return this;
	}
	
	public HttpRequestModifier withHeaders(
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
	
	public HttpRequestModifier withCookies(final CookiesModifier cookies)
	{
		this.cookies = cookies;
		this.hashCode = 0;
		return this;
	}
	
	public HttpRequestModifier withCookies(
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
		final HttpRequestModifier that = (HttpRequestModifier)o;
		return Objects.equals(this.path, that.path)
			&& Objects.equals(this.queryStringParameters, that.queryStringParameters)
			&& Objects.equals(this.headers, that.headers)
			&& Objects.equals(this.cookies, that.cookies);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(this.path, this.queryStringParameters, this.headers, this.cookies);
		}
		return this.hashCode;
	}
}
