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

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import software.xdev.mockserver.logging.LoggingHandler;
import software.xdev.mockserver.mappers.FullHttpResponseToMockServerHttpResponse;
import software.xdev.mockserver.model.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.UPGRADE;
import static io.netty.handler.codec.http.HttpHeaderValues.WEBSOCKET;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.closurecallback.websocketclient.WebSocketClient.CLIENT_REGISTRATION_ID_HEADER;
import static software.xdev.mockserver.closurecallback.websocketclient.WebSocketClient.REGISTRATION_FUTURE;


@SuppressWarnings("rawtypes")
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientHandler.class);
    
    private final WebSocketClient webSocketClient;
    private final WebSocketClientHandshaker handshaker;
    private final String clientId;

    WebSocketClientHandler(String clientId, InetSocketAddress serverAddress, String contextPath, WebSocketClient webSocketClient) throws URISyntaxException {
        this.clientId = clientId;
        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            new URI("ws://" + serverAddress.getHostName() + ":" + serverAddress.getPort() + cleanContextPath(contextPath) + "/_mockserver_callback_websocket"),
            WebSocketVersion.V13,
            null,
            false,
            new DefaultHttpHeaders().add(CLIENT_REGISTRATION_ID_HEADER, clientId),
            Integer.MAX_VALUE
        );
        this.webSocketClient = webSocketClient;
    }

    private String cleanContextPath(String contextPath) {
        if (isNotBlank(contextPath)) {
            return (!contextPath.startsWith("/") ? "/" : "") + contextPath;
        } else {
            return "";
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Web socket client disconnected");
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        Channel ch = ctx.channel();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Web socket client handshake handler received {}", msg);
        }
        if (msg instanceof FullHttpResponse httpResponse) {
            final CompletableFuture<String> registrationFuture = ch.attr(REGISTRATION_FUTURE).get();
            if (httpResponse.headers().contains(UPGRADE, WEBSOCKET, true) && !handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ch, httpResponse);
                registrationFuture.complete(clientId);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("web socket client {} connected", clientId);
                }
                // add extra logging
                if (LOG.isTraceEnabled()) {
                    ch.pipeline().addFirst(new LoggingHandler(WebSocketClient.class.getName() + "-first"));
                }
            } else if (httpResponse.status().equals(HttpResponseStatus.NOT_IMPLEMENTED)) {
                String message = readRequestBody(httpResponse);
                registrationFuture.completeExceptionally(new WebSocketException(message));
                if (LOG.isWarnEnabled()) {
                    LOG.warn(message);
                }
            } else if (httpResponse.status().equals(HttpResponseStatus.RESET_CONTENT)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Web socket client not required MockServer in same JVM as client");
                }
                registrationFuture.complete(clientId);
            } else {
                registrationFuture.completeExceptionally(
                    new WebSocketException("handshake failure unsupported message received "
                        + new FullHttpResponseToMockServerHttpResponse()
                        .mapFullHttpResponseToMockServerResponse(httpResponse)));
                if (LOG.isWarnEnabled()) {
                    LOG.warn("web socket client handshake handler received an unsupported FullHttpResponse message {}", msg);
                }
            }
        } else if (msg instanceof WebSocketFrame frame) {
            if (frame instanceof TextWebSocketFrame txtFrame) {
                webSocketClient.receivedTextWebSocketFrame(txtFrame);
            } else if (frame instanceof PingWebSocketFrame) {
                ctx.write(new PongWebSocketFrame(frame.content().retain()));
            } else if (frame instanceof CloseWebSocketFrame) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Web socket client received request to close");
                }
                ch.close();
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("Web socket client received an unsupported WebSocketFrame message {}", msg);
            }
        } else if (LOG.isWarnEnabled()) {
            LOG.warn("Web socket client received a message of unknown type {}", msg);
        }
    }

    private String readRequestBody(FullHttpResponse fullHttpResponse) {
        if (fullHttpResponse.content().readableBytes() > 0) {
            byte[] bodyBytes = new byte[fullHttpResponse.content().readableBytes()];
            fullHttpResponse.content().readBytes(bodyBytes);
            MediaType mediaType = MediaType.parse(fullHttpResponse.headers().get(CONTENT_TYPE));
            return new String(bodyBytes, mediaType.getCharsetOrDefault());
        }
        return "";
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Web socket client caught exception", cause);
        final CompletableFuture<String> registrationFuture = ctx.channel().attr(REGISTRATION_FUTURE).get();
        if (!registrationFuture.isDone()) {
            registrationFuture.completeExceptionally(cause);
        }
        ctx.close();
    }
}
