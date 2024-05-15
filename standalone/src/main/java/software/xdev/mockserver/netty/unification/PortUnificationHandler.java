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
package software.xdev.mockserver.netty.unification;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.*;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.codec.MockServerHttpServerCodec;
import software.xdev.mockserver.codec.PreserveHeadersNettyRemoves;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.dashboard.DashboardWebSocketHandler;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.LoggingHandler;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mappers.MockServerHttpResponseToFullHttpResponse;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.mock.action.http.HttpActionHandler;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.netty.HttpRequestHandler;
import software.xdev.mockserver.netty.proxy.BinaryRequestProxyingHandler;
import software.xdev.mockserver.netty.proxy.socks.Socks4ProxyHandler;
import software.xdev.mockserver.netty.proxy.socks.Socks5ProxyHandler;
import software.xdev.mockserver.netty.proxy.socks.SocksDetector;
import software.xdev.mockserver.netty.websocketregistry.CallbackWebSocketServerHandler;
import software.xdev.mockserver.socket.tls.NettySslContextFactory;
import software.xdev.mockserver.socket.tls.SniHandler;
import org.slf4j.event.Level;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableSet;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.exception.ExceptionHandling.*;
import static software.xdev.mockserver.logging.MockServerLogger.isEnabled;
import static software.xdev.mockserver.mock.action.http.HttpActionHandler.setRemoteAddress;
import static software.xdev.mockserver.model.HttpResponse.response;
import static software.xdev.mockserver.model.Protocol.HTTP_2;
import static software.xdev.mockserver.netty.HttpRequestHandler.LOCAL_HOST_HEADERS;
import static software.xdev.mockserver.netty.HttpRequestHandler.setProxyingRequest;
import static software.xdev.mockserver.netty.proxy.relay.RelayConnectHandler.*;
import static software.xdev.mockserver.socket.tls.SniHandler.getALPNProtocol;
import static org.slf4j.event.Level.TRACE;
import static org.slf4j.event.Level.WARN;

public class PortUnificationHandler extends ReplayingDecoder<Void> {

    private static final AttributeKey<Boolean> TLS_ENABLED_UPSTREAM = AttributeKey.valueOf("TLS_ENABLED_UPSTREAM");
    private static final AttributeKey<Boolean> TLS_ENABLED_DOWNSTREAM = AttributeKey.valueOf("TLS_ENABLED_DOWNSTREAM");
    private static final AttributeKey<NettySslContextFactory> NETTY_SSL_CONTEXT_FACTORY = AttributeKey.valueOf("NETTY_SSL_CONTEXT_FACTORY");
    private static final AttributeKey<Boolean> HTTP_ENABLED = AttributeKey.valueOf("HTTP_ENABLED");
    private static final AttributeKey<Boolean> HTTP2_ENABLED = AttributeKey.valueOf("HTTP2_ENABLED");
    private static final Map<PortBinding, Set<String>> localAddressesCache = new ConcurrentHashMap<>();

    protected final MockServerLogger mockServerLogger;
    private final LoggingHandler loggingHandler = new LoggingHandler(PortUnificationHandler.class.getName() + "-first");
    private final HttpContentLengthRemover httpContentLengthRemover = new HttpContentLengthRemover();
    private final PreserveHeadersNettyRemoves preserveHeadersNettyRemoves = new PreserveHeadersNettyRemoves();
    private final Configuration configuration;
    private final LifeCycle server;
    private final HttpState httpState;
    private final HttpActionHandler actionHandler;
    private final NettySslContextFactory nettySslContextFactory;
    private final MockServerHttpResponseToFullHttpResponse mockServerHttpResponseToFullHttpResponse;

    public PortUnificationHandler(Configuration configuration, LifeCycle server, HttpState httpState, HttpActionHandler actionHandler, NettySslContextFactory nettySslContextFactory) {
        this.configuration = configuration;
        this.server = server;
        this.mockServerLogger = httpState.getMockServerLogger();
        this.httpState = httpState;
        this.actionHandler = actionHandler;
        this.nettySslContextFactory = nettySslContextFactory;
        this.mockServerHttpResponseToFullHttpResponse = new MockServerHttpResponseToFullHttpResponse(mockServerLogger);
    }

