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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import software.xdev.mockserver.codec.MockServerHttpServerCodec;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.LoggingHandler;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.socket.tls.NettySslContextFactory;
import org.slf4j.event.Level;

import java.util.List;

import static software.xdev.mockserver.echo.http.EchoServer.*;
import static software.xdev.mockserver.logging.MockServerLogger.isEnabled;
import static org.slf4j.event.Level.TRACE;

public class EchoServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Configuration configuration;
    private final MockServerLogger mockServerLogger;
    private final boolean secure;
    private final EchoServer.Error error;
    private final List<TextWebSocketFrame> textWebSocketFrames;
    private final List<Channel> websocketChannels;
    private final List<String> registeredClients;
    private final SslContext sslContext;

    EchoServerInitializer(Configuration configuration, MockServerLogger mockServerLogger, boolean secure, SslContext sslContext, EchoServer.Error error, List<String> registeredClients, List<Channel> websocketChannels, List<TextWebSocketFrame> textWebSocketFrames) {
        this.configuration = configuration;
        this.mockServerLogger = mockServerLogger;
        this.secure = secure;
        this.sslContext = sslContext;
        if (!secure && error == EchoServer.Error.CLOSE_CONNECTION) {
            throw new IllegalArgumentException("Error type CLOSE_CONNECTION is not supported in non-secure mode");
        }
        this.error = error;
        this.registeredClients = registeredClients;
        this.websocketChannels = websocketChannels;
        this.textWebSocketFrames = textWebSocketFrames;
    }

    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        if (error != null) {
            pipeline.addLast(new ErrorHandler(error));
        }

        if (secure) {
            pipeline.addLast((sslContext != null ? sslContext : new NettySslContextFactory(configuration, mockServerLogger, true).createServerSslContext()).newHandler(channel.alloc()));
        }

        if (MockServerLogger.isEnabled(TRACE)) {
            pipeline.addLast(new LoggingHandler(EchoServer.class.getName() + " <-->"));
        }
        if (secure) {
            // use ALPN to determine http1 or http2
            pipeline.addLast(new EchoServerHttpOrHttp2Initializer(mockServerLogger, channel, this::configureHttp1Pipeline, this::configureHttp2Pipeline));
        } else {
            // default to http1 without TLS
            configureHttp1Pipeline(channel, pipeline);
        }
    }

    private void configureHttp1Pipeline(SocketChannel channel, ChannelPipeline pipeline) {
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
        pipeline.addLast(new EchoWebSocketServerHandler(mockServerLogger, registeredClients, websocketChannels, textWebSocketFrames, secure));
        pipeline.addLast(new MockServerHttpServerCodec(configuration, mockServerLogger, secure, null, channel.localAddress().getPort()));
        pipeline.addLast(new EchoServerHandler(
            error,
            mockServerLogger,
            channel.attr(LOG_FILTER).get(),
            channel.attr(NEXT_RESPONSE).get(),
            channel.attr(LAST_REQUEST).get()
        ));
    }

    private void configureHttp2Pipeline(SocketChannel channel, ChannelPipeline pipeline) {
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
            )
            .connection(connection);
        if (isEnabled(TRACE)) {
            http2ConnectionHandlerBuilder.frameLogger(new Http2FrameLogger(LogLevel.TRACE, EchoServerInitializer.class.getName()));
        }
        pipeline.addLast(http2ConnectionHandlerBuilder.build());
        pipeline.addLast(new EchoWebSocketServerHandler(mockServerLogger, registeredClients, websocketChannels, textWebSocketFrames, secure));
        pipeline.addLast(new MockServerHttpServerCodec(configuration, mockServerLogger, secure, null, channel.localAddress().getPort()));
        pipeline.addLast(new EchoServerHandler(
            error,
            mockServerLogger,
            channel.attr(LOG_FILTER).get(),
            channel.attr(NEXT_RESPONSE).get(),
            channel.attr(LAST_REQUEST).get()
        ));
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
