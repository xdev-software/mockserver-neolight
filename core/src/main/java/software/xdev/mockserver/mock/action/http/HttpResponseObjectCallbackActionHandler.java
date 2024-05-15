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

import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.uuid.UUIDService;

import static software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry.WEB_SOCKET_CORRELATION_ID_HEADER_NAME;
import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;
import static org.slf4j.event.Level.TRACE;
import static org.slf4j.event.Level.WARN;

@SuppressWarnings("FieldMayBeFinal")
public class HttpResponseObjectCallbackActionHandler {
    private WebSocketClientRegistry webSocketClientRegistry;
    private final MockServerLogger mockServerLogger;

    public HttpResponseObjectCallbackActionHandler(HttpState httpStateHandler) {
        this.mockServerLogger = httpStateHandler.getMockServerLogger();
        this.webSocketClientRegistry = httpStateHandler.getWebSocketClientRegistry();
    }

    public void handle(final HttpActionHandler actionHandler, final HttpObjectCallback httpObjectCallback, final HttpRequest request, final ResponseWriter responseWriter, final boolean synchronous, Runnable expectationPostProcessor) {
        final String clientId = httpObjectCallback.getClientId();
        if (LocalCallbackRegistry.responseClientExists(clientId)) {
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
        try {
            HttpResponse callbackResponse = LocalCallbackRegistry.retrieveResponseCallback(clientId).handle(request);
            actionHandler.writeResponseActionResponse(callbackResponse, responseWriter, request, httpObjectCallback, synchronous);
        } catch (Throwable throwable) {
            if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(WARN)
                        .setHttpRequest(request)
                        .setMessageFormat("returning{}because client " + clientId + " response callback throw an exception")
                        .setArguments(notFoundResponse())
                        .setThrowable(throwable)
                );
            }
            actionHandler.writeResponseActionResponse(notFoundResponse(), responseWriter, request, httpObjectCallback, synchronous);
        }
    }

    private void handleViaWebSocket(HttpActionHandler actionHandler, HttpObjectCallback httpObjectCallback, HttpRequest request, ResponseWriter responseWriter, boolean synchronous, Runnable expectationPostProcessor, String clientId) {
        final String webSocketCorrelationId = UUIDService.getUUID();
        webSocketClientRegistry.registerResponseCallbackHandler(webSocketCorrelationId, response -> {
            if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(TRACE)
                        .setHttpRequest(request)
                        .setMessageFormat("received response over websocket{}for request{}from client " + clientId + " for correlationId " + webSocketCorrelationId)
                        .setArguments(response, request)
                );
            }
            webSocketClientRegistry.unregisterResponseCallbackHandler(webSocketCorrelationId);
            if (expectationPostProcessor != null) {
                expectationPostProcessor.run();
            }
            actionHandler.writeResponseActionResponse(response.removeHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME), responseWriter, request, httpObjectCallback, synchronous);
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
            actionHandler.writeResponseActionResponse(notFoundResponse(), responseWriter, request, httpObjectCallback, synchronous);
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

}
