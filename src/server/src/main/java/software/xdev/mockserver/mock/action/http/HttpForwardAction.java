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

import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.filters.HopByHopHeaderFilter;
import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;


public abstract class HttpForwardAction
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpForwardAction.class);
	
	private final NettyHttpClient httpClient;
	private final HopByHopHeaderFilter hopByHopHeaderFilter = new HopByHopHeaderFilter();
	
	HttpForwardAction(final NettyHttpClient httpClient)
	{
		this.httpClient = httpClient;
	}
	
	protected HttpForwardActionResult sendRequest(
		final HttpRequest request,
		final InetSocketAddress remoteAddress,
		final Function<HttpResponse, HttpResponse> overrideHttpResponse)
	{
		try
		{
			return new HttpForwardActionResult(
				request,
				this.httpClient.sendRequest(
					this.hopByHopHeaderFilter.onRequest(request).withProtocol(null),
					remoteAddress),
				overrideHttpResponse,
				remoteAddress);
		}
		catch(final Exception e)
		{
			LOG.error("Exception forwarding request {}", request, e);
		}
		return this.notFoundFuture(request);
	}
	
	HttpForwardActionResult notFoundFuture(final HttpRequest httpRequest)
	{
		final CompletableFuture<HttpResponse> notFoundFuture = new CompletableFuture<>();
		notFoundFuture.complete(notFoundResponse());
		return new HttpForwardActionResult(httpRequest, notFoundFuture, null);
	}
}
