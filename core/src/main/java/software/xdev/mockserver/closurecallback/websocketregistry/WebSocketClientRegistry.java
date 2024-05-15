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
package software.xdev.mockserver.closurecallback.websocketregistry;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import software.xdev.mockserver.closurecallback.websocketclient.WebSocketException;
import software.xdev.mockserver.collections.CircularHashMap;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.serialization.WebSocketMessageSerializer;
import software.xdev.mockserver.serialization.model.WebSocketClientIdDTO;
import software.xdev.mockserver.serialization.model.WebSocketErrorDTO;

import java.util.Collections;
import java.util.Map;

import static software.xdev.mockserver.model.HttpResponse.response;
import static org.slf4j.event.Level.TRACE;
import static org.slf4j.event.Level.WARN;

public class WebSocketClientRegistry {

    public static final String WEB_SOCKET_CORRELATION_ID_HEADER_NAME = "WebSocketCorrelationId";
    private final MockServerLogger mockServerLogger;
    private final WebSocketMessageSerializer webSocketMessageSerializer;
    private final Map<String, Channel> clientRegistry;
    private final Map<String, WebSocketResponseCallback> responseCallbackRegistry;
    private final Map<String, WebSocketRequestCallback> forwardCallbackRegistry;

    public WebSocketClientRegistry(Configuration configuration, MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
        this.webSocketMessageSerializer = new WebSocketMessageSerializer(mockServerLogger);
        this.clientRegistry = Collections.synchronizedMap(new CircularHashMap<>(configuration.maxWebSocketExpectations()));
        this.responseCallbackRegistry = new CircularHashMap<>(configuration.maxWebSocketExpectations());
        this.forwardCallbackRegistry = new CircularHashMap<>(configuration.maxWebSocketExpectations());
    }