    public static NettySslContextFactory nettySslContextFactory(Channel channel) {
        if (channel.attr(NETTY_SSL_CONTEXT_FACTORY).get() != null) {
            return channel.attr(NETTY_SSL_CONTEXT_FACTORY).get();
        } else {
            throw new RuntimeException("NettySslContextFactory not yet initialised for channel " + channel);
        }
    }

    public static void enableSslUpstreamAndDownstream(Channel channel) {
        channel.attr(TLS_ENABLED_UPSTREAM).set(Boolean.TRUE);
        channel.attr(TLS_ENABLED_DOWNSTREAM).set(Boolean.TRUE);
    }

    public static boolean isSslEnabledUpstream(Channel channel) {
        if (channel.attr(TLS_ENABLED_UPSTREAM).get() != null) {
            return channel.attr(TLS_ENABLED_UPSTREAM).get();
        } else {
            return false;
        }
    }

    public static void enableSslDownstream(Channel channel) {
        channel.attr(TLS_ENABLED_DOWNSTREAM).set(Boolean.TRUE);
    }

    public static void disableSslDownstream(Channel channel) {
        channel.attr(TLS_ENABLED_DOWNSTREAM).set(Boolean.FALSE);
    }

    public static boolean isSslEnabledDownstream(Channel channel) {
        if (channel.attr(TLS_ENABLED_DOWNSTREAM).get() != null) {
            return channel.attr(TLS_ENABLED_DOWNSTREAM).get();
        } else {
            return false;
        }
    }

    public static void httpEnabled(Channel channel) {
        channel.attr(HTTP_ENABLED).set(Boolean.TRUE);
    }

    public static boolean isHttpEnabled(Channel channel) {
        if (channel.attr(HTTP_ENABLED).get() != null) {
            return channel.attr(HTTP_ENABLED).get();
        } else {
            return false;
        }
    }

    public static void http2Enabled(Channel channel) {
        channel.attr(HTTP2_ENABLED).set(Boolean.TRUE);
    }

