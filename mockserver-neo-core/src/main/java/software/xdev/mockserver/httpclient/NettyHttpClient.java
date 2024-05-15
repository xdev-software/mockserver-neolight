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
package software.xdev.mockserver.httpclient;

import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.filters.HopByHopHeaderFilter;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;
import software.xdev.mockserver.socket.tls.NettySslContextFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.xdev.mockserver.model.HttpResponse.response;

public class NettyHttpClient {

    static final AttributeKey<Boolean> SECURE = AttributeKey.valueOf("SECURE");
    static final AttributeKey<InetSocketAddress> REMOTE_SOCKET = AttributeKey.valueOf("REMOTE_SOCKET");
    static final AttributeKey<CompletableFuture<Message>> RESPONSE_FUTURE = AttributeKey.valueOf("RESPONSE_FUTURE");
    static final AttributeKey<Boolean> ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE = AttributeKey.valueOf("ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE");
    private static final HopByHopHeaderFilter hopByHopHeaderFilter = new HopByHopHeaderFilter();
    private final Configuration configuration;
    private final MockServerLogger mockServerLogger;
    private final EventLoopGroup eventLoopGroup;
    private final Map<ProxyConfiguration.Type, ProxyConfiguration> proxyConfigurations;
    private final boolean forwardProxyClient;
    private final NettySslContextFactory nettySslContextFactory;

    public NettyHttpClient(Configuration configuration, MockServerLogger mockServerLogger, EventLoopGroup eventLoopGroup, List<ProxyConfiguration> proxyConfigurations, boolean forwardProxyClient) {
        this(configuration, mockServerLogger, eventLoopGroup, proxyConfigurations, forwardProxyClient, new NettySslContextFactory(configuration, mockServerLogger, false));
    }

    public NettyHttpClient(Configuration configuration, MockServerLogger mockServerLogger, EventLoopGroup eventLoopGroup, List<ProxyConfiguration> proxyConfigurations, boolean forwardProxyClient, NettySslContextFactory nettySslContextFactory) {
        this.configuration = configuration;
        this.mockServerLogger = mockServerLogger;
        this.eventLoopGroup = eventLoopGroup;
        this.proxyConfigurations = proxyConfigurations != null ? proxyConfigurations.stream().collect(Collectors.toMap(ProxyConfiguration::getType, proxyConfiguration -> proxyConfiguration)) : ImmutableMap.of();
        this.forwardProxyClient = forwardProxyClient;
        this.nettySslContextFactory = nettySslContextFactory;
    }

    public CompletableFuture<HttpResponse> sendRequest(final HttpRequest httpRequest) throws SocketConnectionException {
        return sendRequest(httpRequest, httpRequest.socketAddressFromHostHeader());
    }

    public CompletableFuture<HttpResponse> sendRequest(final HttpRequest httpRequest, InetSocketAddress remoteAddress) throws SocketConnectionException {
        return sendRequest(httpRequest, remoteAddress, configuration.socketConnectionTimeoutInMillis());
    }