    public void receivedTextWebSocketFrame(TextWebSocketFrame textWebSocketFrame) {
        try {
            Object deserializedMessage = webSocketMessageSerializer.deserialize(textWebSocketFrame.text());
            if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(TRACE)
                        .setMessageFormat("received message over websocket{}")
                        .setArguments(deserializedMessage)
                );
            }
            if (deserializedMessage instanceof HttpResponse) {
                HttpResponse httpResponse = (HttpResponse) deserializedMessage;
                String firstHeader = httpResponse.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
                WebSocketResponseCallback webSocketResponseCallback = responseCallbackRegistry.get(firstHeader);
                if (webSocketResponseCallback != null) {
                    webSocketResponseCallback.handle(httpResponse);
                }
            } else if (deserializedMessage instanceof HttpRequest) {
                HttpRequest httpRequest = (HttpRequest) deserializedMessage;
                final String firstHeader = httpRequest.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
                WebSocketRequestCallback webSocketRequestCallback = forwardCallbackRegistry.get(firstHeader);
                if (webSocketRequestCallback != null) {
                    webSocketRequestCallback.handle(httpRequest);
                }
            } else if (deserializedMessage instanceof WebSocketErrorDTO) {
                WebSocketErrorDTO webSocketErrorDTO = (WebSocketErrorDTO) deserializedMessage;
                if (forwardCallbackRegistry.containsKey(webSocketErrorDTO.getWebSocketCorrelationId())) {
                    forwardCallbackRegistry
                        .get(webSocketErrorDTO.getWebSocketCorrelationId())
                        .handleError(
                            response()
                                .withStatusCode(404)
                                .withBody(webSocketErrorDTO.getMessage())
                        );
                } else if (responseCallbackRegistry.containsKey(webSocketErrorDTO.getWebSocketCorrelationId())) {
                    responseCallbackRegistry
                        .get(webSocketErrorDTO.getWebSocketCorrelationId())
                        .handle(
                            response()
                                .withStatusCode(404)
                                .withBody(webSocketErrorDTO.getMessage())
                        );
                }
            } else {
                throw new WebSocketException("Unsupported web socket message " + deserializedMessage);
            }
        } catch (Exception e) {
            throw new WebSocketException("Exception while receiving web socket message" + textWebSocketFrame.text(), e);
        }
    }

    public int size() {
        return clientRegistry.size();
    }

    public void registerClient(String clientId, ChannelHandlerContext ctx) {
        try {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(new WebSocketClientIdDTO().setClientId(clientId))));
        } catch (Exception e) {
            throw new WebSocketException("Exception while sending web socket registration client id message to client " + clientId, e);
        }
        clientRegistry.put(clientId, ctx.channel());
        if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setMessageFormat("registering client " + clientId + "")
            );
        }
    }

    public void unregisterClient(String clientId) {
        LocalCallbackRegistry.unregisterCallback(clientId);
        Channel removeChannel = clientRegistry.remove(clientId);
        if (removeChannel != null && removeChannel.isOpen()) {
            removeChannel.close();
        }
        if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setMessageFormat("unregistering client " + clientId + "")
            );
        }
    }

    public void registerResponseCallbackHandler(String webSocketCorrelationId, WebSocketResponseCallback expectationResponseCallback) {
        responseCallbackRegistry.put(webSocketCorrelationId, expectationResponseCallback);
        if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setMessageFormat("registering response callback " + webSocketCorrelationId)
            );
        }
    }

    public void unregisterResponseCallbackHandler(String webSocketCorrelationId) {
        responseCallbackRegistry.remove(webSocketCorrelationId);
        if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setMessageFormat("unregistering response callback " + webSocketCorrelationId + "")
            );
        }
    }

    public void registerForwardCallbackHandler(String webSocketCorrelationId, WebSocketRequestCallback expectationForwardCallback) {
        forwardCallbackRegistry.put(webSocketCorrelationId, expectationForwardCallback);
        if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setMessageFormat("registering forward callback " + webSocketCorrelationId)
            );
        }
    }

    public void unregisterForwardCallbackHandler(String webSocketCorrelationId) {
        forwardCallbackRegistry.remove(webSocketCorrelationId);
        if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setMessageFormat("unregistering forward callback " + webSocketCorrelationId + "")
            );
        }
    }

    public boolean sendClientMessage(String clientId, HttpRequest httpRequest, HttpResponse httpResponse) {
        try {
            if (clientRegistry.containsKey(clientId)) {
                if (httpResponse == null) {
                    if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(TRACE)
                                .setHttpRequest(httpRequest)
                                .setMessageFormat("sending message{}to client " + clientId)
                                .setArguments(httpRequest)
                        );
                    }
                    clientRegistry.get(clientId).writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(httpRequest)));
                } else {
                    HttpRequestAndHttpResponse httpRequestAndHttpResponse = new HttpRequestAndHttpResponse()
                        .withHttpRequest(httpRequest)
                        .withHttpResponse(httpResponse);
                    if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(TRACE)
                                .setHttpRequest(httpRequest)
                                .setMessageFormat("sending message{}to client " + clientId + "")
                                .setArguments(httpRequestAndHttpResponse)
                        );
                    }
                    clientRegistry.get(clientId).writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(httpRequestAndHttpResponse)));
                }
                return true;
            } else {
                if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(WARN)
                            .setHttpRequest(httpRequest)
                            .setMessageFormat("client " + clientId + " not found for request{}client registry only contains{}")
                            .setArguments(httpRequest, clientRegistry)
                    );
                }
                return false;
            }
        } catch (Exception e) {
            throw new WebSocketException("Exception while sending web socket message " + httpRequest + " to client " + clientId, e);
        }
    }

    public synchronized void reset() {
        forwardCallbackRegistry.clear();
        responseCallbackRegistry.clear();
        clientRegistry.forEach((clientId, channel) -> {
            LocalCallbackRegistry.unregisterCallback(clientId);
            channel.close();
        });
        clientRegistry.clear();
    }
}