    public static boolean isHttp2Enabled(Channel channel) {
        if (channel.attr(HTTP2_ENABLED).get() != null) {
            return channel.attr(HTTP2_ENABLED).get();
        } else {
            return false;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        ctx.channel().attr(NETTY_SSL_CONTEXT_FACTORY).set(nettySslContextFactory);
        if (SocksDetector.isSocks4(msg, actualReadableBytes())) {
            logStage(ctx, "adding SOCKS4 decoders");
            enableSocks4(ctx, msg);
        } else if (SocksDetector.isSocks5(msg, actualReadableBytes())) {
            logStage(ctx, "adding SOCKS5 decoders");
            enableSocks5(ctx, msg);
        } else if (isTls(msg)) {
            logStage(ctx, "adding TLS decoders");
            enableTls(ctx, msg);
        } else if (HTTP_2.equals(getALPNProtocol(mockServerLogger, ctx))) {
            logStage(ctx, "adding HTTP2 decoders");
            switchToHttp2(ctx, msg);
        } else if (isHttp(msg)) {
            logStage(ctx, "adding HTTP decoders");
            switchToHttp(ctx, msg);
        } else if (isProxyConnected(msg)) {
            logStage(ctx, "setting proxy connected");
            switchToProxyConnected(ctx, msg);
        } else if (configuration.assumeAllRequestsAreHttp()) {
            logStage(ctx, "adding HTTP decoders");
            switchToHttp(ctx, msg);
        } else {
            logStage(ctx, "adding binary decoder");
            switchToBinaryRequestProxying(ctx, msg);
        }

        if (isEnabled(TRACE)) {
            loggingHandler.addLoggingHandler(ctx);
        }
    }

    private void logStage(ChannelHandlerContext ctx, String message) {
        if (isEnabled(TRACE)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.TRACE)
                    .setMessageFormat(message + " for channel:{}pipeline:{}")
                    .setArguments(ctx.channel().toString(), ctx.pipeline().names())
            );
        }
    }

    private void enableSocks4(ChannelHandlerContext ctx, ByteBuf msg) {
        enableSocks(ctx, msg, new Socks4ServerDecoder(), new Socks4ProxyHandler(configuration, mockServerLogger, server), Socks4ServerEncoder.INSTANCE);
    }

    private void enableSocks5(ChannelHandlerContext ctx, ByteBuf msg) {
        enableSocks(ctx, msg, new Socks5InitialRequestDecoder(), new Socks5ProxyHandler(configuration, mockServerLogger, server), Socks5ServerEncoder.DEFAULT);
    }

    private void enableSocks(ChannelHandlerContext ctx, ByteBuf msg, ReplayingDecoder<?> socksInitialRequestDecoder, ChannelHandler... channelHandlers) {
        ChannelPipeline pipeline = ctx.pipeline();
        for (ChannelHandler channelHandler : channelHandlers) {
            if (isSslEnabledUpstream(ctx.channel())) {
                pipeline.addAfter("SslHandler#0", null, channelHandler);
            } else {
                pipeline.addFirst(channelHandler);
            }
        }
        pipeline.addFirst(socksInitialRequestDecoder);

        setProxyingRequest(ctx, Boolean.TRUE);

        // re-unify (with SOCKS5 enabled)
        ctx.pipeline().fireChannelRead(msg.readBytes(actualReadableBytes()));
    }

    private boolean isTls(ByteBuf buf) {
        return SslHandler.isEncrypted(buf);
    }

    private void enableTls(ChannelHandlerContext ctx, ByteBuf msg) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addFirst(new SniHandler(configuration, nettySslContextFactory));
        enableSslUpstreamAndDownstream(ctx.channel());

        // re-unify (with SSL enabled)
        ctx.pipeline().fireChannelRead(msg.readBytes(actualReadableBytes()));
    }

    private boolean isHttp(ByteBuf msg) {
        String method = msg.toString(msg.readerIndex(), 8, StandardCharsets.US_ASCII);
        return method.startsWith("GET ") ||
            method.startsWith("POST ") ||
            method.startsWith("PUT ") ||
            method.startsWith("HEAD ") ||
            method.startsWith("OPTIONS ") ||
            method.startsWith("PATCH ") ||
            method.startsWith("DELETE ") ||
            method.startsWith("TRACE ") ||
            method.startsWith("CONNECT ");
    }

    private void switchToHttp2(ChannelHandlerContext ctx, ByteBuf msg) {
        if (!isHttp2Enabled(ctx.channel())) {
            http2Enabled(ctx.channel());

            ChannelPipeline pipeline = ctx.pipeline();

            final Http2Connection connection = new DefaultHttp2Connection(true);
            final HttpToHttp2ConnectionHandlerBuilder http2ConnectionHandlerBuilder = new HttpToHttp2ConnectionHandlerBuilder()
                .frameListener(
                    new DelegatingDecompressorFrameListener(
                        connection,
                        new InboundHttp2ToHttpAdapterBuilder(connection)
                            .maxContentLength(Integer.MAX_VALUE)
                            .propagateSettings(true)
                            .validateHttpHeaders(false)
                            .build()
                    )
                );
            if (isEnabled(TRACE)) {
                http2ConnectionHandlerBuilder.frameLogger(new Http2FrameLogger(LogLevel.TRACE, PortUnificationHandler.class.getName()));
            }
            addLastIfNotPresent(pipeline, http2ConnectionHandlerBuilder.connection(connection).build());
            // TODO(jamesdbloom) consider Http2MultiplexHandler and test behaviour when multiple requests sent over the same connection
            addLastIfNotPresent(pipeline, new CallbackWebSocketServerHandler(httpState));
            addLastIfNotPresent(pipeline, new DashboardWebSocketHandler(httpState, isSslEnabledUpstream(ctx.channel()), false));
            addLastIfNotPresent(pipeline, new MockServerHttpServerCodec(configuration, mockServerLogger, isSslEnabledUpstream(ctx.channel()), SniHandler.retrieveClientCertificates(mockServerLogger, ctx), ctx.channel().localAddress()));
            addLastIfNotPresent(pipeline, new HttpRequestHandler(configuration, server, httpState, actionHandler));
            pipeline.remove(this);

            ctx.channel().attr(LOCAL_HOST_HEADERS).set(getLocalAddresses(ctx));

            // fire message back through pipeline
            ctx.fireChannelRead(msg.readBytes(actualReadableBytes()));
        }
    }

    private void switchToHttp(ChannelHandlerContext ctx, ByteBuf msg) {
        if (!isHttpEnabled(ctx.channel())) {
            httpEnabled(ctx.channel());

            ChannelPipeline pipeline = ctx.pipeline();

            addLastIfNotPresent(pipeline, new HttpServerCodec(
                configuration.maxInitialLineLength(),
                configuration.maxHeaderSize(),
                configuration.maxChunkSize()
            ));
            addLastIfNotPresent(pipeline, preserveHeadersNettyRemoves);
            addLastIfNotPresent(pipeline, new HttpContentDecompressor());
            addLastIfNotPresent(pipeline, httpContentLengthRemover);
            addLastIfNotPresent(pipeline, new HttpObjectAggregator(Integer.MAX_VALUE));
            if (configuration.tlsMutualAuthenticationRequired() && !isSslEnabledUpstream(ctx.channel())) {
                HttpResponse httpResponse = response()
                    .withStatusCode(426)
                    .withHeader("Upgrade", "TLS/1.2, HTTP/1.1")
                    .withHeader("Connection", "Upgrade");
                if (MockServerLogger.isEnabled(Level.INFO)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.INFO)
                            .setMessageFormat("no tls for connection:{}returning response:{}")
                            .setArguments(ctx.channel().localAddress(), httpResponse)
                    );
                }
                ctx
                    .channel()
                    .writeAndFlush(mockServerHttpResponseToFullHttpResponse
                        .mapMockServerResponseToNettyResponse(
                            // Upgrade Required
                            httpResponse
                        ).get(0)
                    )
                    .addListener((ChannelFuture future) -> future.channel().disconnect().awaitUninterruptibly());
            } else {
                addLastIfNotPresent(pipeline, new CallbackWebSocketServerHandler(httpState));
                addLastIfNotPresent(pipeline, new DashboardWebSocketHandler(httpState, isSslEnabledUpstream(ctx.channel()), false));
                addLastIfNotPresent(pipeline, new MockServerHttpServerCodec(configuration, mockServerLogger, isSslEnabledUpstream(ctx.channel()), SniHandler.retrieveClientCertificates(mockServerLogger, ctx), ctx.channel().localAddress()));
                addLastIfNotPresent(pipeline, new HttpRequestHandler(configuration, server, httpState, actionHandler));
                pipeline.remove(this);

                ctx.channel().attr(LOCAL_HOST_HEADERS).set(getLocalAddresses(ctx));

                // fire message back through pipeline
                ctx.fireChannelRead(msg.readBytes(actualReadableBytes()));
            }
        }
    }

    private boolean isProxyConnected(ByteBuf msg) {
        return msg.toString(msg.readerIndex(), 8, StandardCharsets.US_ASCII).startsWith(PROXIED);
    }

    private void switchToProxyConnected(ChannelHandlerContext ctx, ByteBuf msg) {
        String message = readMessage(msg);
        if (message.startsWith(PROXIED_SECURE)) {
            String[] hostParts = StringUtils.substringAfter(message, PROXIED_SECURE).split(":");
            int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 443;
            enableSslUpstreamAndDownstream(ctx.channel());
            setProxyingRequest(ctx, Boolean.TRUE);
            setRemoteAddress(ctx, new InetSocketAddress(hostParts[0], port));
        } else if (message.startsWith(PROXIED)) {
            String[] hostParts = StringUtils.substringAfter(message, PROXIED).split(":");
            int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 80;
            setProxyingRequest(ctx, Boolean.TRUE);
            setRemoteAddress(ctx, new InetSocketAddress(hostParts[0], port));
        }
        ctx.writeAndFlush(Unpooled.copiedBuffer((PROXIED_RESPONSE + message).getBytes(StandardCharsets.UTF_8))).awaitUninterruptibly();
    }

    private String readMessage(ByteBuf msg) {
        byte[] bytes = new byte[actualReadableBytes()];
        msg.readBytes(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private void switchToBinaryRequestProxying(ChannelHandlerContext ctx, ByteBuf msg) {
        addLastIfNotPresent(ctx.pipeline(), new BinaryRequestProxyingHandler(configuration, httpState.getMockServerLogger(), httpState.getScheduler(), actionHandler.getHttpClient()));

        // fire message back through pipeline
        ctx.fireChannelRead(msg.readBytes(actualReadableBytes()));
    }

    private Set<String> getLocalAddresses(ChannelHandlerContext ctx) {
        SocketAddress localAddress = ctx.channel().localAddress();
        Set<String> localAddresses = null;
        if (localAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) localAddress;
            String portExtension = calculatePortExtension(inetSocketAddress, isSslEnabledUpstream(ctx.channel()));
            PortBinding cacheKey = new PortBinding(inetSocketAddress, portExtension);
            localAddresses = localAddressesCache.get(cacheKey);
            if (localAddresses == null) {
                localAddresses = calculateLocalAddresses(inetSocketAddress, portExtension);
                localAddressesCache.put(cacheKey, localAddresses);
            }
        }
        return (localAddresses == null) ? Collections.emptySet() : localAddresses;
    }

    private String calculatePortExtension(InetSocketAddress inetSocketAddress, boolean sslEnabledUpstream) {
        String portExtension;
        if (((inetSocketAddress.getPort() == 443) && sslEnabledUpstream)
            || ((inetSocketAddress.getPort() == 80) && !sslEnabledUpstream)) {
            portExtension = "";
        } else {
            portExtension = ":" + inetSocketAddress.getPort();
        }
        return portExtension;
    }

    private Set<String> calculateLocalAddresses(InetSocketAddress localAddress, String portExtension) {
        InetAddress socketAddress = localAddress.getAddress();
        Set<String> localAddresses = new HashSet<>();
        localAddresses.add(socketAddress.getHostAddress() + portExtension);
        localAddresses.add(socketAddress.getCanonicalHostName() + portExtension);
        localAddresses.add(socketAddress.getHostName() + portExtension);
        localAddresses.add("localhost" + portExtension);
        localAddresses.add("127.0.0.1" + portExtension);
        return unmodifiableSet(localAddresses);
    }

    private void addLastIfNotPresent(ChannelPipeline pipeline, ChannelHandler channelHandler) {
        if (pipeline.get(channelHandler.getClass()) == null) {
            pipeline.addLast(channelHandler);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        if (connectionClosedException(throwable)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception caught by port unification handler -> closing pipeline " + ctx.channel())
                    .setThrowable(throwable)
            );
        } else if (sslHandshakeException(throwable)) {
            if (throwable.getMessage().contains("certificate_unknown") || throwable.getMessage().toLowerCase().contains("unknown_ca")) {
                if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.WARN)
                            .setMessageFormat("TLS handshake failure:" + NEW_LINE + NEW_LINE + " Client does not trust MockServer Certificate Authority for:{}See http://mock-server.com/mock_server/HTTPS_TLS.html to enable the client to trust MocksServer Certificate Authority." + NEW_LINE)
                            .setArguments(ctx.channel())
                            .setThrowable(throwable)
                    );
                }
            } else if (!throwable.getMessage().contains("close_notify during handshake")) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("TLS handshake failure while a client attempted to connect to " + ctx.channel())
                        .setThrowable(throwable)
                );
            }
        }
        closeOnFlush(ctx.channel());
    }
}
