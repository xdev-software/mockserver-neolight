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
package software.xdev.mockserver.lifecycle;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.mock.listeners.MockServerMatcherNotifier;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.scheduler.SchedulerThreadFactory;
import software.xdev.mockserver.stop.Stoppable;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.configuration.ServerConfiguration.configuration;
import static software.xdev.mockserver.mock.HttpState.setPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class LifeCycle implements Stoppable {
    
    private static final Logger LOG = LoggerFactory.getLogger(LifeCycle.class);
    protected final EventLoopGroup bossGroup;
    protected final EventLoopGroup workerGroup;
    protected final HttpState httpState;
    private final ServerConfiguration configuration;
    protected ServerBootstrap serverServerBootstrap;
    private final List<Future<Channel>> serverChannelFutures = new ArrayList<>();
    private final CompletableFuture<String> stopFuture = new CompletableFuture<>();
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final Scheduler scheduler;

    protected LifeCycle(ServerConfiguration configuration) {
        this.configuration = configuration != null ? configuration : configuration();
        this.bossGroup = new NioEventLoopGroup(5, new SchedulerThreadFactory(this.getClass().getSimpleName() + "-bossEventLoop"));
        this.workerGroup = new NioEventLoopGroup(this.configuration.nioEventLoopThreadCount(), new SchedulerThreadFactory(this.getClass().getSimpleName() + "-workerEventLoop"));
        this.scheduler = new Scheduler(this.configuration);
        this.httpState = new HttpState(this.configuration, this.scheduler);
    }

    public CompletableFuture<String> stopAsync() {
        if (!stopFuture.isDone() && stopping.compareAndSet(false, true)) {
            final String message = "stopped for port" + (getLocalPorts().size() == 1 ? ": " + getLocalPorts().get(0) : "s: " + getLocalPorts());
            if (LOG.isInfoEnabled()) {
                LOG.info(message);
            }
            new SchedulerThreadFactory("Stop").newThread(() -> {
                List<ChannelFuture> collect = serverChannelFutures
                    .stream()
                    .flatMap(channelFuture -> {
                        try {
                            return Stream.of(channelFuture.get());
                        } catch (Exception ex) {
                            // ignore
                            return Stream.empty();
                        }
                    })
                    .map(ChannelOutboundInvoker::disconnect)
                    .collect(Collectors.toList());
                try {
                    for (ChannelFuture channelFuture : collect) {
                        channelFuture.get();
                    }
                } catch (Exception ex) {
                    // ignore
                }

                httpState.stop();
                scheduler.shutdown();

                // Shut down all event loops to terminate all threads.
                bossGroup.shutdownGracefully(5, 5, MILLISECONDS);
                workerGroup.shutdownGracefully(5, 5, MILLISECONDS);

                // Wait until all threads are terminated.
                bossGroup.terminationFuture().syncUninterruptibly();
                workerGroup.terminationFuture().syncUninterruptibly();

                stopFuture.complete(message);
            }).start();
        }
        return stopFuture;
    }

    public void stop() {
        try {
            stopAsync().get(10, SECONDS);
        } catch (Exception ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception while stopping", ex);
            }
        }
    }

    @Override
    public void close() {
        stop();
    }

    protected EventLoopGroup getEventLoopGroup() {
        return workerGroup;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public boolean isRunning() {
        return !bossGroup.isShuttingDown() || !workerGroup.isShuttingDown();
    }

    public List<Integer> getLocalPorts() {
        return getBoundPorts(serverChannelFutures);
    }

    public int getLocalPort() {
        return getFirstBoundPort(serverChannelFutures);
    }

    private Integer getFirstBoundPort(List<Future<Channel>> channelFutures) {
        for (Future<Channel> channelOpened : channelFutures) {
            try {
                return ((InetSocketAddress) channelOpened.get(15, SECONDS).localAddress()).getPort();
            } catch (Exception ex) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Exception while retrieving port from channel future, ignoring port for this channel", ex);
                }
            }
        }
        return -1;
    }

    private List<Integer> getBoundPorts(List<Future<Channel>> channelFutures) {
        List<Integer> ports = new ArrayList<>();
        for (Future<Channel> channelOpened : channelFutures) {
            try {
                ports.add(((InetSocketAddress) channelOpened.get(3, SECONDS).localAddress()).getPort());
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Exception while retrieving port from channel future, ignoring port for this channel", e);
                }
            }
        }
        return ports;
    }

    public List<Integer> bindServerPorts(final List<Integer> requestedPortBindings) {
        return bindPorts(serverServerBootstrap, requestedPortBindings, serverChannelFutures);
    }

    private List<Integer> bindPorts(final ServerBootstrap serverBootstrap, List<Integer> requestedPortBindings, List<Future<Channel>> channelFutures) {
        List<Integer> actualPortBindings = new ArrayList<>();
        final String localBoundIP = configuration.localBoundIP();
        for (final Integer portToBind : requestedPortBindings) {
            try {
                final CompletableFuture<Channel> channelOpened = new CompletableFuture<>();
                channelFutures.add(channelOpened);
                new SchedulerThreadFactory("MockServer thread for port: " + portToBind, false).newThread(() -> {
                    try {
                        InetSocketAddress inetSocketAddress;
                        if (isBlank(localBoundIP)) {
                            inetSocketAddress = new InetSocketAddress(portToBind);
                        } else {
                            inetSocketAddress = new InetSocketAddress(localBoundIP, portToBind);
                        }
                        serverBootstrap
                            .bind(inetSocketAddress)
                            .addListener((ChannelFutureListener) future -> {
                                if (future.isSuccess()) {
                                    channelOpened.complete(future.channel());
                                } else {
                                    channelOpened.completeExceptionally(future.cause());
                                }
                            })
                            .channel().closeFuture().syncUninterruptibly();

                    } catch (Exception e) {
                        channelOpened.completeExceptionally(new RuntimeException("Exception while binding MockServer to port " + portToBind, e));
                    }
                }).start();

                actualPortBindings.add(((InetSocketAddress) channelOpened.get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS).localAddress()).getPort());
            } catch (Exception e) {
                throw new RuntimeException("Exception while binding MockServer to port " + portToBind, e instanceof ExecutionException ? e.getCause() : e);
            }
        }
        return actualPortBindings;
    }

    protected void startedServer(List<Integer> ports) {
        setPort(ports);
        if (LOG.isInfoEnabled()) {
            LOG.info("started on port {}",
                ports.size() == 1 ? ": " + ports.get(0) : "s: " + ports);
        }
    }

    public LifeCycle registerListener(ExpectationsListener expectationsListener) {
        httpState.getRequestMatchers().registerListener((requestMatchers, cause) -> {
            if (cause == MockServerMatcherNotifier.Cause.API) {
                expectationsListener.updated(requestMatchers.retrieveActiveExpectations(null));
            }
        });
        return this;
    }

}
