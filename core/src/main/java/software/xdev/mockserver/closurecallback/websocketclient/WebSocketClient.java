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
package software.xdev.mockserver.closurecallback.websocketclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.LoggingHandler;
import software.xdev.mockserver.mock.action.ExpectationCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback;
import software.xdev.mockserver.model.HttpMessage;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.serialization.WebSocketMessageSerializer;
import software.xdev.mockserver.serialization.model.WebSocketClientIdDTO;
import software.xdev.mockserver.serialization.model.WebSocketErrorDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry.WEB_SOCKET_CORRELATION_ID_HEADER_NAME;
import static org.slf4j.event.Level.TRACE;
import static org.slf4j.event.Level.WARN;

@SuppressWarnings("rawtypes")
public class WebSocketClient<T extends HttpMessage> {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClient.class);
    static final AttributeKey<CompletableFuture<String>> REGISTRATION_FUTURE = AttributeKey.valueOf("REGISTRATION_FUTURE");
    private Channel channel;
    private final WebSocketMessageSerializer webSocketMessageSerializer;
    private ExpectationCallback<T> expectationCallback;
    private ExpectationForwardAndResponseCallback expectationForwardResponseCallback;
    private boolean isStopped = false;
    private final EventLoopGroup eventLoopGroup;
    private final String clientId;
    public static final String CLIENT_REGISTRATION_ID_HEADER = "X-CLIENT-REGISTRATION-ID";

    public WebSocketClient(final EventLoopGroup eventLoopGroup, final String clientId) {
        this.eventLoopGroup = eventLoopGroup;
        this.clientId = clientId;
        this.webSocketMessageSerializer = new WebSocketMessageSerializer();
    }

    private Future<String> register(final InetSocketAddress serverAddress, final String contextPath, int reconnectAttempts) {
        CompletableFuture<String> registrationFuture = new CompletableFuture<>();
        try {
            new Bootstrap()
                .group(this.eventLoopGroup)
                .channel(NioSocketChannel.class)
                .attr(REGISTRATION_FUTURE, registrationFuture)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws URISyntaxException {
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
                        ch.pipeline().addLast(new WebSocketClientHandler(clientId, serverAddress, contextPath, WebSocketClient.this));
                        // add logging
                        if (LOG.isTraceEnabled()) {
                            ch.pipeline().addLast(new LoggingHandler(WebSocketClient.class.getName() + "-last"));
                        }
                    }
                })
                .connect(serverAddress)
                .addListener((ChannelFutureListener) connectChannelFuture -> {
                    channel = connectChannelFuture.channel();
                    channel.closeFuture().addListener((ChannelFutureListener) closeChannelFuture -> {
                        if (!isStopped && reconnectAttempts > 0) {
                            // attempt to re-connect
                            register(serverAddress, contextPath, reconnectAttempts - 1);
                        }
                    });
                });

            // handle HttpResponseStatus.RESET_CONTENT

        } catch (Exception e) {
            registrationFuture.completeExceptionally(new WebSocketException("Exception while starting web socket client", e));
        }
        return registrationFuture;
    }

    void receivedTextWebSocketFrame(TextWebSocketFrame textWebSocketFrame) {
        try {
            Object deserializedMessage = webSocketMessageSerializer.deserialize(textWebSocketFrame.text());
            if (deserializedMessage instanceof HttpRequest request) {
                String webSocketCorrelationId = request.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Received request {} over websocket for client {} for correlationId {}", request, clientId, webSocketCorrelationId);
                }
                if (expectationCallback != null) {
                    try {
                        T result = expectationCallback.handle(request);
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Returning response {} for request {} "
                                    + "over websocket for client {} for correlationId {}",
                                result,
                                request,
                                clientId,
                                webSocketCorrelationId);
                        }
                        result.withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId);
                        channel.writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(result)));
                    } catch (Exception ex) {
                        LOG.error("Exception thrown while handling callback for request", ex);
                        channel.writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(
                            new WebSocketErrorDTO()
                                .setMessage(ex.getMessage())
                                .setWebSocketCorrelationId(webSocketCorrelationId)
                        )));
                    }
                }
            } else if (deserializedMessage instanceof HttpRequestAndHttpResponse httpRequestAndHttpResponse) {
                HttpRequest httpRequest = httpRequestAndHttpResponse.getHttpRequest();
                HttpResponse httpResponse = httpRequestAndHttpResponse.getHttpResponse();
                String webSocketCorrelationId = httpRequest.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Received request and response {} over websocket for client {} for correlationId {}", httpResponse, clientId, webSocketCorrelationId);
                }
                if (expectationForwardResponseCallback != null) {
                    try {
                        HttpResponse response = expectationForwardResponseCallback.handle(httpRequest, httpResponse);
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Returning response {} for request and response {} "
                                + "over websocket for client {} for correlationId {}",
                                response,
                                httpRequestAndHttpResponse,
                                clientId,
                                webSocketCorrelationId);
                        }
                        response.withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId);
                        channel.writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(response)));
                    } catch (Exception ex) {
                        LOG.error("Exception thrown while handling callback for request and response", ex);
                        channel.writeAndFlush(new TextWebSocketFrame(webSocketMessageSerializer.serialize(
                            new WebSocketErrorDTO()
                                .setMessage(ex.getMessage())
                                .setWebSocketCorrelationId(webSocketCorrelationId)
                        )));
                    }
                }
            } else if (deserializedMessage instanceof WebSocketClientIdDTO) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Received client id {}", deserializedMessage);
                }
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.trace("Web socket client received a message that isn't "
                        + "HttpRequest or HttpRequestAndHttpResponse {} which has been deserialized as {}",
                        textWebSocketFrame.text(),
                        deserializedMessage);
                }
                throw new WebSocketException("Unsupported web socket message " + textWebSocketFrame.text());
            }
        } catch (Exception e) {
            throw new WebSocketException("Exception while receiving web socket message", e);
        }
    }

    public void stopClient() {
        isStopped = true;
        try {
            if (eventLoopGroup != null && !eventLoopGroup.isShuttingDown()) {
                eventLoopGroup.shutdownGracefully();
            }
            if (channel != null && channel.isOpen()) {
                channel.close().sync();
                channel = null;
            }
        } catch (InterruptedException e) {
            throw new WebSocketException("Exception while closing client", e);
        }
    }

    public Future<String> registerExpectationCallback(
        final ExpectationCallback<T> expectationCallback,
        ExpectationForwardAndResponseCallback expectationForwardResponseCallback,
        final InetSocketAddress serverAddress,
        final String contextPath) {
        if (this.expectationCallback == null) {
            this.expectationCallback = expectationCallback;
            this.expectationForwardResponseCallback = expectationForwardResponseCallback;
            return register(serverAddress, contextPath, 3);
        } else {
            throw new IllegalArgumentException("It is not possible to set response callback once a forward callback has been set");
        }
    }
}
