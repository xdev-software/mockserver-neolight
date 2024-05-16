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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.LoggingHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.mock.action.http.HttpActionHandler.getRemoteAddress;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.*;

@ChannelHandler.Sharable
public abstract class RelayConnectHandler<T> extends SimpleChannelInboundHandler<T> {
    
    private static final Logger LOG = LoggerFactory.getLogger(RelayConnectHandler.class);
    public static final String PROXIED = "PROXIED_";
    public static final String PROXIED_SECURE = PROXIED + "SECURE_";
    public static final String PROXIED_RESPONSE = "PROXIED_RESPONSE_";
    private final Configuration configuration;
    private final LifeCycle server;
    protected final String host;
    protected final int port;

    protected RelayConnectHandler(Configuration configuration, LifeCycle server, String host, int port) {
        this.configuration = configuration;
        this.server = server;
        this.host = host;
        this.port = port;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext proxyClientCtx, final T request) {
        Bootstrap bootstrap = new Bootstrap()
            .group(proxyClientCtx.channel().eventLoop())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelActive(final ChannelHandlerContext mockServerCtx) {
                    if (isSslEnabledUpstream(proxyClientCtx.channel())) {
                        mockServerCtx.writeAndFlush(Unpooled.copiedBuffer((PROXIED_SECURE + host + ":" + port).getBytes(StandardCharsets.UTF_8))).awaitUninterruptibly();
                    } else {
                        mockServerCtx.writeAndFlush(Unpooled.copiedBuffer((PROXIED + host + ":" + port).getBytes(StandardCharsets.UTF_8))).awaitUninterruptibly();
                    }
                }

                @Override
                public void channelRead(ChannelHandlerContext mockServerCtx, Object msg) {
                    if (msg instanceof ByteBuf byteBuf) {
                        byte[] bytes = ByteBufUtil.getBytes(byteBuf);
                        if (new String(bytes, StandardCharsets.UTF_8).startsWith(PROXIED_RESPONSE)) {
                            proxyClientCtx
                                .writeAndFlush(successResponse(request))
                                .addListener((ChannelFutureListener) channelFuture -> {
                                    removeCodecSupport(proxyClientCtx);

                                    // upstream (to MockServer)
                                    ChannelPipeline pipelineToMockServer = mockServerCtx.channel().pipeline();

                                    if (LOG.isTraceEnabled()) {
                                        pipelineToMockServer.addLast(new LoggingHandler(RelayConnectHandler.class.getName() + "-downstream -->"));
                                    }

                                    pipelineToMockServer.addLast(new HttpClientCodec(configuration.maxInitialLineLength(), configuration.maxHeaderSize(), configuration.maxChunkSize()));
                                    pipelineToMockServer.addLast(new HttpContentDecompressor());
                                    pipelineToMockServer.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));

                                    pipelineToMockServer.addLast(new DownstreamProxyRelayHandler(proxyClientCtx.channel()));

                                    // downstream (to proxy client)
                                    ChannelPipeline pipelineToProxyClient = proxyClientCtx.channel().pipeline();


                                    if (LOG.isTraceEnabled()) {
                                        pipelineToProxyClient.addLast(new LoggingHandler(RelayConnectHandler.class.getName() + "-upstream <-- "));
                                    }
                                    
                                    pipelineToProxyClient.addLast(new HttpServerCodec(configuration.maxInitialLineLength(), configuration.maxHeaderSize(), configuration.maxChunkSize()));
                                    pipelineToProxyClient.addLast(new HttpContentDecompressor());
                                    pipelineToProxyClient.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));

                                    pipelineToProxyClient.addLast(new UpstreamProxyRelayHandler(mockServerCtx.channel()));
                                });
                        } else {
                            mockServerCtx.fireChannelRead(msg);
                        }
                    }
                }
            });

        final InetSocketAddress remoteSocket = getDownstreamSocket(proxyClientCtx);
        bootstrap.connect(remoteSocket).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                failure("Connection failed to " + remoteSocket, future.cause(), proxyClientCtx, failureResponse(request));
            }
        });
    }

    private InetSocketAddress getDownstreamSocket(ChannelHandlerContext ctx) {
        InetSocketAddress remoteAddress = getRemoteAddress(ctx);
        if (remoteAddress != null) {
            return remoteAddress;
        }
        return new InetSocketAddress(server.getLocalPort());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        failure("Exception caught by CONNECT proxy handler -> closing pipeline ", cause, ctx, failureResponse(null));
    }

    private void failure(String message, Throwable cause, ChannelHandlerContext ctx, Object response) {
        if (connectionClosedException(cause)) {
            LOG.error(message, cause);
        }
        Channel channel = ctx.channel();
        channel.writeAndFlush(response);
        if (channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    protected abstract void removeCodecSupport(ChannelHandlerContext ctx);

    protected abstract Object successResponse(Object request);

    protected abstract Object failureResponse(Object request);

    protected void removeHandler(ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerType) {
        if (pipeline.get(handlerType) != null) {
            pipeline.remove(handlerType);
        }
    }

    protected void removeHandler(ChannelPipeline pipeline, ChannelHandler channelHandler) {
        if (pipeline.toMap().containsValue(channelHandler)) {
            pipeline.remove(channelHandler);
        }
    }

}
