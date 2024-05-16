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
import software.xdev.mockserver.codec.MockServerHttpServerCodec;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.logging.LoggingHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class EchoServerInitializer extends ChannelInitializer<SocketChannel> {
    
    private static final Logger LOG = LoggerFactory.getLogger(EchoServerInitializer.class);
    
    private final Configuration configuration;
    private final EchoServer.Error error;
    private final List<TextWebSocketFrame> textWebSocketFrames;
    private final List<Channel> websocketChannels;
    private final List<String> registeredClients;

    EchoServerInitializer(Configuration configuration, EchoServer.Error error, List<String> registeredClients, List<Channel> websocketChannels, List<TextWebSocketFrame> textWebSocketFrames) {
        this.configuration = configuration;
        if (error == EchoServer.Error.CLOSE_CONNECTION) {
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

        if (LOG.isTraceEnabled()) {
            pipeline.addLast(new LoggingHandler(EchoServer.class.getName() + " <-->"));
        }
        // default to http1 without TLS
        configureHttp1Pipeline(channel, pipeline);
    }

    private void configureHttp1Pipeline(SocketChannel channel, ChannelPipeline pipeline) {
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
        pipeline.addLast(new EchoWebSocketServerHandler(registeredClients, websocketChannels, textWebSocketFrames));
        pipeline.addLast(new MockServerHttpServerCodec(configuration, channel.localAddress().getPort()));
        pipeline.addLast(new EchoServerHandler(
            error,
            channel.attr(EchoServer.LOG_FILTER).get(),
            channel.attr(EchoServer.NEXT_RESPONSE).get(),
            channel.attr(EchoServer.LAST_REQUEST).get()
        ));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Echo server caught exception", cause);
        ctx.close();
    }
}
