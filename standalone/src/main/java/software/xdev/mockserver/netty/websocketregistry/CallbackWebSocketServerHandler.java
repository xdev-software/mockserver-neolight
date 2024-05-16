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
package software.xdev.mockserver.netty.websocketregistry;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.codec.MockServerHttpServerCodec;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.netty.HttpRequestHandler;
import software.xdev.mockserver.uuid.UUIDService;
import org.slf4j.event.Level;

import static com.google.common.net.HttpHeaders.HOST;
import static software.xdev.mockserver.closurecallback.websocketclient.WebSocketClient.CLIENT_REGISTRATION_ID_HEADER;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.isSslEnabledUpstream;

@ChannelHandler.Sharable
public class CallbackWebSocketServerHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<Boolean> CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET = AttributeKey.valueOf("CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET");
    private static final String UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI = "/_mockserver_callback_websocket";
    private final MockServerLogger mockServerLogger;
    private WebSocketServerHandshaker handshaker;
    private final WebSocketClientRegistry webSocketClientRegistry;

    public CallbackWebSocketServerHandler(HttpState httpStateHandler) {
        webSocketClientRegistry = httpStateHandler.getWebSocketClientRegistry();
        mockServerLogger = httpStateHandler.getMockServerLogger();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        boolean release = true;
        try {
            if (msg instanceof FullHttpRequest && ((FullHttpRequest) msg).uri().equals(UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI)) {
                upgradeChannel(ctx, (FullHttpRequest) msg);
                ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).set(true);
            } else if (ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).get() != null &&
                ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).get() &&
                msg instanceof WebSocketFrame) {
                handleWebSocketFrame(ctx, (WebSocketFrame) msg);
            } else {
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {
            if (release) {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void upgradeChannel(final ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        handshaker = new WebSocketServerHandshakerFactory(
            (isSslEnabledUpstream(ctx.channel()) ? "wss" : "ws") + "://" + httpRequest.headers().get(HOST) + UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI,
            null,
            true,
            Integer.MAX_VALUE
        ).newHandshaker(httpRequest);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            final String clientId = httpRequest.headers().contains(CLIENT_REGISTRATION_ID_HEADER) ? httpRequest.headers().get(CLIENT_REGISTRATION_ID_HEADER) : UUIDService.getUUID();
            if (LocalCallbackRegistry.responseClientExists(clientId)
                || LocalCallbackRegistry.forwardClientExists(clientId)) {
                // found locally to indicate to client
                HttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.RESET_CONTENT, ctx.channel().alloc().buffer(0));
                HttpUtil.setContentLength(res, 0);
                ctx.channel().writeAndFlush(res, ctx.channel().newPromise());
            } else {
                handshaker
                    .handshake(
                        ctx.channel(),
                        httpRequest,
                        new DefaultHttpHeaders().add(CLIENT_REGISTRATION_ID_HEADER, clientId),
                        ctx.channel().newPromise()
                    )
                    .addListener((ChannelFutureListener) future -> {
                        ctx.pipeline().remove(MockServerHttpServerCodec.class);
                        ctx.pipeline().remove(HttpRequestHandler.class);
                        if (MockServerLogger.isEnabled(Level.TRACE)) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(Level.TRACE)
                                    .setMessageFormat("registering client " + clientId)
                            );
                        }
                        webSocketClientRegistry.registerClient(clientId, ctx);
                        future.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                            if (MockServerLogger.isEnabled(Level.TRACE)) {
                                mockServerLogger.logEvent(
                                    new LogEntry()
                                        .setLogLevel(Level.TRACE)
                                        .setMessageFormat("unregistering callback for client " + clientId)
                                );
                            }
                            webSocketClientRegistry.unregisterClient(clientId);
                        });
                    });
            }
        }
    }

    private void handleWebSocketFrame(final ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
        } else if (frame instanceof TextWebSocketFrame) {
            webSocketClientRegistry.receivedTextWebSocketFrame(((TextWebSocketFrame) frame));
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
        } else {
            throw new UnsupportedOperationException(frame.getClass().getName() + " frame types not supported");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (connectionClosedException(cause)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("web socket server caught exception")
                    .setThrowable(cause)
            );
        }
        ctx.close();
    }

}
