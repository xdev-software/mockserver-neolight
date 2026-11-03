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


public class HttpRequestAndHttpResponse extends ObjectWithJsonToString
{
	private int hashCode;
	private HttpRequest httpRequest;
	private HttpResponse httpResponse;
	
	public HttpRequest getHttpRequest()
	{
		return this.httpRequest;
	}
	
	public HttpRequestAndHttpResponse withHttpRequest(final HttpRequest httpRequest)
	{
		this.httpRequest = httpRequest;
		this.hashCode = 0;
		return this;
	}
	
	public HttpResponse getHttpResponse()
	{
		return this.httpResponse;
	}
	
	public HttpRequestAndHttpResponse withHttpResponse(final HttpResponse httpResponse)
	{
		this.httpResponse = httpResponse;
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
		final HttpRequestAndHttpResponse that = (HttpRequestAndHttpResponse)o;
		return Objects.equals(this.httpRequest, that.httpRequest)
			&& Objects.equals(this.httpResponse, that.httpResponse);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(this.httpRequest, this.httpResponse);
		}
		return this.hashCode;
	}
}
