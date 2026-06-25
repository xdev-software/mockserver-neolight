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

import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.LogEventRequestAndResponse;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.model.RequestDefinition;


public class LogEventRequestAndResponseDTO extends ObjectWithJsonToString implements DTO<LogEventRequestAndResponse>
{
	private String timestamp;
	private RequestDefinitionDTO httpRequest;
	private HttpResponseDTO httpResponse;
	
	public LogEventRequestAndResponseDTO()
	{
	}
	
	public LogEventRequestAndResponseDTO(final LogEventRequestAndResponse httpRequestAndHttpResponse)
	{
		if(httpRequestAndHttpResponse != null)
		{
			final RequestDefinition httpRequest = httpRequestAndHttpResponse.getHttpRequest();
			if(httpRequest instanceof HttpRequest)
			{
				this.httpRequest = new HttpRequestDTO((HttpRequest)httpRequest);
			}
			final HttpResponse httpResponse = httpRequestAndHttpResponse.getHttpResponse();
			if(httpResponse != null)
			{
				this.httpResponse = new HttpResponseDTO(httpResponse);
			}
			this.timestamp = httpRequestAndHttpResponse.getTimestamp();
		}
	}
	
	@Override
	public LogEventRequestAndResponse buildObject()
	{
		RequestDefinition httpRequest = null;
		HttpResponse httpResponse = null;
		if(this.httpRequest != null)
		{
			httpRequest = this.httpRequest.buildObject();
		}
		if(this.httpResponse != null)
		{
			httpResponse = this.httpResponse.buildObject();
		}
		return new LogEventRequestAndResponse()
			.withHttpRequest(httpRequest)
			.withHttpResponse(httpResponse)
			.withTimestamp(this.timestamp);
	}
	
	public String getTimestamp()
	{
		return this.timestamp;
	}
	
	public void setTimestamp(final String timestamp)
	{
		this.timestamp = timestamp;
	}
	
	public RequestDefinitionDTO getHttpRequest()
	{
		return this.httpRequest;
	}
	
	public void setHttpRequest(final HttpRequestDTO httpRequest)
	{
		this.httpRequest = httpRequest;
	}
	
	public HttpResponseDTO getHttpResponse()
	{
		return this.httpResponse;
	}
	
	public void setHttpResponse(final HttpResponseDTO httpResponse)
	{
		this.httpResponse = httpResponse;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final LogEventRequestAndResponseDTO that))
		{
			return false;
		}
		return Objects.equals(this.getTimestamp(), that.getTimestamp())
			&& Objects.equals(this.getHttpRequest(), that.getHttpRequest())
			&& Objects.equals(this.getHttpResponse(), that.getHttpResponse());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getTimestamp(), this.getHttpRequest(), this.getHttpResponse());
	}
}
