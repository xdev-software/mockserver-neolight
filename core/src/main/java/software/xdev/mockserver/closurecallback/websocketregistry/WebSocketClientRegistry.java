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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WebSocketClientRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientRegistry.class);
    
    public static final String WEB_SOCKET_CORRELATION_ID_HEADER_NAME = "WebSocketCorrelationId";
    private final WebSocketMessageSerializer webSocketMessageSerializer;
    private final Map<String, Channel> clientRegistry;
    private final Map<String, WebSocketResponseCallback> responseCallbackRegistry;
    private final Map<String, WebSocketRequestCallback> forwardCallbackRegistry;

    public WebSocketClientRegistry(Configuration configuration) {
        this.webSocketMessageSerializer = new WebSocketMessageSerializer();
        this.clientRegistry = Collections.synchronizedMap(new CircularHashMap<>(configuration.maxWebSocketExpectations()));
        this.responseCallbackRegistry = new CircularHashMap<>(configuration.maxWebSocketExpectations());
        this.forwardCallbackRegistry = new CircularHashMap<>(configuration.maxWebSocketExpectations());
    }

    public void receivedTextWebSocketFrame(TextWebSocketFrame textWebSocketFrame) {
        try {
            Object deserializedMessage = webSocketMessageSerializer.deserialize(textWebSocketFrame.text());
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received message over websocket {}", deserializedMessage);
            }
            if (deserializedMessage instanceof HttpResponse httpResponse) {
                String firstHeader = httpResponse.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
                WebSocketResponseCallback webSocketResponseCallback = responseCallbackRegistry.get(firstHeader);
                if (webSocketResponseCallback != null) {
                    webSocketResponseCallback.handle(httpResponse);
                }
            } else if (deserializedMessage instanceof HttpRequest httpRequest) {
                final String firstHeader = httpRequest.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
                WebSocketRequestCallback webSocketRequestCallback = forwardCallbackRegistry.get(firstHeader);
                if (webSocketRequestCallback != null) {
                    webSocketRequestCallback.handle(httpRequest);
                }
            } else if (deserializedMessage instanceof WebSocketErrorDTO webSocketErrorDTO) {
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
        if (LOG.isTraceEnabled()) {
            LOG.trace("Registering client {}", clientId);
        }
    }

    public void unregisterClient(String clientId) {
        LocalCallbackRegistry.unregisterCallback(clientId);
        Channel removeChannel = clientRegistry.remove(clientId);
        if (removeChannel != null && removeChannel.isOpen()) {
            removeChannel.close();
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Unregistering client {}", clientId);
        }
    }

    public void registerResponseCallbackHandler(String webSocketCorrelationId, WebSocketResponseCallback expectationResponseCallback) {
        responseCallbackRegistry.put(webSocketCorrelationId, expectationResponseCallback);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Registering response callback {}", webSocketCorrelationId);
        }
    }

    public void unregisterResponseCallbackHandler(String webSocketCorrelationId) {
        responseCallbackRegistry.remove(webSocketCorrelationId);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Unregistering response callback {}", webSocketCorrelationId);
        }
    }

    public void registerForwardCallbackHandler(String webSocketCorrelationId, WebSocketRequestCallback expectationForwardCallback) {
        forwardCallbackRegistry.put(webSocketCorrelationId, expectationForwardCallback);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Registering forward callback {}", webSocketCorrelationId);
        }
    }

    public void unregisterForwardCallbackHandler(String webSocketCorrelationId) {
        forwardCallbackRegistry.remove(webSocketCorrelationId);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Unregistering forward callback {}", webSocketCorrelationId);
        }
    }

    public boolean sendClientMessage(String clientId, HttpRequest httpRequest, HttpResponse httpResponse) {
        try {
            if (clientRegistry.containsKey(clientId)) {
                if (httpResponse == null) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Sending message {} to client {}", httpRequest, clientId);
                    }
                    clientRegistry.get(clientId).writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(httpRequest)));
                } else {
                    HttpRequestAndHttpResponse httpRequestAndHttpResponse = new HttpRequestAndHttpResponse()
                        .withHttpRequest(httpRequest)
                        .withHttpResponse(httpResponse);
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Sending message {} to client {}", httpRequestAndHttpResponse, clientId);
                    }
                    clientRegistry.get(clientId).writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(httpRequestAndHttpResponse)));
                }
                return true;
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Client {} not found for request {} client registry only contains {}",
                        clientId,
                        httpRequest,
                        clientRegistry);
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
