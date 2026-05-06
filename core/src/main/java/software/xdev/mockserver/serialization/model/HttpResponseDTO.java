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

import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpResponse;


public class HttpResponseDTO implements DTO<HttpResponse>
{
	private Integer statusCode;
	private String reasonPhrase;
	private BodyWithContentTypeDTO body;
	private Cookies cookies;
	private Headers headers;
	private DelayDTO delay;
	private ConnectionOptionsDTO connectionOptions;
	
	public HttpResponseDTO()
	{
	}
	
	public HttpResponseDTO(final HttpResponse httpResponse)
	{
		if(httpResponse != null)
		{
			this.statusCode = httpResponse.getStatusCode();
			this.reasonPhrase = httpResponse.getReasonPhrase();
			this.body = BodyWithContentTypeDTO.createWithContentTypeDTO(httpResponse.getBody());
			this.headers = httpResponse.getHeaders();
			this.cookies = httpResponse.getCookies();
			this.delay = httpResponse.getDelay() != null ? new DelayDTO(httpResponse.getDelay()) : null;
			this.connectionOptions = httpResponse.getConnectionOptions() != null
				? new ConnectionOptionsDTO(httpResponse.getConnectionOptions())
				: null;
		}
	}
	
	@Override
	public HttpResponse buildObject()
	{
		return new HttpResponse()
			.withStatusCode(this.statusCode)
			.withReasonPhrase(this.reasonPhrase)
			.withBody(this.body != null ? this.body.buildObject() : null)
			.withHeaders(this.headers)
			.withCookies(this.cookies)
			.withDelay(this.delay != null ? this.delay.buildObject() : null)
			.withConnectionOptions(this.connectionOptions != null ? this.connectionOptions.buildObject() : null);
	}
	
	public Integer getStatusCode()
	{
		return this.statusCode;
	}
	
	public HttpResponseDTO setStatusCode(final Integer statusCode)
	{
		this.statusCode = statusCode;
		return this;
	}
	
	public String getReasonPhrase()
	{
		return this.reasonPhrase;
	}
	
	public HttpResponseDTO setReasonPhrase(final String reasonPhrase)
	{
		this.reasonPhrase = reasonPhrase;
		return this;
	}
	
	public BodyWithContentTypeDTO getBody()
	{
		return this.body;
	}
	
	public HttpResponseDTO setBody(final BodyWithContentTypeDTO body)
	{
		this.body = body;
		return this;
	}
	
	public Headers getHeaders()
	{
		return this.headers;
	}
	
	public HttpResponseDTO setHeaders(final Headers headers)
	{
		this.headers = headers;
		return this;
	}
	
	public Cookies getCookies()
	{
		return this.cookies;
	}
	
	public HttpResponseDTO setCookies(final Cookies cookies)
	{
		this.cookies = cookies;
		return this;
	}
	
	public DelayDTO getDelay()
	{
		return this.delay;
	}
	
	public HttpResponseDTO setDelay(final DelayDTO delay)
	{
		this.delay = delay;
		return this;
	}
	
	public ConnectionOptionsDTO getConnectionOptions()
	{
		return this.connectionOptions;
	}
	
	public HttpResponseDTO setConnectionOptions(final ConnectionOptionsDTO connectionOptions)
	{
		this.connectionOptions = connectionOptions;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final HttpResponseDTO that))
		{
			return false;
		}
		return Objects.equals(this.getStatusCode(), that.getStatusCode())
			&& Objects.equals(this.getReasonPhrase(), that.getReasonPhrase()) && Objects.equals(
			this.getBody(),
			that.getBody()) && Objects.equals(this.getCookies(), that.getCookies()) && Objects.equals(
			this.getHeaders(),
			that.getHeaders()) && Objects.equals(this.getDelay(), that.getDelay()) && Objects.equals(
			this.getConnectionOptions(),
			that.getConnectionOptions());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			this.getStatusCode(),
			this.getReasonPhrase(),
			this.getBody(),
			this.getCookies(),
			this.getHeaders(),
			this.getDelay(),
			this.getConnectionOptions());
	}
}
