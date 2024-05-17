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

import com.fasterxml.jackson.annotation.JsonAlias;

import software.xdev.mockserver.model.HttpOverrideForwardedRequest;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestModifier;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.HttpResponseModifier;


@SuppressWarnings("UnusedReturnValue")
public class HttpOverrideForwardedRequestDTO implements DTO<HttpOverrideForwardedRequest>
{
	@JsonAlias("httpRequest")
	private HttpRequestDTO requestOverride;
	private HttpRequestModifierDTO requestModifier;
	@JsonAlias("httpResponse")
	private HttpResponseDTO responseOverride;
	private HttpResponseModifierDTO responseModifier;
	private DelayDTO delay;
	
	public HttpOverrideForwardedRequestDTO(final HttpOverrideForwardedRequest httpOverrideForwardedRequest)
	{
		if(httpOverrideForwardedRequest != null)
		{
			final HttpRequest overrideHttpRequest = httpOverrideForwardedRequest.getRequestOverride();
			if(overrideHttpRequest != null)
			{
				this.requestOverride = new HttpRequestDTO(overrideHttpRequest);
			}
			final HttpRequestModifier modifyHttpRequest = httpOverrideForwardedRequest.getRequestModifier();
			if(modifyHttpRequest != null)
			{
				this.requestModifier = new HttpRequestModifierDTO(modifyHttpRequest);
			}
			final HttpResponse overrideHttpResponse = httpOverrideForwardedRequest.getResponseOverride();
			if(overrideHttpResponse != null)
			{
				this.responseOverride = new HttpResponseDTO(overrideHttpResponse);
			}
			final HttpResponseModifier modifyHttpResponse = httpOverrideForwardedRequest.getResponseModifier();
			if(modifyHttpResponse != null)
			{
				this.responseModifier = new HttpResponseModifierDTO(modifyHttpResponse);
			}
			this.delay = (httpOverrideForwardedRequest.getDelay() != null ?
				new DelayDTO(httpOverrideForwardedRequest.getDelay()) :
				null);
		}
	}
	
	public HttpOverrideForwardedRequestDTO()
	{
	}
	
	@Override
	public HttpOverrideForwardedRequest buildObject()
	{
		HttpRequest overrideHttpRequest = null;
		if(this.requestOverride != null)
		{
			overrideHttpRequest = this.requestOverride.buildObject();
		}
		HttpRequestModifier modifyHttpRequest = null;
		if(this.requestModifier != null)
		{
			modifyHttpRequest = this.requestModifier.buildObject();
		}
		HttpResponse overrideHttpResponse = null;
		if(this.responseOverride != null)
		{
			overrideHttpResponse = this.responseOverride.buildObject();
		}
		HttpResponseModifier modifyHttpResponse = null;
		if(this.responseModifier != null)
		{
			modifyHttpResponse = this.responseModifier.buildObject();
		}
		return new HttpOverrideForwardedRequest()
			.withRequestOverride(overrideHttpRequest)
			.withRequestModifier(modifyHttpRequest)
			.withResponseOverride(overrideHttpResponse)
			.withResponseModifier(modifyHttpResponse)
			.withDelay((this.delay != null ? this.delay.buildObject() : null));
	}
	
	public HttpRequestDTO getRequestOverride()
	{
		return this.requestOverride;
	}
	
	public HttpOverrideForwardedRequestDTO setRequestOverride(final HttpRequestDTO requestOverride)
	{
		this.requestOverride = requestOverride;
		return this;
	}
	
	public HttpRequestModifierDTO getRequestModifier()
	{
		return this.requestModifier;
	}
	
	public HttpOverrideForwardedRequestDTO setRequestModifier(final HttpRequestModifierDTO requestModifier)
	{
		this.requestModifier = requestModifier;
		return this;
	}
	
	public HttpResponseDTO getResponseOverride()
	{
		return this.responseOverride;
	}
	
	public HttpOverrideForwardedRequestDTO setResponseOverride(final HttpResponseDTO responseOverride)
	{
		this.responseOverride = responseOverride;
		return this;
	}
	
	public HttpResponseModifierDTO getResponseModifier()
	{
		return this.responseModifier;
	}
	
	public HttpOverrideForwardedRequestDTO setResponseModifier(final HttpResponseModifierDTO responseModifier)
	{
		this.responseModifier = responseModifier;
		return this;
	}
	
	public DelayDTO getDelay()
	{
		return this.delay;
	}
	
	public HttpOverrideForwardedRequestDTO setDelay(final DelayDTO delay)
	{
		this.delay = delay;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final HttpOverrideForwardedRequestDTO that))
		{
			return false;
		}
		return Objects.equals(this.getRequestOverride(), that.getRequestOverride())
			&& Objects.equals(this.getRequestModifier(), that.getRequestModifier())
			&& Objects.equals(this.getResponseOverride(), that.getResponseOverride())
			&& Objects.equals(this.getResponseModifier(), that.getResponseModifier())
			&& Objects.equals(this.getDelay(), that.getDelay());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			this.getRequestOverride(),
			this.getRequestModifier(),
			this.getResponseOverride(),
			this.getResponseModifier(),
			this.getDelay());
	}
}

