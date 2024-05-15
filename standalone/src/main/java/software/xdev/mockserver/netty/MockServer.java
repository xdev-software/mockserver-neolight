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
package software.xdev.mockserver.netty;

import com.google.common.collect.ImmutableList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import software.xdev.mockserver.authentication.ChainedAuthenticationHandler;
import software.xdev.mockserver.authentication.jwt.JWTAuthenticationHandler;
import software.xdev.mockserver.authentication.mtls.MTLSAuthenticationHandler;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.lifecycle.ExpectationsListener;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mock.action.http.HttpActionHandler;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;
import software.xdev.mockserver.socket.tls.NettySslContextFactory;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.xdev.mockserver.configuration.Configuration.configuration;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.SERVER_CONFIGURATION;
import static software.xdev.mockserver.mock.action.http.HttpActionHandler.REMOTE_SOCKET;
import static software.xdev.mockserver.netty.HttpRequestHandler.PROXYING;
import static software.xdev.mockserver.proxyconfiguration.ProxyConfiguration.proxyConfiguration;

public class MockServer extends LifeCycle {

    private InetSocketAddress remoteSocket;

    /**
     * Start the instance using the ports provided
     *
     * @param localPorts the local port(s) to use, use 0 or no vararg values to specify any free port
     */
    public MockServer(final Integer... localPorts) {
        this(null, proxyConfiguration(configuration()), localPorts);
    }

    /**
     * Start the instance using the ports provided
     *
     * @param localPorts the local port(s) to use, use 0 or no vararg values to specify any free port
     */
    public MockServer(final Configuration configuration, final Integer... localPorts) {
        this(configuration, proxyConfiguration(configuration), localPorts);
    }

    /**
     * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional proxy
     *
     * @param proxyConfiguration the proxy configuration to send requests forwarded or proxied by MockServer via another proxy
     * @param localPorts         the local port(s) to use, use 0 or no vararg values to specify any free port
     */
    public MockServer(final ProxyConfiguration proxyConfiguration, final Integer... localPorts) {
        this(null, ImmutableList.of(proxyConfiguration), localPorts);
    }

    /**
     * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional proxy
     *
     * @param proxyConfigurations the proxy configuration to send requests forwarded or proxied by MockServer via another proxy
     * @param localPorts          the local port(s) to use, use 0 or no vararg values to specify any free port
     */
    public MockServer(final Configuration configuration, final List<ProxyConfiguration> proxyConfigurations, final Integer... localPorts) {
        super(configuration);
        createServerBootstrap(configuration, proxyConfigurations, localPorts);

        // wait to start
        getLocalPort();
    }

    /**
     * Start the instance using the ports provided
     *
     * @param remotePort the port of the remote server to connect to
     * @param remoteHost the hostname of the remote server to connect to (if null defaults to "localhost")
     * @param localPorts the local port(s) to use
     */
    public MockServer(final Integer remotePort, final String remoteHost, final Integer... localPorts) {
        this(null, proxyConfiguration(configuration()), remoteHost, remotePort, localPorts);
    }

    /**
     * Start the instance using the ports provided
     *
     * @param remotePort the port of the remote server to connect to
     * @param remoteHost the hostname of the remote server to connect to (if null defaults to "localhost")
     * @param localPorts the local port(s) to use
     */
    public MockServer(final Configuration configuration, final Integer remotePort, final String remoteHost, final Integer... localPorts) {
        this(configuration, proxyConfiguration(configuration), remoteHost, remotePort, localPorts);
    }

    /**
     * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional proxy
     *
     * @param localPorts the local port(s) to use
     * @param remoteHost the hostname of the remote server to connect to (if null defaults to "localhost")
     * @param remotePort the port of the remote server to connect to
     */
    public MockServer(final Configuration configuration, final ProxyConfiguration proxyConfiguration, String remoteHost, final Integer remotePort, final Integer... localPorts) {
        this(configuration, ImmutableList.of(proxyConfiguration), remoteHost, remotePort, localPorts);
    }

