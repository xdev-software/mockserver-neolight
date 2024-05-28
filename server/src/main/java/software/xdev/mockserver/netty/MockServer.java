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

import static java.util.Collections.singletonList;
import static software.xdev.mockserver.configuration.ServerConfiguration.configuration;
import static software.xdev.mockserver.mock.action.http.HttpActionHandler.REMOTE_SOCKET;
import static software.xdev.mockserver.netty.HttpRequestHandler.PROXYING;
import static software.xdev.mockserver.proxyconfiguration.ProxyConfiguration.proxyConfiguration;
import static software.xdev.mockserver.util.StringUtils.isBlank;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.ExpectationsListener;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.mock.action.http.HttpActionHandler;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;


public class MockServer extends LifeCycle
{
	private static final Logger LOG = LoggerFactory.getLogger(MockServer.class);
	
	private InetSocketAddress remoteSocket;
	
	/**
	 * Start the instance using the ports provided
	 *
	 * @param localPorts the local port(s) to use, use 0 or no vararg values to specify any free port
	 */
	public MockServer(final Integer... localPorts)
	{
		this(null, proxyConfiguration(configuration()), localPorts);
	}
	
	/**
	 * Start the instance using the ports provided
	 *
	 * @param localPorts the local port(s) to use, use 0 or no vararg values to specify any free port
	 */
	public MockServer(final ServerConfiguration configuration, final Integer... localPorts)
	{
		this(configuration, proxyConfiguration(configuration), localPorts);
	}
	
	/**
	 * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional
	 * proxy
	 *
	 * @param proxyConfiguration the proxy configuration to send requests forwarded or proxied by MockServer via
	 *                              another
	 *                           proxy
	 * @param localPorts         the local port(s) to use, use 0 or no vararg values to specify any free port
	 */
	public MockServer(final ProxyConfiguration proxyConfiguration, final Integer... localPorts)
	{
		this(null, List.of(proxyConfiguration), localPorts);
	}
	
	/**
	 * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional
	 * proxy
	 *
	 * @param proxyConfigurations the proxy configuration to send requests forwarded or proxied by MockServer via
	 *                            another proxy
	 * @param localPorts          the local port(s) to use, use 0 or no vararg values to specify any free port
	 */
	public MockServer(
		final ServerConfiguration configuration,
		final List<ProxyConfiguration> proxyConfigurations,
		final Integer... localPorts)
	{
		super(configuration);
		this.createServerBootstrap(configuration, proxyConfigurations, localPorts);
		
		// wait to start
		this.getLocalPort();
	}
	
	/**
	 * Start the instance using the ports provided
	 *
	 * @param remotePort the port of the remote server to connect to
	 * @param remoteHost the hostname of the remote server to connect to (if null defaults to "localhost")
	 * @param localPorts the local port(s) to use
	 */
	public MockServer(final Integer remotePort, final String remoteHost, final Integer... localPorts)
	{
		this(null, proxyConfiguration(configuration()), remoteHost, remotePort, localPorts);
	}
	
	/**
	 * Start the instance using the ports provided
	 *
	 * @param remotePort the port of the remote server to connect to
	 * @param remoteHost the hostname of the remote server to connect to (if null defaults to "localhost")
	 * @param localPorts the local port(s) to use
	 */
	public MockServer(
		final ServerConfiguration configuration,
		final Integer remotePort,
		final String remoteHost,
		final Integer... localPorts)
	{
		this(configuration, proxyConfiguration(configuration), remoteHost, remotePort, localPorts);
	}
	
	/**
	 * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional
	 * proxy
	 *
	 * @param localPorts the local port(s) to use
	 * @param remoteHost the hostname of the remote server to connect to (if null defaults to "localhost")
	 * @param remotePort the port of the remote server to connect to
	 */
	public MockServer(
		final ServerConfiguration configuration,
		final ProxyConfiguration proxyConfiguration,
		final String remoteHost,
		final Integer remotePort,
		final Integer... localPorts)
	{
		this(configuration, List.of(proxyConfiguration), remoteHost, remotePort, localPorts);
	}
	
	/**
	 * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional
	 * proxy
	 *
	 * @param localPorts the local port(s) to use
	 * @param remoteHost the hostname of the remote server to connect to (if null defaults to "localhost")
	 * @param remotePort the port of the remote server to connect to
	 */
	public MockServer(
		final ServerConfiguration configuration,
		final List<ProxyConfiguration> proxyConfigurations,
		final String remoteHost,
		final Integer remotePort,
		final Integer... localPorts)
	{
		super(configuration);
		if(remotePort == null)
		{
			throw new IllegalArgumentException("You must specify a remote hostname");
		}
		
		this.remoteSocket = new InetSocketAddress(isBlank(remoteHost) ? remoteHost : "localhost", remotePort);
		if(proxyConfigurations != null && LOG.isInfoEnabled())
		{
			LOG.info("Using proxy configuration for forwarded requests: {}", proxyConfigurations);
		}
		this.createServerBootstrap(configuration, proxyConfigurations, localPorts);
		
		// wait to start
		this.getLocalPort();
	}
	
	@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:FinalParameters"})
	private void createServerBootstrap(
		ServerConfiguration configuration,
		final List<ProxyConfiguration> proxyConfigurations,
		final Integer... localPorts)
	{
		if(configuration == null)
		{
			configuration = configuration();
		}
		
		List<Integer> portBindings = singletonList(0);
		if(localPorts != null && localPorts.length > 0)
		{
			portBindings = Arrays.asList(localPorts);
		}
		
		this.serverServerBootstrap = new ServerBootstrap()
			.group(this.bossGroup, this.workerGroup)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.channel(NioServerSocketChannel.class)
			.childOption(ChannelOption.AUTO_READ, true)
			.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
			.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
			.childHandler(
				new MockServerUnificationInitializer(
					configuration,
					MockServer.this,
					this.httpState,
					new HttpActionHandler(
						configuration,
						this.getEventLoopGroup(),
						this.httpState,
						proxyConfigurations)))
			.childAttr(REMOTE_SOCKET, this.remoteSocket)
			.childAttr(PROXYING, this.remoteSocket != null);
		
		try
		{
			this.bindServerPorts(portBindings);
		}
		catch(final Exception ex)
		{
			LOG.error("Exception binding to port(s) {}", portBindings);
			this.stop();
			throw ex;
		}
		this.startedServer(this.getLocalPorts());
	}
	
	public InetSocketAddress getRemoteAddress()
	{
		return this.remoteSocket;
	}
	
	@Override
	public MockServer registerListener(final ExpectationsListener expectationsListener)
	{
		super.registerListener(expectationsListener);
		return this;
	}
}
