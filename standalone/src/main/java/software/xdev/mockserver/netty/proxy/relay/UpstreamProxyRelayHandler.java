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
package software.xdev.mockserver.netty.proxy.relay;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.ssl.SslHandler;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import org.slf4j.event.Level;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;

import static software.xdev.mockserver.exception.ExceptionHandling.closeOnFlush;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.model.Protocol.HTTP_2;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.isSslEnabledDownstream;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.nettySslContextFactory;
import static software.xdev.mockserver.socket.tls.SniHandler.getALPNProtocol;

public class UpstreamProxyRelayHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final MockServerLogger mockServerLogger;
    private final Channel upstreamChannel;
    private final Channel downstreamChannel;

    public UpstreamProxyRelayHandler(MockServerLogger mockServerLogger, Channel upstreamChannel, Channel downstreamChannel) {
        super(false);
        this.upstreamChannel = upstreamChannel;
        this.downstreamChannel = downstreamChannel;
        this.mockServerLogger = mockServerLogger;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
        ctx.write(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        if (isSslEnabledDownstream(upstreamChannel) && downstreamChannel.pipeline().get(SslHandler.class) == null) {
            downstreamChannel.pipeline().addFirst(nettySslContextFactory(ctx.channel()).createClientSslContext(true, HTTP_2.equals(getALPNProtocol(mockServerLogger, ctx))).newHandler(ctx.alloc()));
        }
        downstreamChannel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.channel().read();
            } else {
                if (isNotSocketClosedException(future.cause())) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setMessageFormat("exception while returning response for request \"" + request.method() + " " + request.uri() + "\"")
                            .setThrowable(future.cause())
                    );
                }
                future.channel().close();
            }
        });
    }

    private boolean isNotSocketClosedException(Throwable cause) {
        return !(cause instanceof ClosedChannelException || cause instanceof ClosedSelectorException);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        closeOnFlush(downstreamChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (connectionClosedException(cause)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception caught by upstream relay handler -> closing pipeline " + ctx.channel())
                    .setThrowable(cause)
            );
        }
        closeOnFlush(ctx.channel());
    }

}
