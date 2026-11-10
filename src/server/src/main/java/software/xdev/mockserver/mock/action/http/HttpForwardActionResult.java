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
package software.xdev.mockserver.mock.action.http;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;


public class HttpForwardActionResult
{
	private final HttpRequest httpRequest;
	private final InetSocketAddress remoteAddress;
	private CompletableFuture<HttpResponse> httpResponse;
	private final Function<HttpResponse, HttpResponse> overrideHttpResponse;
	private final AtomicBoolean overrideHttpResponseApplied = new AtomicBoolean(false);
	
	public HttpForwardActionResult(
		final HttpRequest httpRequest,
		final CompletableFuture<HttpResponse> httpResponse,
		final Function<HttpResponse, HttpResponse> overrideHttpResponse)
	{
		this(httpRequest, httpResponse, overrideHttpResponse, null);
	}
	
	HttpForwardActionResult(
		final HttpRequest httpRequest,
		final CompletableFuture<HttpResponse> httpResponse,
		final Function<HttpResponse, HttpResponse> overrideHttpResponse,
		final InetSocketAddress remoteAddress)
	{
		this.httpRequest = httpRequest;
		this.httpResponse = httpResponse;
		this.overrideHttpResponse = overrideHttpResponse;
		this.remoteAddress = remoteAddress;
	}
	
	public HttpRequest getHttpRequest()
	{
		return this.httpRequest;
	}
	
	public CompletableFuture<HttpResponse> getHttpResponse()
	{
		if(this.overrideHttpResponse == null)
		{
			return this.httpResponse;
		}
		if(this.overrideHttpResponseApplied.compareAndSet(false, true))
		{
			this.httpResponse = this.httpResponse.thenApply(response -> {
				if(response != null)
				{
					return this.overrideHttpResponse.apply(response);
				}
				else
				{
					return null;
				}
			});
		}
		return this.httpResponse;
	}
	
	public HttpForwardActionResult setHttpResponse(final CompletableFuture<HttpResponse> httpResponse)
	{
		this.httpResponse = httpResponse;
		return this;
	}
	
	public InetSocketAddress getRemoteAddress()
	{
		return this.remoteAddress;
	}
}
