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

import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.proxy.Socks5ProxyHandler;
import software.xdev.mockserver.codec.LimitedHttpContentDecompressor;
import software.xdev.mockserver.codec.MockServerBinaryClientCodec;
import software.xdev.mockserver.codec.MockServerHttpClientCodec;
import software.xdev.mockserver.logging.LoggingHandler;
import software.xdev.mockserver.model.Protocol;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;


@ChannelHandler.Sharable
public class HttpClientInitializer extends ChannelInitializer<SocketChannel>
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpClientInitializer.class);
	
	private final Protocol httpProtocol;
	private final HttpClientConnectionErrorHandler httpClientConnectionHandler;
	private final CompletableFuture<Protocol> protocolFuture;
	private final HttpClientHandler httpClientHandler;
	private final Map<ProxyConfiguration.Type, ProxyConfiguration> proxyConfigurations;
	
	HttpClientInitializer(
		final Map<ProxyConfiguration.Type, ProxyConfiguration> proxyConfigurations,
		final Protocol httpProtocol)
	{
		this.proxyConfigurations = proxyConfigurations;
		this.httpProtocol = httpProtocol;
		this.protocolFuture = new CompletableFuture<>();
		this.httpClientHandler = new HttpClientHandler();
		this.httpClientConnectionHandler = new HttpClientConnectionErrorHandler();
	}
	
	public void whenComplete(final BiConsumer<? super Protocol, ? super Throwable> action)
	{
		this.protocolFuture.whenComplete(action);
	}
	
	@Override
	public void initChannel(final SocketChannel channel)
	{
		final ChannelPipeline pipeline = channel.pipeline();
		
		if(this.proxyConfigurations != null && this.proxyConfigurations.containsKey(ProxyConfiguration.Type.SOCKS5))
		{
			final ProxyConfiguration proxyConfiguration =
				this.proxyConfigurations.get(ProxyConfiguration.Type.SOCKS5);
			if(isNotBlank(proxyConfiguration.getUsername()) && isNotBlank(proxyConfiguration.getPassword()))
			{
				pipeline.addLast(new Socks5ProxyHandler(
					proxyConfiguration.getProxyAddress(),
					proxyConfiguration.getUsername(),
					proxyConfiguration.getPassword()));
			}
			else
			{
				pipeline.addLast(new Socks5ProxyHandler(proxyConfiguration.getProxyAddress()));
			}
		}
		
		pipeline.addLast(this.httpClientConnectionHandler);
		
		// add logging
		if(LOG.isTraceEnabled())
		{
			pipeline.addLast(new LoggingHandler(HttpClientHandler.class.getName()));
		}
		
		if(this.httpProtocol == null)
		{
			this.configureBinaryPipeline(pipeline);
		}
		else
		{
			// default to http1 without TLS
			this.configureHttp1Pipeline(pipeline);
		}
	}
	
	private void configureHttp1Pipeline(final ChannelPipeline pipeline)
	{
		pipeline.addLast(new HttpClientCodec());
		pipeline.addLast(new LimitedHttpContentDecompressor());
		pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
		pipeline.addLast(new MockServerHttpClientCodec(this.proxyConfigurations));
		pipeline.addLast(this.httpClientHandler);
		this.protocolFuture.complete(Protocol.HTTP_1_1);
	}
	
	private void configureBinaryPipeline(final ChannelPipeline pipeline)
	{
		pipeline.addLast(new MockServerBinaryClientCodec());
		pipeline.addLast(this.httpClientHandler);
		this.protocolFuture.complete(null);
	}
}
