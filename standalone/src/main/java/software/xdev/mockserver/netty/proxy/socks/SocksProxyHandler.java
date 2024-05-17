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
package software.xdev.mockserver.netty.proxy.socks;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.LifeCycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.netty.HttpRequestHandler.setProxyingRequest;

@ChannelHandler.Sharable
public abstract class SocksProxyHandler<T> extends SimpleChannelInboundHandler<T> {
    
    private static final Logger LOG = LoggerFactory.getLogger(SocksProxyHandler.class);
    
    protected final ServerConfiguration configuration;
    protected final LifeCycle server;

    protected SocksProxyHandler(ServerConfiguration configuration, LifeCycle server) {
        super(false);
        this.configuration = configuration;
        this.server = server;
    }

    protected void forwardConnection(final ChannelHandlerContext ctx, ChannelHandler forwarder) {
        setProxyingRequest(ctx, Boolean.TRUE);

        ctx.pipeline().replace(this, null, forwarder);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (connectionClosedException(cause)) {
            LOG.error("Exception caught by SOCKS proxy handler -> closing pipeline {}", ctx.channel(), cause);
        }
        ctx.close();
    }
}
