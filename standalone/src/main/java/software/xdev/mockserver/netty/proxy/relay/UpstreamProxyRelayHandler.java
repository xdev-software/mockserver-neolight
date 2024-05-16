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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;

import static software.xdev.mockserver.exception.ExceptionHandling.closeOnFlush;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;

public class UpstreamProxyRelayHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private static final Logger LOG = LoggerFactory.getLogger(UpstreamProxyRelayHandler.class);
    
    private final Channel downstreamChannel;

    public UpstreamProxyRelayHandler(Channel downstreamChannel) {
        super(false);
        this.downstreamChannel = downstreamChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
        ctx.write(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        downstreamChannel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.channel().read();
            } else {
                if (isNotSocketClosedException(future.cause())) {
                    LOG.error("Exception while returning response for request \"{} {}\"", request.method(), request.uri());
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
            LOG.error("Exception caught by upstream relay handler -> closing pipeline {}", ctx.channel(), cause);
        }
        closeOnFlush(ctx.channel());
    }

}
