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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import org.slf4j.event.Level;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.netty.HttpRequestHandler.setProxyingRequest;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.disableSslDownstream;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.enableSslDownstream;

@ChannelHandler.Sharable
public abstract class SocksProxyHandler<T> extends SimpleChannelInboundHandler<T> {

    protected final Configuration configuration;
    protected final LifeCycle server;
    protected final MockServerLogger mockServerLogger;

    public SocksProxyHandler(Configuration configuration, MockServerLogger mockServerLogger, LifeCycle server) {
        super(false);
        this.configuration = configuration;
        this.server = server;
        this.mockServerLogger = mockServerLogger;
    }

    protected void forwardConnection(final ChannelHandlerContext ctx, ChannelHandler forwarder, final String addr, int port) {
        Channel channel = ctx.channel();
        setProxyingRequest(ctx, Boolean.TRUE);
        if (String.valueOf(port).endsWith("80")) {
            disableSslDownstream(channel);
        } else if (String.valueOf(port).endsWith("443")) {
            enableSslDownstream(channel);
        }

        // add Subject Alternative Name for SSL certificate
        if (isNotBlank(addr)) {
            server.getScheduler().submit(() -> configuration.addSubjectAlternativeName(addr));
        }

        ctx.pipeline().replace(this, null, forwarder);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (connectionClosedException(cause)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception caught by SOCKS proxy handler -> closing pipeline " + ctx.channel())
                    .setThrowable(cause)
            );
        }
        ctx.close();
    }
}
