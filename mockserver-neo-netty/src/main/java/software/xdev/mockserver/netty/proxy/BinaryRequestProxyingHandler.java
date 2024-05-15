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
package software.xdev.mockserver.netty.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.BinaryMessage;
import software.xdev.mockserver.model.BinaryProxyListener;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.uuid.UUIDService;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.exception.ExceptionHandling.closeOnFlush;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.formatting.StringFormatter.formatBytes;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.FORWARDED_REQUEST;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.RECEIVED_REQUEST;
import static software.xdev.mockserver.mock.action.http.HttpActionHandler.getRemoteAddress;
import static software.xdev.mockserver.model.BinaryMessage.bytes;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.isSslEnabledUpstream;

@ChannelHandler.Sharable
public class BinaryRequestProxyingHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Configuration configuration;
    private final MockServerLogger mockServerLogger;
    private final Scheduler scheduler;
    private final NettyHttpClient httpClient;
    private final BinaryProxyListener binaryExchangeCallback;

    public BinaryRequestProxyingHandler(final Configuration configuration, final MockServerLogger mockServerLogger, final Scheduler scheduler, final NettyHttpClient httpClient) {
        super(true);
        this.configuration = configuration;
        this.mockServerLogger = mockServerLogger;
        this.scheduler = scheduler;
        this.httpClient = httpClient;
        this.binaryExchangeCallback = configuration.binaryProxyListener();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        BinaryMessage binaryRequest = bytes(ByteBufUtil.getBytes(byteBuf));
        String logCorrelationId = UUIDService.getUUID();
        mockServerLogger.logEvent(
            new LogEntry()
                .setType(RECEIVED_REQUEST)
                .setLogLevel(Level.INFO)
                .setCorrelationId(logCorrelationId)
                .setMessageFormat("received binary request:{}")
                .setArguments(ByteBufUtil.hexDump(binaryRequest.getBytes()))
        );
        final InetSocketAddress remoteAddress = getRemoteAddress(ctx);
        if (remoteAddress != null) { // binary protocol is only supported for proxies request and not mocking
            sendMessage(ctx, binaryRequest, logCorrelationId, remoteAddress);
        } else {
            if (MockServerLogger.isEnabled(Level.INFO)) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.INFO)
                        .setCorrelationId(logCorrelationId)
                        .setMessageFormat(
                            "unknown message format, only HTTP requests are supported for mocking or HTTP & binary requests for proxying, but request is not being proxied and request is not valid HTTP, found request in binary: {} in utf8 text: {}"
                        )
                        .setArguments(ByteBufUtil.hexDump(binaryRequest.getBytes()), new String(binaryRequest.getBytes(), StandardCharsets.UTF_8))
                );
            }
            ctx.writeAndFlush(Unpooled.copiedBuffer(
                "unknown message format, only HTTP requests are supported for mocking or HTTP & binary requests for proxying, but request is not being proxied and request is not valid HTTP".getBytes(StandardCharsets.UTF_8)
            ));
            ctx.close();
        }
    }

    private void sendMessage(ChannelHandlerContext ctx, BinaryMessage binaryRequest, String logCorrelationId, InetSocketAddress remoteAddress) {
        CompletableFuture<BinaryMessage> binaryResponseFuture = httpClient
            .sendRequest(
                binaryRequest,
                isSslEnabledUpstream(ctx.channel()),
                remoteAddress,
                configuration.socketConnectionTimeoutInMillis()
            );

        if (configuration.forwardBinaryRequestsWithoutWaitingForResponse()) {
            processNotWaitingForResponse(ctx, binaryRequest, logCorrelationId, remoteAddress, binaryResponseFuture);
        } else {
            processWaitingForResponse(ctx, binaryRequest, logCorrelationId, remoteAddress, binaryResponseFuture);
        }
    }

    private void processNotWaitingForResponse(ChannelHandlerContext ctx, BinaryMessage binaryRequest, String logCorrelationId, InetSocketAddress remoteAddress, CompletableFuture<BinaryMessage> binaryResponseFuture) {
        if (binaryExchangeCallback != null) {
            binaryExchangeCallback.onProxy(binaryRequest, binaryResponseFuture, remoteAddress, ctx.channel().remoteAddress());
        }
        scheduler.submit(binaryResponseFuture, () -> {
            try {
                BinaryMessage binaryResponse = binaryResponseFuture.get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
                if (binaryResponse != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(FORWARDED_REQUEST)
                            .setLogLevel(Level.INFO)
                            .setCorrelationId(logCorrelationId)
                            .setMessageFormat("returning binary response:{}from:{}for forwarded binary request:{}")
                            .setArguments(formatBytes(binaryResponse.getBytes()), remoteAddress, formatBytes(binaryRequest.getBytes()))
                    );
                    ctx.writeAndFlush(Unpooled.copiedBuffer(binaryResponse.getBytes()));
                }
            } catch (Throwable throwable) {
                if (MockServerLogger.isEnabled(Level.WARN)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.WARN)
                            .setCorrelationId(logCorrelationId)
                            .setMessageFormat("exception " + throwable.getMessage() + " sending hex{}to{}closing connection")
                            .setArguments(ByteBufUtil.hexDump(binaryRequest.getBytes()), remoteAddress)
                            .setThrowable(throwable)
                    );
                }
                ctx.close();
            }
        }, false);
    }

    private void processWaitingForResponse(ChannelHandlerContext ctx, BinaryMessage binaryRequest, String logCorrelationId, InetSocketAddress remoteAddress, CompletableFuture<BinaryMessage> binaryResponseFuture) {
        scheduler.submit(binaryResponseFuture, () -> {
            try {
                BinaryMessage binaryResponse = binaryResponseFuture.get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setType(FORWARDED_REQUEST)
                        .setLogLevel(Level.INFO)
                        .setCorrelationId(logCorrelationId)
                        .setMessageFormat("returning binary response:{}from:{}for forwarded binary request:{}")
                        .setArguments(formatBytes(binaryResponse.getBytes()), remoteAddress, formatBytes(binaryRequest.getBytes()))
                );
                if (binaryExchangeCallback != null) {
                    binaryExchangeCallback.onProxy(binaryRequest, binaryResponseFuture, remoteAddress, ctx.channel().remoteAddress());
                }
                ctx.writeAndFlush(Unpooled.copiedBuffer(binaryResponse.getBytes()));
            } catch (Throwable throwable) {
                if (MockServerLogger.isEnabled(Level.WARN)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.WARN)
                            .setCorrelationId(logCorrelationId)
                            .setMessageFormat("exception " + throwable.getMessage() + " sending hex{}to{}closing connection")
                            .setArguments(ByteBufUtil.hexDump(binaryRequest.getBytes()), remoteAddress)
                            .setThrowable(throwable)
                    );
                }
                ctx.close();
            }
        }, false);
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
                    .setMessageFormat("exception caught by " + this.getClass() + " handler -> closing pipeline " + ctx.channel())
                    .setThrowable(cause)
            );
        }
        closeOnFlush(ctx.channel());
    }
}
