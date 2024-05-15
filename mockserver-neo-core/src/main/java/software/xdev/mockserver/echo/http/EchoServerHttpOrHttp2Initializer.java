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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import org.slf4j.event.Level;

import java.util.function.BiConsumer;

import static org.slf4j.event.Level.TRACE;

public class EchoServerHttpOrHttp2Initializer extends ApplicationProtocolNegotiationHandler {

    private final MockServerLogger mockServerLogger;
    private final SocketChannel channel;
    private final BiConsumer<SocketChannel, ChannelPipeline> http2Initializer;
    private final BiConsumer<SocketChannel, ChannelPipeline> http1Initializer;

    protected EchoServerHttpOrHttp2Initializer(MockServerLogger mockServerLogger, SocketChannel channel, BiConsumer<SocketChannel, ChannelPipeline> http1Initializer, BiConsumer<SocketChannel, ChannelPipeline> http2Initializer) {
        super("");
        this.mockServerLogger = mockServerLogger;
        this.channel = channel;
        this.http2Initializer = http2Initializer;
        this.http1Initializer = http1Initializer;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
        if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.TRACE)
                    .setMessageFormat("found ALPN protocol:{}")
                    .setArguments(protocol)
            );
        }
        ChannelPipeline pipeline = ctx.pipeline();
        if (pipeline.get(EchoServerHttpOrHttp2Initializer.class) != null) {
            pipeline.remove(EchoServerHttpOrHttp2Initializer.class);
        }
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            http2Initializer.accept(channel, pipeline);
        } else {
            http1Initializer.accept(channel, pipeline);
        }
    }
}
