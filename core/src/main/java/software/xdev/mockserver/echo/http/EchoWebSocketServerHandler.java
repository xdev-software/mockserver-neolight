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
package software.xdev.mockserver.echo.http;

import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import software.xdev.mockserver.codec.MockServerHttpServerCodec;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.uuid.UUIDService;
import org.slf4j.event.Level;

import java.util.List;

import static com.google.common.net.HttpHeaders.HOST;
import static software.xdev.mockserver.closurecallback.websocketclient.WebSocketClient.CLIENT_REGISTRATION_ID_HEADER;

@ChannelHandler.Sharable
public class EchoWebSocketServerHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<Boolean> CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET = AttributeKey.valueOf("CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET");
    private static final String UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI = "/_mockserver_callback_websocket";
    private final MockServerLogger mockServerLogger;
    private final List<String> registeredClients;
    private final List<Channel> websocketChannels;
    private final List<TextWebSocketFrame> textWebSocketFrames;
    private final boolean isSecure;
    private WebSocketServerHandshaker handshaker;

    EchoWebSocketServerHandler(MockServerLogger mockServerLogger, List<String> registeredClients, List<Channel> websocketChannels, List<TextWebSocketFrame> textWebSocketFrames, boolean isSecure) {
        this.mockServerLogger = mockServerLogger;
        this.registeredClients = registeredClients;
        this.websocketChannels = websocketChannels;
        this.textWebSocketFrames = textWebSocketFrames;
        this.isSecure = isSecure;
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
            (isSecure ? "wss" : "ws") + "://" + httpRequest.headers().get(HOST) + UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI,
            null,
            true,
            Integer.MAX_VALUE
        ).newHandshaker(httpRequest);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            final String clientId = httpRequest.headers().contains(CLIENT_REGISTRATION_ID_HEADER) ? httpRequest.headers().get(CLIENT_REGISTRATION_ID_HEADER) : UUIDService.getUUID();
            handshaker
                .handshake(
                    ctx.channel(),
                    httpRequest,
                    new DefaultHttpHeaders().add(CLIENT_REGISTRATION_ID_HEADER, clientId),
                    ctx.channel().newPromise()
                )
                .addListener((ChannelFutureListener) future -> {
                    ctx.pipeline().remove(MockServerHttpServerCodec.class);
                    if (MockServerLogger.isEnabled(Level.TRACE)) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(Level.TRACE)
                                .setMessageFormat("registering client " + clientId)
                        );
                    }
                    registeredClients.add(clientId);
                    websocketChannels.add(future.channel());
                    future.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                        if (MockServerLogger.isEnabled(Level.TRACE)) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(Level.TRACE)
                                    .setMessageFormat("unregistering callback for client " + clientId)
                            );
                        }
                        registeredClients.remove(clientId);
                        websocketChannels.remove(future.channel());
                    });
                });
        }
    }

    private void handleWebSocketFrame(final ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
        } else if (frame instanceof TextWebSocketFrame) {
            textWebSocketFrames.add(((TextWebSocketFrame) frame));
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
        } else {
            throw new UnsupportedOperationException(frame.getClass().getName() + " frame types not supported");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        mockServerLogger.logEvent(
            new LogEntry()
                .setLogLevel(Level.ERROR)
                .setMessageFormat("echo server caught exception")
                .setThrowable(cause)
        );
        ctx.close();
    }

}
