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

import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketRequestCallback;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardCallback;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.uuid.UUIDService;

import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry.WEB_SOCKET_CORRELATION_ID_HEADER_NAME;
import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpForwardObjectCallbackActionHandler extends HttpForwardAction {
    
    private static final Logger LOG = LoggerFactory.getLogger(HttpForwardObjectCallbackActionHandler.class);
    private WebSocketClientRegistry webSocketClientRegistry;

    public HttpForwardObjectCallbackActionHandler(HttpState httpStateHandler, NettyHttpClient httpClient) {
        super(httpClient);
        this.webSocketClientRegistry = httpStateHandler.getWebSocketClientRegistry();
    }

    public void handle(final HttpActionHandler actionHandler, final HttpObjectCallback httpObjectCallback, final HttpRequest request, final ResponseWriter responseWriter, final boolean synchronous, Runnable expectationPostProcessor) {
        final String clientId = httpObjectCallback.getClientId();
        if (LocalCallbackRegistry.forwardClientExists(clientId)) {
            handleLocally(actionHandler, httpObjectCallback, request, responseWriter, synchronous, clientId);
        } else {
            handleViaWebSocket(actionHandler, httpObjectCallback, request, responseWriter, synchronous, expectationPostProcessor, clientId);
        }
    }

    private void handleLocally(HttpActionHandler actionHandler, HttpObjectCallback httpObjectCallback, HttpRequest request, ResponseWriter responseWriter, boolean synchronous, String clientId) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Locally sending request {} to client {}", request, clientId);
        }
        ExpectationForwardCallback expectationForwardCallback = LocalCallbackRegistry.retrieveForwardCallback(clientId);
        try {
            HttpRequest callbackRequest = expectationForwardCallback.handle(request);
            final HttpForwardActionResult responseFuture = sendRequest(
                callbackRequest,
                null,
                null
            );
            ExpectationForwardAndResponseCallback expectationForwardAndResponseCallback = LocalCallbackRegistry.retrieveForwardAndResponseCallback(clientId);
            if (expectationForwardAndResponseCallback != null) {
                actionHandler.executeAfterForwardActionResponse(responseFuture, (httpResponse, exception) -> {
                    if (httpResponse != null) {
                        try {
                            HttpResponse callbackResponse = expectationForwardAndResponseCallback.handle(callbackRequest, httpResponse);
                            actionHandler.writeForwardActionResponse(callbackResponse, responseWriter, request, httpObjectCallback);
                        } catch (Throwable ex2) {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Returning {} because client {} response callback threw an exception", notFoundResponse(), clientId, ex2);
                            }
                            actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
                        }
                    } else if (exception != null) {
                        actionHandler.handleExceptionDuringForwardingRequest(httpObjectCallback, request, responseWriter, exception);
                    }
                }, synchronous);
            } else {
                actionHandler.writeForwardActionResponse(responseFuture, responseWriter, request, httpObjectCallback, synchronous);
            }
        } catch (Exception ex) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Returning {} because client {} request callback throw an exception", notFoundResponse(), clientId, ex);
            }
            actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
        }
    }

    private void handleViaWebSocket(HttpActionHandler actionHandler, HttpObjectCallback httpObjectCallback, HttpRequest request, ResponseWriter responseWriter, boolean synchronous, Runnable expectationPostProcessor, String clientId) {
        final String webSocketCorrelationId = UUIDService.getUUID();
        webSocketClientRegistry.registerForwardCallbackHandler(webSocketCorrelationId, new WebSocketRequestCallback() {
            @Override
            public void handle(final HttpRequest callbackRequest) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Received request over websocket {} from client {} for correlationId {}", callbackRequest, clientId, webSocketCorrelationId);
                }
                final HttpForwardActionResult responseFuture = sendRequest(
                    callbackRequest.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME),
                    null,
                    null
                );
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Received response for request {} from client {}", callbackRequest, clientId);
                }
                webSocketClientRegistry.unregisterForwardCallbackHandler(webSocketCorrelationId);
                if (expectationPostProcessor != null && isFalse(httpObjectCallback.getResponseCallback())) {
                    expectationPostProcessor.run();
                }
                if (isTrue(httpObjectCallback.getResponseCallback())) {
                    handleResponseViaWebSocket(callbackRequest, responseFuture, actionHandler, webSocketCorrelationId, clientId, expectationPostProcessor, responseWriter, httpObjectCallback, synchronous);
                } else {
                    actionHandler.writeForwardActionResponse(responseFuture, responseWriter, callbackRequest, httpObjectCallback, synchronous);
                }
            }

            @Override
            public void handleError(HttpResponse httpResponse) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error sending request over websocket for client {} for correlationId {}", clientId, webSocketCorrelationId);
                }
                webSocketClientRegistry.unregisterForwardCallbackHandler(webSocketCorrelationId);
                actionHandler.writeResponseActionResponse(httpResponse, responseWriter, request, httpObjectCallback, synchronous);
            }
        });
        if (!webSocketClientRegistry.sendClientMessage(clientId, request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId), null)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Returning {} because client {} has closed web socket connection", notFoundResponse(), clientId);
            }
            actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
        } else if (LOG.isTraceEnabled()) {
            LOG.trace("Sending request over websocket {} to client {} for correlationId {}", request, clientId, webSocketCorrelationId);
        }
    }

    private void handleResponseViaWebSocket(HttpRequest request, HttpForwardActionResult responseFuture, HttpActionHandler actionHandler, String webSocketCorrelationId, String clientId, Runnable expectationPostProcessor, ResponseWriter responseWriter, HttpObjectCallback httpObjectCallback, boolean synchronous) {
        actionHandler.executeAfterForwardActionResponse(responseFuture, (httpResponse, exception) -> {
            if (httpResponse != null) {
                // register callback for overridden response
                CompletableFuture<HttpResponse> httpResponseCompletableFuture = new CompletableFuture<>();
                webSocketClientRegistry.registerResponseCallbackHandler(webSocketCorrelationId, overriddenResponse -> {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Received response over websocket {} for request and response {} from "
                            + "client {} for correlationId {}",
                            overriddenResponse,
                            new HttpRequestAndHttpResponse()
                                .withHttpRequest(request)
                                .withHttpResponse(httpResponse),
                            clientId,
                            webSocketCorrelationId);
                    }
                    webSocketClientRegistry.unregisterResponseCallbackHandler(webSocketCorrelationId);
                    if (expectationPostProcessor != null) {
                        expectationPostProcessor.run();
                    }
                    httpResponseCompletableFuture.complete(overriddenResponse.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME));
                });
                // send websocket message to override response
                if (!webSocketClientRegistry.sendClientMessage(clientId, request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId), httpResponse)) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Returning {} because client {} has closed web socket connection", notFoundResponse(), clientId);
                    }
                    actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
                } else if (LOG.isTraceEnabled()) {
                    LOG.trace("Sending request over websocket {} to client {} for correlationId {}", request, clientId, webSocketCorrelationId);
                }
                // return overridden response
                actionHandler.writeForwardActionResponse(responseFuture.setHttpResponse(httpResponseCompletableFuture), responseWriter, request, httpObjectCallback, synchronous);
            } else if (exception != null) {
                actionHandler.handleExceptionDuringForwardingRequest(httpObjectCallback, request, responseWriter, exception);
            }
        }, synchronous);
    }

}