    /**
     * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional proxy
     *
     * @param localPorts the local port(s) to use
     * @param remoteHost the hostname of the remote server to connect to (if null defaults to "localhost")
     * @param remotePort the port of the remote server to connect to
     */
    public MockServer(final Configuration configuration, final List<ProxyConfiguration> proxyConfigurations, String remoteHost, final Integer remotePort, final Integer... localPorts) {
        super(configuration);
        if (remotePort == null) {
            throw new IllegalArgumentException("You must specify a remote hostname");
        }
        if (isBlank(remoteHost)) {
            remoteHost = "localhost";
        }

        remoteSocket = new InetSocketAddress(remoteHost, remotePort);
        if (proxyConfigurations != null && MockServerLogger.isEnabled(Level.INFO)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(SERVER_CONFIGURATION)
                    .setLogLevel(Level.INFO)
                    .setMessageFormat("using proxy configuration for forwarded requests:{}")
                    .setArguments(proxyConfigurations)
            );
        }
        createServerBootstrap(configuration, proxyConfigurations, localPorts);

        // wait to start
        getLocalPort();
    }

    private void createServerBootstrap(Configuration configuration, final List<ProxyConfiguration> proxyConfigurations, final Integer... localPorts) {
        if (configuration == null) {
            configuration = configuration();
        }

        List<Integer> portBindings = singletonList(0);
        if (localPorts != null && localPorts.length > 0) {
            portBindings = Arrays.asList(localPorts);
        }

        final NettySslContextFactory nettyServerSslContextFactory = new NettySslContextFactory(configuration, mockServerLogger, true);
        final NettySslContextFactory nettyClientSslContextFactory = new NettySslContextFactory(configuration, mockServerLogger, false);
        if (configuration.controlPlaneTLSMutualAuthenticationRequired() && configuration.controlPlaneJWTAuthenticationRequired()) {
            httpState.setControlPlaneAuthenticationHandler(
                new ChainedAuthenticationHandler(
                    new MTLSAuthenticationHandler(mockServerLogger, nettyServerSslContextFactory.trustCertificateChain(configuration.controlPlaneTLSMutualAuthenticationCAChain())),
                    new JWTAuthenticationHandler(mockServerLogger, configuration.controlPlaneJWTAuthenticationJWKSource())
                        .withExpectedAudience(configuration.controlPlaneJWTAuthenticationExpectedAudience())
                        .withMatchingClaims(configuration.controlPlaneJWTAuthenticationMatchingClaims())
                        .withRequiredClaims(configuration.controlPlaneJWTAuthenticationRequiredClaims())
                )
            );
        } else if (configuration.controlPlaneTLSMutualAuthenticationRequired()) {
            httpState.setControlPlaneAuthenticationHandler(
                new MTLSAuthenticationHandler(mockServerLogger, nettyServerSslContextFactory.trustCertificateChain(configuration.controlPlaneTLSMutualAuthenticationCAChain()))
            );
        } else if (configuration.controlPlaneJWTAuthenticationRequired()) {
            httpState.setControlPlaneAuthenticationHandler(
                new JWTAuthenticationHandler(mockServerLogger, configuration.controlPlaneJWTAuthenticationJWKSource())
                    .withExpectedAudience(configuration.controlPlaneJWTAuthenticationExpectedAudience())
                    .withMatchingClaims(configuration.controlPlaneJWTAuthenticationMatchingClaims())
                    .withRequiredClaims(configuration.controlPlaneJWTAuthenticationRequiredClaims())
            );
        }
        serverServerBootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
            .childHandler(new MockServerUnificationInitializer(configuration, MockServer.this, httpState, new HttpActionHandler(configuration, getEventLoopGroup(), httpState, proxyConfigurations, nettyClientSslContextFactory), nettyServerSslContextFactory))
            .childAttr(REMOTE_SOCKET, remoteSocket)
            .childAttr(PROXYING, remoteSocket != null);

        try {
            bindServerPorts(portBindings);
        } catch (Throwable throwable) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(SERVER_CONFIGURATION)
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception binding to port(s) " + portBindings)
                    .setThrowable(throwable)
            );
            stop();
            throw throwable;
        }
        startedServer(getLocalPorts());
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteSocket;
    }

    public MockServer registerListener(ExpectationsListener expectationsListener) {
        super.registerListener(expectationsListener);
        return this;
    }

}
