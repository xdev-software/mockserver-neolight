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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;


public class HttpOverrideForwardedRequest extends Action<HttpOverrideForwardedRequest>
{
	private int hashCode;
	@JsonAlias("httpRequest")
	private HttpRequest requestOverride;
	private HttpRequestModifier requestModifier;
	@JsonAlias("httpResponse")
	private HttpResponse responseOverride;
	private HttpResponseModifier responseModifier;
	
	/**
	 * Static builder which will allow overriding proxied request with the specified request.
	 */
	public static HttpOverrideForwardedRequest forwardOverriddenRequest()
	{
		return new HttpOverrideForwardedRequest();
	}
	
	/**
	 * Static builder which will allow overriding proxied request with the specified request.
	 *
	 * @param httpRequest the HttpRequest specifying what to override
	 */
	public static HttpOverrideForwardedRequest forwardOverriddenRequest(final HttpRequest httpRequest)
	{
		return new HttpOverrideForwardedRequest().withRequestOverride(httpRequest);
	}
	
	/**
	 * Static builder which will allow overriding or modifying proxied request with the specified request.
	 *
	 * @param httpRequest     the HttpRequest specifying what to override
	 * @param requestModifier what to modify in the request
	 */
	public static HttpOverrideForwardedRequest forwardOverriddenRequest(
		final HttpRequest httpRequest,
		final HttpRequestModifier requestModifier)
	{
		return new HttpOverrideForwardedRequest()
			.withRequestOverride(httpRequest)
			.withRequestModifier(requestModifier);
	}
	
	/**
	 * Static builder which will allow overriding proxied request with the specified request.
	 *
	 * @param httpRequest  the HttpRequest specifying what to override
	 * @param httpResponse the HttpRequest specifying what to override
	 */
	public static HttpOverrideForwardedRequest forwardOverriddenRequest(
		final HttpRequest httpRequest,
		final HttpResponse httpResponse)
	{
		return new HttpOverrideForwardedRequest()
			.withRequestOverride(httpRequest)
			.withResponseOverride(httpResponse);
	}
	
	/**
	 * Static builder which will allow overriding proxied request with the specified request.
	 *
	 * @param httpRequest      the HttpRequest specifying what to override
	 * @param requestModifier  what to modify in the request
	 * @param httpResponse     the HttpRequest specifying what to override
	 * @param responseModifier what to modify in the response
	 */
	public static HttpOverrideForwardedRequest forwardOverriddenRequest(
		final HttpRequest httpRequest,
		final HttpRequestModifier requestModifier,
		final HttpResponse httpResponse,
		final HttpResponseModifier responseModifier)
	{
		return new HttpOverrideForwardedRequest()
			.withRequestOverride(httpRequest)
			.withResponseModifier(responseModifier)
			.withResponseOverride(httpResponse)
			.withRequestModifier(requestModifier);
	}
	
	public HttpRequest getRequestOverride()
	{
		return this.requestOverride;
	}
	
	/**
	 * All fields, headers, cookies, etc of the provided request will be overridden
	 *
	 * @param httpRequest the HttpRequest specifying what to override
	 */
	public HttpOverrideForwardedRequest withRequestOverride(final HttpRequest httpRequest)
	{
		this.requestOverride = httpRequest;
		this.hashCode = 0;
		return this;
	}
	
	public HttpRequestModifier getRequestModifier()
	{
		return this.requestModifier;
	}
	
	/**
	 * Allow path, query parameters, headers and cookies to be modified
	 *
	 * @param requestModifier what to modify
	 */
	public HttpOverrideForwardedRequest withRequestModifier(final HttpRequestModifier requestModifier)
	{
		this.requestModifier = requestModifier;
		this.hashCode = 0;
		return this;
	}
	
	public HttpResponse getResponseOverride()
	{
		return this.responseOverride;
	}
	
	/**
	 * All fields, headers, cookies, etc of the provided response will be overridden
	 *
	 * @param httpResponse the HttpResponse specifying what to override
	 */
	public HttpOverrideForwardedRequest withResponseOverride(final HttpResponse httpResponse)
	{
		this.responseOverride = httpResponse;
		this.hashCode = 0;
		return this;
	}
	
	public HttpResponseModifier getResponseModifier()
	{
		return this.responseModifier;
	}
	
	/**
	 * Allow headers and cookies to be modified
	 *
	 * @param responseModifier what to modify
	 */
	public HttpOverrideForwardedRequest withResponseModifier(final HttpResponseModifier responseModifier)
	{
		this.responseModifier = responseModifier;
		this.hashCode = 0;
		return this;
	}
	
	@Override
	@JsonIgnore
	public Type getType()
	{
		return Type.FORWARD_REPLACE;
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
		final HttpOverrideForwardedRequest that = (HttpOverrideForwardedRequest)o;
		return Objects.equals(this.requestOverride, that.requestOverride)
			&& Objects.equals(this.requestModifier, that.requestModifier)
			&& Objects.equals(this.responseOverride, that.responseOverride)
			&& Objects.equals(this.responseModifier, that.responseModifier);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode =
				Objects.hash(super.hashCode(), this.requestOverride,
					this.requestModifier, this.responseOverride, this.responseModifier);
		}
		return this.hashCode;
	}
}
