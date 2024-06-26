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


public class LogEventRequestAndResponse extends ObjectWithJsonToString
{
	private String timestamp;
	private RequestDefinition httpRequest;
	private HttpResponse httpResponse;
	
	public String getTimestamp()
	{
		return this.timestamp;
	}
	
	public LogEventRequestAndResponse withTimestamp(final String timestamp)
	{
		this.timestamp = timestamp;
		return this;
	}
	
	public RequestDefinition getHttpRequest()
	{
		return this.httpRequest;
	}
	
	public LogEventRequestAndResponse withHttpRequest(final RequestDefinition httpRequest)
	{
		this.httpRequest = httpRequest;
		return this;
	}
	
	public HttpResponse getHttpResponse()
	{
		return this.httpResponse;
	}
	
	public LogEventRequestAndResponse withHttpResponse(final HttpResponse httpResponse)
	{
		this.httpResponse = httpResponse;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final LogEventRequestAndResponse that))
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
