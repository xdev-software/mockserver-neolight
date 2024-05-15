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
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
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
import static org.slf4j.event.Level.*;

@SuppressWarnings("FieldMayBeFinal")
public class HttpForwardObjectCallbackActionHandler extends HttpForwardAction {

    private WebSocketClientRegistry webSocketClientRegistry;

    public HttpForwardObjectCallbackActionHandler(HttpState httpStateHandler, NettyHttpClient httpClient) {
        super(httpStateHandler.getMockServerLogger(), httpClient);
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
        if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setHttpRequest(request)
                    .setMessageFormat("locally sending request{}to client " + clientId)
                    .setArguments(request)
            );
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
                        } catch (Throwable throwable) {
                            if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                                mockServerLogger.logEvent(
                                    new LogEntry()
                                        .setLogLevel(WARN)
                                        .setHttpRequest(request)
                                        .setMessageFormat("returning{}because client " + clientId + " response callback threw an exception")
                                        .setArguments(notFoundResponse())
                                        .setThrowable(throwable)
                                );
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
        } catch (Throwable throwable) {
            if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(WARN)
                        .setHttpRequest(request)
                        .setMessageFormat("returning{}because client " + clientId + " request callback throw an exception")
                        .setArguments(notFoundResponse())
                        .setThrowable(throwable)
                );
            }
            actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
        }
    }

    private void handleViaWebSocket(HttpActionHandler actionHandler, HttpObjectCallback httpObjectCallback, HttpRequest request, ResponseWriter responseWriter, boolean synchronous, Runnable expectationPostProcessor, String clientId) {
        final String webSocketCorrelationId = UUIDService.getUUID();
        webSocketClientRegistry.registerForwardCallbackHandler(webSocketCorrelationId, new WebSocketRequestCallback() {
            @Override
            public void handle(final HttpRequest callbackRequest) {
                if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(TRACE)
                            .setHttpRequest(request)
                            .setMessageFormat("received request over websocket{}from client " + clientId + " for correlationId " + webSocketCorrelationId)
                            .setArguments(callbackRequest)
                    );
                }
                final HttpForwardActionResult responseFuture = sendRequest(
                    callbackRequest.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME),
                    null,
                    null
                );
                if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(TRACE)
                            .setHttpRequest(request)
                            .setMessageFormat("received response for request{}from client " + clientId)
                            .setArguments(callbackRequest)
                    );
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
                if (MockServerLogger.isEnabled(DEBUG) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(DEBUG)
                            .setHttpRequest(request)
                            .setMessageFormat("error sending request over websocket for client " + clientId + " for correlationId " + webSocketCorrelationId)
                    );
                }
                webSocketClientRegistry.unregisterForwardCallbackHandler(webSocketCorrelationId);
                actionHandler.writeResponseActionResponse(httpResponse, responseWriter, request, httpObjectCallback, synchronous);
            }
        });
        if (!webSocketClientRegistry.sendClientMessage(clientId, request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId), null)) {
            if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(WARN)
                        .setHttpRequest(request)
                        .setMessageFormat("returning{}because client " + clientId + " has closed web socket connection")
                        .setArguments(notFoundResponse())
                );
            }
            actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
        } else if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setHttpRequest(request)
                    .setMessageFormat("sending request over websocket{}to client " + clientId + " for correlationId " + webSocketCorrelationId)
                    .setArguments(request)
            );
        }
    }

    private void handleResponseViaWebSocket(HttpRequest request, HttpForwardActionResult responseFuture, HttpActionHandler actionHandler, String webSocketCorrelationId, String clientId, Runnable expectationPostProcessor, ResponseWriter responseWriter, HttpObjectCallback httpObjectCallback, boolean synchronous) {
        actionHandler.executeAfterForwardActionResponse(responseFuture, (httpResponse, exception) -> {
            if (httpResponse != null) {
                // register callback for overridden response
                CompletableFuture<HttpResponse> httpResponseCompletableFuture = new CompletableFuture<>();
                webSocketClientRegistry.registerResponseCallbackHandler(webSocketCorrelationId, overriddenResponse -> {
                    if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(TRACE)
                                .setHttpRequest(request)
                                .setMessageFormat("received response over websocket{}for request and response{}from client " + clientId + " for correlationId " + webSocketCorrelationId)
                                .setArguments(
                                    overriddenResponse,
                                    new HttpRequestAndHttpResponse()
                                        .withHttpRequest(request)
                                        .withHttpResponse(httpResponse)
                                )
                        );
                    }
                    webSocketClientRegistry.unregisterResponseCallbackHandler(webSocketCorrelationId);
                    if (expectationPostProcessor != null) {
                        expectationPostProcessor.run();
                    }
                    httpResponseCompletableFuture.complete(overriddenResponse.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME));
                });
                // send websocket message to override response
                if (!webSocketClientRegistry.sendClientMessage(clientId, request.clone().withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId), httpResponse)) {
                    if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(WARN)
                                .setHttpRequest(request)
                                .setMessageFormat("returning{}because client " + clientId + " has closed web socket connection")
                                .setArguments(notFoundResponse())
                        );
                    }
                    actionHandler.writeForwardActionResponse(notFoundFuture(request), responseWriter, request, httpObjectCallback, synchronous);
                } else if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(TRACE)
                            .setHttpRequest(request)
                            .setMessageFormat("sending response over websocket{}to client " + clientId + " for correlationId " + webSocketCorrelationId)
                            .setArguments(httpResponse)
                    );
                }
                // return overridden response
                actionHandler.writeForwardActionResponse(responseFuture.setHttpResponse(httpResponseCompletableFuture), responseWriter, request, httpObjectCallback, synchronous);
            } else if (exception != null) {
                actionHandler.handleExceptionDuringForwardingRequest(httpObjectCallback, request, responseWriter, exception);
            }
        }, synchronous);
    }

}
