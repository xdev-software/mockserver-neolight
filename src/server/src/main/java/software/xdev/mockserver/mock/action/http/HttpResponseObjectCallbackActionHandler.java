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

import static software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry.WEB_SOCKET_CORRELATION_ID_HEADER_NAME;
import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.uuid.UUIDService;


public class HttpResponseObjectCallbackActionHandler
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpResponseObjectCallbackActionHandler.class);
	private final WebSocketClientRegistry webSocketClientRegistry;
	
	public HttpResponseObjectCallbackActionHandler(final HttpState httpStateHandler)
	{
		this.webSocketClientRegistry = httpStateHandler.getWebSocketClientRegistry();
	}
	
	public void handle(
		final HttpActionHandler actionHandler,
		final HttpObjectCallback httpObjectCallback,
		final HttpRequest request,
		final ResponseWriter responseWriter,
		final boolean synchronous,
		final Runnable expectationPostProcessor)
	{
		final String clientId = httpObjectCallback.getClientId();
		if(LocalCallbackRegistry.responseClientExists(clientId))
		{
			this.handleLocally(actionHandler, httpObjectCallback, request, responseWriter, synchronous, clientId);
		}
		else
		{
			this.handleViaWebSocket(
				actionHandler,
				httpObjectCallback,
				request,
				responseWriter,
				synchronous,
				expectationPostProcessor,
				clientId);
		}
	}
	
	private void handleLocally(
		final HttpActionHandler actionHandler,
		final HttpObjectCallback httpObjectCallback,
		final HttpRequest request,
		final ResponseWriter responseWriter,
		final boolean synchronous,
		final String clientId)
	{
		if(LOG.isTraceEnabled())
		{
			LOG.trace("Locally sending request {} to client {}", request, clientId);
		}
		try
		{
			final HttpResponse callbackResponse =
				LocalCallbackRegistry.retrieveResponseCallback(clientId).handle(request);
			actionHandler.writeResponseActionResponse(
				callbackResponse,
				responseWriter,
				request,
				httpObjectCallback,
				synchronous);
		}
		catch(final Exception ex)
		{
			if(LOG.isWarnEnabled())
			{
				LOG.warn("Returning {} because client {} response callback throw an exception",
					notFoundResponse(), clientId, ex);
			}
			actionHandler.writeResponseActionResponse(
				notFoundResponse(),
				responseWriter,
				request,
				httpObjectCallback,
				synchronous);
		}
	}
	
	private void handleViaWebSocket(
		final HttpActionHandler actionHandler,
		final HttpObjectCallback httpObjectCallback,
		final HttpRequest request,
		final ResponseWriter responseWriter,
		final boolean synchronous,
		final Runnable expectationPostProcessor,
		final String clientId)
	{
		final String webSocketCorrelationId = UUIDService.getUUID();
		this.webSocketClientRegistry.registerResponseCallbackHandler(webSocketCorrelationId, response -> {
			if(LOG.isTraceEnabled())
			{
				LOG.trace("Received response over websocket {} for request {} from client {} for correlationId {}",
					response, request, clientId, webSocketCorrelationId);
			}
			this.webSocketClientRegistry.unregisterResponseCallbackHandler(webSocketCorrelationId);
			if(expectationPostProcessor != null)
			{
				expectationPostProcessor.run();
			}
			actionHandler.writeResponseActionResponse(
				response.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME),
				responseWriter,
				request,
				httpObjectCallback,
				synchronous);
		});
		if(!this.webSocketClientRegistry.sendClientMessage(
			clientId,
			request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId),
			null))
		{
			if(LOG.isWarnEnabled())
			{
				LOG.warn("Returning {} because client {} has closed web socket connection",
					notFoundResponse(), clientId);
			}
			actionHandler.writeResponseActionResponse(
				notFoundResponse(),
				responseWriter,
				request,
				httpObjectCallback,
				synchronous);
		}
		else if(LOG.isTraceEnabled())
		{
			LOG.trace("Sending request over websocket {} to client {} for correlationId {}",
				request, clientId, webSocketCorrelationId);
		}
	}
}