    public CompletableFuture<HttpResponse> sendRequest(final HttpRequest httpRequest, InetSocketAddress remoteAddress, Long connectionTimeoutMillis) throws SocketConnectionException {
        if (!eventLoopGroup.isShuttingDown()) {
            if (proxyConfigurations != null && !Boolean.TRUE.equals(httpRequest.isSecure())
                && proxyConfigurations.containsKey(ProxyConfiguration.Type.HTTP)
                && isHostNotOnNoProxyHostList(remoteAddress)) {
                ProxyConfiguration proxyConfiguration = proxyConfigurations.get(ProxyConfiguration.Type.HTTP);
                remoteAddress = proxyConfiguration.getProxyAddress();
                proxyConfiguration.addProxyAuthenticationHeader(httpRequest);
            } else if (remoteAddress == null) {
                remoteAddress = httpRequest.socketAddressFromHostHeader();
            }
            if (Protocol.HTTP_2.equals(httpRequest.getProtocol()) && !Boolean.TRUE.equals(httpRequest.isSecure())) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.WARN)
                        .setMessageFormat("HTTP2 requires ALPN but request is not secure (i.e. TLS) so protocol changed to HTTP1")
                );
                httpRequest.withProtocol(Protocol.HTTP_1_1);
            }

            final CompletableFuture<HttpResponse> httpResponseFuture = new CompletableFuture<>();
            final CompletableFuture<Message> responseFuture = new CompletableFuture<>();
            final Protocol httpProtocol = httpRequest.getProtocol() != null ? httpRequest.getProtocol() : Protocol.HTTP_1_1;

            final HttpClientInitializer clientInitializer = new HttpClientInitializer(proxyConfigurations, mockServerLogger, forwardProxyClient, nettySslContextFactory, httpProtocol);

            new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis != null ? connectionTimeoutMillis.intValue() : null)
                .attr(SECURE, httpRequest.isSecure() != null && httpRequest.isSecure())
                .attr(REMOTE_SOCKET, remoteAddress)
                .attr(RESPONSE_FUTURE, responseFuture)
                .attr(ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE, true)
                .handler(clientInitializer)
                .connect(remoteAddress)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        // ensure if HTTP2 is used then settings have been received from server
                        clientInitializer.whenComplete((protocol, throwable) -> {
                            if (throwable != null) {
                                httpResponseFuture.completeExceptionally(throwable);
                            } else {
                                // send the HTTP request
                                future.channel().writeAndFlush(httpRequest);
                            }
                        });
                    } else {
                        httpResponseFuture.completeExceptionally(future.cause());
                    }
                });

            responseFuture
                .whenComplete((message, throwable) -> {
                    if (throwable == null) {
                        if (message != null) {
                            if (forwardProxyClient) {
                                httpResponseFuture.complete(hopByHopHeaderFilter.onResponse((HttpResponse) message));
                            } else {
                                httpResponseFuture.complete((HttpResponse) message);
                            }
                        } else {
                            httpResponseFuture.complete(response());
                        }
                    } else {
                        httpResponseFuture.completeExceptionally(throwable);
                    }
                });

            return httpResponseFuture;
        } else {
            throw new IllegalStateException("Request sent after client has been stopped - the event loop has been shutdown so it is not possible to send a request");
        }
    }

    public CompletableFuture<BinaryMessage> sendRequest(final BinaryMessage binaryRequest, final boolean isSecure, InetSocketAddress remoteAddress, Long connectionTimeoutMillis) throws SocketConnectionException {
        if (!eventLoopGroup.isShuttingDown()) {
            if (proxyConfigurations != null && !isSecure && proxyConfigurations.containsKey(ProxyConfiguration.Type.HTTP)) {
                remoteAddress = proxyConfigurations.get(ProxyConfiguration.Type.HTTP).getProxyAddress();
            } else if (remoteAddress == null) {
                throw new IllegalArgumentException("Remote address cannot be null");
            }

            final CompletableFuture<BinaryMessage> binaryResponseFuture = new CompletableFuture<>();
            final CompletableFuture<Message> responseFuture = new CompletableFuture<>();

            new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis != null ? connectionTimeoutMillis.intValue() : null)
                .attr(SECURE, isSecure)
                .attr(REMOTE_SOCKET, remoteAddress)
                .attr(RESPONSE_FUTURE, responseFuture)
                .attr(ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE, !configuration.forwardBinaryRequestsWithoutWaitingForResponse())
                .handler(new HttpClientInitializer(proxyConfigurations, mockServerLogger, forwardProxyClient, nettySslContextFactory, null))
                .connect(remoteAddress)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        if (MockServerLogger.isEnabled(Level.DEBUG)) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(Level.DEBUG)
                                    .setMessageFormat("sending bytes hex{}to{}")
                                    .setArguments(ByteBufUtil.hexDump(binaryRequest.getBytes()), future.channel().attr(REMOTE_SOCKET).get())
                            );
                        }
                        // send the binary request
                        future.channel().writeAndFlush(Unpooled.copiedBuffer(binaryRequest.getBytes()));
                    } else {
                        binaryResponseFuture.completeExceptionally(future.cause());
                    }
                });

            responseFuture
                .whenComplete((message, throwable) -> {
                    if (throwable == null) {
                        binaryResponseFuture.complete((BinaryMessage) message);
                    } else {
                        throwable.printStackTrace();
                        binaryResponseFuture.completeExceptionally(throwable);
                    }
                });

            return binaryResponseFuture;
        } else {
            throw new IllegalStateException("Request sent after client has been stopped - the event loop has been shutdown so it is not possible to send a request");
        }
    }

    public HttpResponse sendRequest(HttpRequest httpRequest, long timeout, TimeUnit unit, boolean ignoreErrors) {
        HttpResponse httpResponse = null;
        try {
            httpResponse = sendRequest(httpRequest).get(timeout, unit);
        } catch (TimeoutException e) {
            if (!ignoreErrors) {
                throw new SocketCommunicationException("Response was not received from MockServer after " + configuration.maxSocketTimeoutInMillis() + " milliseconds, to wait longer please use \"mockserver.maxSocketTimeout\" system property or ConfigurationProperties.maxSocketTimeout(long milliseconds)", e.getCause());
            }
        } catch (InterruptedException | ExecutionException ex) {
            if (!ignoreErrors) {
                Throwable cause = ex.getCause();
                if (cause instanceof SocketConnectionException) {
                    throw (SocketConnectionException) cause;
                } else if (cause instanceof ConnectException) {
                    throw new SocketConnectionException("Unable to connect to socket " + httpRequest.socketAddressFromHostHeader(), cause);
                } else if (cause instanceof UnknownHostException) {
                    throw new SocketConnectionException("Unable to resolve host " + httpRequest.socketAddressFromHostHeader(), cause);
                } else if (cause instanceof IOException) {
                    throw new SocketConnectionException(cause.getMessage(), cause);
                } else {
                    throw new RuntimeException("Exception while sending request - " + ex.getMessage(), ex);
                }
            }
        }
        return httpResponse;
    }

    public HttpResponse sendRequest(HttpRequest httpRequest, long timeout, TimeUnit unit) {
        return sendRequest(httpRequest, timeout, unit, false);
    }

    private boolean isHostNotOnNoProxyHostList(InetSocketAddress remoteAddress) {
        if (remoteAddress == null
            || StringUtils.isBlank(configuration.noProxyHosts())) {
            return true;
        }
        return Stream.of(configuration.noProxyHosts().split(","))
            .map(String::trim)
            .map(host -> {
                try {
                    return InetAddress.getByName(host);
                } catch (UnknownHostException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .noneMatch(remoteAddress.getAddress()::equals);
    }
}
