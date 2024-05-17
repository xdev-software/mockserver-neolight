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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.event.EventBus;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.scheduler.SchedulerThreadFactory;
import software.xdev.mockserver.stop.Stoppable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.configuration.ServerConfiguration.configuration;


public class EchoServer implements Stoppable {
    
    private static final Logger LOG = LoggerFactory.getLogger(EchoServer.class);
    
    static final AttributeKey<EventBus> LOG_FILTER = AttributeKey.valueOf("SERVER_LOG_FILTER");
    static final AttributeKey<NextResponse> NEXT_RESPONSE = AttributeKey.valueOf("NEXT_RESPONSE");
    static final AttributeKey<LastRequest> LAST_REQUEST = AttributeKey.valueOf("LAST_REQUEST");
    
    private final ServerConfiguration configuration = configuration();
    private final Scheduler scheduler = new Scheduler(configuration);
    private final EventBus eventBus = new EventBus(configuration, scheduler, true);
    private final NextResponse nextResponse = new NextResponse();
    private final LastRequest lastRequest = new LastRequest();
    private final CompletableFuture<Integer> boundPort = new CompletableFuture<>();
    private final List<String> registeredClients;
    private final List<Channel> websocketChannels;
    private final List<TextWebSocketFrame> textWebSocketFrames;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public EchoServer() {
        this(null);
    }

    public EchoServer(final Error error) {
        registeredClients = new ArrayList<>();
        websocketChannels = new ArrayList<>();
        textWebSocketFrames = new ArrayList<>();
        new SchedulerThreadFactory("MockServer EchoServer Thread").newThread(() -> {
            bossGroup = new NioEventLoopGroup(3, new SchedulerThreadFactory(this.getClass().getSimpleName() + "-bossEventLoop"));
            workerGroup = new NioEventLoopGroup(5, new SchedulerThreadFactory(this.getClass().getSimpleName() + "-workerEventLoop"));
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 100)
                .handler(new LoggingHandler(EchoServer.class))
                .childHandler(new EchoServerInitializer(configuration, error, registeredClients, websocketChannels, textWebSocketFrames))
                .childAttr(LOG_FILTER, eventBus)
                .childAttr(NEXT_RESPONSE, nextResponse)
                .childAttr(LAST_REQUEST, lastRequest)
                .bind(0)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        boundPort.complete(((InetSocketAddress) future.channel().localAddress()).getPort());
                    } else {
                        boundPort.completeExceptionally(future.cause());
                    }
                });
        }).start();

        try {
            // wait for proxy to start all channels
            boundPort.get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
            TimeUnit.MILLISECONDS.sleep(5);
        } catch (Exception e) {
            LOG.error("Exception while waiting for proxy to complete starting up", e);
        }
    }

    public void stop() {
        scheduler.shutdown();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void close() {
        stop();
    }

    public Integer getPort() {
        try {
            return boundPort.get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public EventBus mockServerEventLog() {
        return eventBus;
    }

    public void withNextResponse(HttpResponse... httpResponses) {
        // WARNING: this logic is only for unit tests that run in series and is NOT thread safe!!!
        nextResponse.httpResponse.addAll(Arrays.asList(httpResponses));
    }

    public void clear() {
        // WARNING: this logic is only for unit tests that run in series and is NOT thread safe!!!
        nextResponse.httpResponse.clear();
        lastRequest.httpRequest.set(new CompletableFuture<>());
    }

    public HttpRequest getLastRequest() {
        try {
            HttpRequest httpRequest = lastRequest.httpRequest.get().get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
            lastRequest.httpRequest.set(new CompletableFuture<>());
            return httpRequest;
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.error("Exception while waiting to receive request", e);
            return null;
        }
    }

    public List<String> getRegisteredClients() {
        return registeredClients;
    }

    public List<Channel> getWebsocketChannels() {
        return websocketChannels;
    }

    public List<TextWebSocketFrame> getTextWebSocketFrames() {
        return textWebSocketFrames;
    }

    public enum Error {
        CLOSE_CONNECTION,
        LARGER_CONTENT_LENGTH,
        SMALLER_CONTENT_LENGTH,
        RANDOM_BYTES_RESPONSE
    }

    public static class NextResponse {
        public final Queue<HttpResponse> httpResponse = new LinkedList<>();
    }

    public static class LastRequest {
        public final AtomicReference<CompletableFuture<software.xdev.mockserver.model.HttpRequest>> httpRequest = new AtomicReference<>(new CompletableFuture<>());
    }
}
