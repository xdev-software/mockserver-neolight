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

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry.WEB_SOCKET_CORRELATION_ID_HEADER_NAME;
import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketRequestCallback;
import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardCallback;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.uuid.UUIDService;


public class HttpForwardObjectCallbackActionHandler extends HttpForwardAction
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpForwardObjectCallbackActionHandler.class);
	private final WebSocketClientRegistry webSocketClientRegistry;
	
	public HttpForwardObjectCallbackActionHandler(final HttpState httpStateHandler, final NettyHttpClient httpClient)
	{
		super(httpClient);
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
		if(LocalCallbackRegistry.forwardClientExists(clientId))
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
	
	@SuppressWarnings("PMD.CognitiveComplexity")
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
		final ExpectationForwardCallback expectationForwardCallback =
			LocalCallbackRegistry.retrieveForwardCallback(clientId);
		try
		{
			final HttpRequest callbackRequest = expectationForwardCallback.handle(request);
			final HttpForwardActionResult responseFuture = this.sendRequest(
				callbackRequest,
				null,
				null
			);
			final ExpectationForwardAndResponseCallback expectationForwardAndResponseCallback =
				LocalCallbackRegistry.retrieveForwardAndResponseCallback(clientId);
			if(expectationForwardAndResponseCallback != null)
			{
				actionHandler.executeAfterForwardActionResponse(responseFuture, (httpResponse, exception) -> {
					if(httpResponse != null)
					{
						try
						{
							final HttpResponse callbackResponse =
								expectationForwardAndResponseCallback.handle(callbackRequest, httpResponse);
							actionHandler.writeForwardActionResponse(
								callbackResponse,
								responseWriter,
								request,
								httpObjectCallback);
						}
						catch(final Exception ex2)
						{
							if(LOG.isWarnEnabled())
							{
								LOG.warn(
									"Returning {} because client {} response callback threw an exception",
									notFoundResponse(),
									clientId,
									ex2);
							}
							actionHandler.writeForwardActionResponse(
								this.notFoundFuture(request),
								responseWriter,
								request,
								httpObjectCallback,
								synchronous);
						}
					}
					else if(exception != null)
					{
						actionHandler.handleExceptionDuringForwardingRequest(
							httpObjectCallback,
							request,
							responseWriter,
							exception);
					}
				}, synchronous);
			}
			else
			{
				actionHandler.writeForwardActionResponse(
					responseFuture,
					responseWriter,
					request,
					httpObjectCallback,
					synchronous);
			}
		}
		catch(final Exception ex)
		{
			if(LOG.isWarnEnabled())
			{
				LOG.warn(
					"Returning {} because client {} request callback throw an exception",
					notFoundResponse(),
					clientId,
					ex);
			}
			actionHandler.writeForwardActionResponse(
				this.notFoundFuture(request),
				responseWriter,
				request,
				httpObjectCallback,
				synchronous);
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
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
		this.webSocketClientRegistry.registerForwardCallbackHandler(
			webSocketCorrelationId,
			new WebSocketRequestCallback()
			{
				@Override
				public void handle(final HttpRequest callbackRequest)
				{
					if(LOG.isTraceEnabled())
					{
						LOG.trace(
							"Received request over websocket {} from client {} for correlationId {}",
							callbackRequest,
							clientId,
							webSocketCorrelationId);
					}
					final HttpForwardActionResult responseFuture =
						HttpForwardObjectCallbackActionHandler.this.sendRequest(
							callbackRequest.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME),
							null,
							null
						);
					if(LOG.isTraceEnabled())
					{
						LOG.trace("Received response for request {} from client {}", callbackRequest, clientId);
					}
					HttpForwardObjectCallbackActionHandler.this.webSocketClientRegistry
						.unregisterForwardCallbackHandler(webSocketCorrelationId);
					if(expectationPostProcessor != null && isFalse(httpObjectCallback.getResponseCallback()))
					{
						expectationPostProcessor.run();
					}
					if(isTrue(httpObjectCallback.getResponseCallback()))
					{
						HttpForwardObjectCallbackActionHandler.this.handleResponseViaWebSocket(
							callbackRequest,
							responseFuture,
							actionHandler,
							webSocketCorrelationId,
							clientId,
							expectationPostProcessor,
							responseWriter,
							httpObjectCallback,
							synchronous);
					}
					else
					{
						actionHandler.writeForwardActionResponse(
							responseFuture,
							responseWriter,
							callbackRequest,
							httpObjectCallback,
							synchronous);
					}
				}
				
				@Override
				public void handleError(final HttpResponse httpResponse)
				{
					if(LOG.isDebugEnabled())
					{
						LOG.debug(
							"Error sending request over websocket for client {} for correlationId {}",
							clientId,
							webSocketCorrelationId);
					}
					HttpForwardObjectCallbackActionHandler.this.webSocketClientRegistry
						.unregisterForwardCallbackHandler(webSocketCorrelationId);
					actionHandler.writeResponseActionResponse(
						httpResponse,
						responseWriter,
						request,
						httpObjectCallback,
						synchronous);
				}
			});
		if(!this.webSocketClientRegistry.sendClientMessage(
			clientId,
			request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId),
			null))
		{
			if(LOG.isWarnEnabled())
			{
				LOG.warn(
					"Returning {} because client {} has closed web socket connection",
					notFoundResponse(),
					clientId);
			}
			actionHandler.writeForwardActionResponse(
				this.notFoundFuture(request),
				responseWriter,
				request,
				httpObjectCallback,
				synchronous);
		}
		else if(LOG.isTraceEnabled())
		{
			LOG.trace(
				"Sending request over websocket {} to client {} for correlationId {}",
				request,
				clientId,
				webSocketCorrelationId);
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	private void handleResponseViaWebSocket(
		final HttpRequest request,
		final HttpForwardActionResult responseFuture,
		final HttpActionHandler actionHandler,
		final String webSocketCorrelationId,
		final String clientId,
		final Runnable expectationPostProcessor,
		final ResponseWriter responseWriter,
		final HttpObjectCallback httpObjectCallback,
		final boolean synchronous)
	{
		actionHandler.executeAfterForwardActionResponse(responseFuture, (httpResponse, exception) -> {
			if(httpResponse != null)
			{
				// register callback for overridden response
				final CompletableFuture<HttpResponse> httpResponseCompletableFuture = new CompletableFuture<>();
				this.webSocketClientRegistry.registerResponseCallbackHandler(
					webSocketCorrelationId,
					overriddenResponse -> {
						if(LOG.isTraceEnabled())
						{
							LOG.trace(
								"Received response over websocket {} for request and response {} from "
									+ "client {} for correlationId {}",
								overriddenResponse,
								new HttpRequestAndHttpResponse()
									.withHttpRequest(request)
									.withHttpResponse(httpResponse),
								clientId,
								webSocketCorrelationId);
						}
						this.webSocketClientRegistry.unregisterResponseCallbackHandler(webSocketCorrelationId);
						if(expectationPostProcessor != null)
						{
							expectationPostProcessor.run();
						}
						httpResponseCompletableFuture.complete(overriddenResponse.removeHeader(
							WEB_SOCKET_CORRELATION_ID_HEADER_NAME));
					});
				// send websocket message to override response
				if(!this.webSocketClientRegistry.sendClientMessage(
					clientId,
					request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId),
					httpResponse))
				{
					if(LOG.isWarnEnabled())
					{
						LOG.warn(
							"Returning {} because client {} has closed web socket connection",
							notFoundResponse(),
							clientId);
					}
					actionHandler.writeForwardActionResponse(
						this.notFoundFuture(request),
						responseWriter,
						request,
						httpObjectCallback,
						synchronous);
				}
				else if(LOG.isTraceEnabled())
				{
					LOG.trace(
						"Sending request over websocket {} to client {} for correlationId {}",
						request,
						clientId,
						webSocketCorrelationId);
				}
				// return overridden response
				actionHandler.writeForwardActionResponse(
					responseFuture.setHttpResponse(httpResponseCompletableFuture),
					responseWriter,
					request,
					httpObjectCallback,
					synchronous);
			}
			else if(exception != null)
			{
				actionHandler.handleExceptionDuringForwardingRequest(
					httpObjectCallback,
					request,
					responseWriter,
					exception);
			}
		}, synchronous);
	}
}
