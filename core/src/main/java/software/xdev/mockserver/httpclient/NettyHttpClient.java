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

import static software.xdev.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.filters.HopByHopHeaderFilter;
import software.xdev.mockserver.model.BinaryMessage;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.Message;
import software.xdev.mockserver.model.Protocol;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;


public class NettyHttpClient
{
	private static final Logger LOG = LoggerFactory.getLogger(NettyHttpClient.class);
	
	static final AttributeKey<InetSocketAddress> REMOTE_SOCKET = AttributeKey.valueOf("REMOTE_SOCKET");
	static final AttributeKey<CompletableFuture<Message>> RESPONSE_FUTURE = AttributeKey.valueOf("RESPONSE_FUTURE");
	static final AttributeKey<Boolean> ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE =
		AttributeKey.valueOf("ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE");
	private static final HopByHopHeaderFilter HOP_BY_HOP_HEADER_FILTER = new HopByHopHeaderFilter();
	private final Configuration configuration;
	private final EventLoopGroup eventLoopGroup;
	private final Map<ProxyConfiguration.Type, ProxyConfiguration> proxyConfigurations;
	private final boolean forwardProxyClient;
	
	public NettyHttpClient(
		final Configuration configuration,
		final EventLoopGroup eventLoopGroup,
		final List<ProxyConfiguration> proxyConfigurations,
		final boolean forwardProxyClient)
	{
		this.configuration = configuration;
		this.eventLoopGroup = eventLoopGroup;
		this.proxyConfigurations = proxyConfigurations != null
			? proxyConfigurations.stream()
			.collect(Collectors.toMap(ProxyConfiguration::getType, proxyConfiguration -> proxyConfiguration))
			: Map.of();
		this.forwardProxyClient = forwardProxyClient;
	}
	
	public CompletableFuture<HttpResponse> sendRequest(final HttpRequest httpRequest)
	{
		return this.sendRequest(httpRequest, httpRequest.socketAddressFromHostHeader());
	}
	
	public CompletableFuture<HttpResponse> sendRequest(
		final HttpRequest httpRequest,
		final InetSocketAddress remoteAddress)
	{
		return this.sendRequest(httpRequest, remoteAddress, this.configuration.socketConnectionTimeoutInMillis());
	}
	
	@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:FinalParameters", "PMD.CognitiveComplexity"})
	public CompletableFuture<HttpResponse> sendRequest(
		final HttpRequest httpRequest,
		InetSocketAddress remoteAddress,
		final Long connectionTimeoutMillis)
	{
		if(!this.eventLoopGroup.isShuttingDown())
		{
			if(remoteAddress == null)
			{
				remoteAddress = httpRequest.socketAddressFromHostHeader();
			}
			if(Protocol.HTTP_2.equals(httpRequest.getProtocol()))
			{
				LOG.warn("HTTP2 requires ALPN but request is not secure (i.e. TLS) so protocol changed to HTTP1");
				httpRequest.withProtocol(Protocol.HTTP_1_1);
			}
			
			final CompletableFuture<HttpResponse> httpResponseFuture = new CompletableFuture<>();
			final CompletableFuture<Message> responseFuture = new CompletableFuture<>();
			final Protocol httpProtocol =
				httpRequest.getProtocol() != null ? httpRequest.getProtocol() : Protocol.HTTP_1_1;
			
			final HttpClientInitializer clientInitializer =
				new HttpClientInitializer(this.proxyConfigurations, httpProtocol);
			
			new Bootstrap()
				.group(this.eventLoopGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.AUTO_READ, true)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
				.option(
					ChannelOption.CONNECT_TIMEOUT_MILLIS,
					connectionTimeoutMillis != null ? connectionTimeoutMillis.intValue() : null)
				.attr(REMOTE_SOCKET, remoteAddress)
				.attr(RESPONSE_FUTURE, responseFuture)
				.attr(ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE, true)
				.handler(clientInitializer)
				.connect(remoteAddress)
				.addListener((ChannelFutureListener)future -> {
					if(future.isSuccess())
					{
						// ensure if HTTP2 is used then settings have been received from server
						clientInitializer.whenComplete((protocol, throwable) -> {
							if(throwable != null)
							{
								httpResponseFuture.completeExceptionally(throwable);
							}
							else
							{
								// send the HTTP request
								future.channel().writeAndFlush(httpRequest);
							}
						});
					}
					else
					{
						httpResponseFuture.completeExceptionally(future.cause());
					}
				});
			
			responseFuture
				.whenComplete((message, throwable) -> {
					if(throwable == null)
					{
						if(message != null)
						{
							if(this.forwardProxyClient)
							{
								httpResponseFuture.complete(HOP_BY_HOP_HEADER_FILTER.onResponse((HttpResponse)message));
							}
							else
							{
								httpResponseFuture.complete((HttpResponse)message);
							}
						}
						else
						{
							httpResponseFuture.complete(response());
						}
					}
					else
					{
						httpResponseFuture.completeExceptionally(throwable);
					}
				});
			
			return httpResponseFuture;
		}
		else
		{
			throw new IllegalStateException(
				"Request sent after client has been stopped - the event loop has been shutdown so it is not possible "
					+ "to send a request");
		}
	}
	
	@SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:MagicNumber", "PMD.CognitiveComplexity"})
	public CompletableFuture<BinaryMessage> sendRequest(
		final BinaryMessage binaryRequest,
		final boolean isSecure,
		InetSocketAddress remoteAddress,
		final Long connectionTimeoutMillis)
	{
		if(!this.eventLoopGroup.isShuttingDown())
		{
			if(this.proxyConfigurations != null && !isSecure
				&& this.proxyConfigurations.containsKey(ProxyConfiguration.Type.HTTP))
			{
				remoteAddress = this.proxyConfigurations.get(ProxyConfiguration.Type.HTTP).getProxyAddress();
			}
			else if(remoteAddress == null)
			{
				throw new IllegalArgumentException("Remote address cannot be null");
			}
			
			final CompletableFuture<BinaryMessage> binaryResponseFuture = new CompletableFuture<>();
			final CompletableFuture<Message> responseFuture = new CompletableFuture<>();
			
			new Bootstrap()
				.group(this.eventLoopGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.AUTO_READ, true)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
				.option(
					ChannelOption.CONNECT_TIMEOUT_MILLIS,
					connectionTimeoutMillis != null ? connectionTimeoutMillis.intValue() : null)
				.attr(REMOTE_SOCKET, remoteAddress)
				.attr(RESPONSE_FUTURE, responseFuture)
				.attr(
					ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE,
					!this.configuration.forwardBinaryRequestsWithoutWaitingForResponse())
				.handler(new HttpClientInitializer(this.proxyConfigurations, null))
				.connect(remoteAddress)
				.addListener((ChannelFutureListener)future -> {
					if(future.isSuccess())
					{
						if(LOG.isDebugEnabled())
						{
							LOG.debug(
								"Sending bytes hex {} to {}",
								ByteBufUtil.hexDump(binaryRequest.getBytes()),
								future.channel().attr(REMOTE_SOCKET).get());
						}
						// send the binary request
						future.channel().writeAndFlush(Unpooled.copiedBuffer(binaryRequest.getBytes()));
					}
					else
					{
						binaryResponseFuture.completeExceptionally(future.cause());
					}
				});
			
			responseFuture
				.whenComplete((message, throwable) -> {
					if(throwable == null)
					{
						binaryResponseFuture.complete((BinaryMessage)message);
					}
					else
					{
						LOG.error("", throwable);
						binaryResponseFuture.completeExceptionally(throwable);
					}
				});
			
			return binaryResponseFuture;
		}
		else
		{
			throw new IllegalStateException(
				"Request sent after client has been stopped - the event loop has been shutdown so it is not possible "
					+ "to send a request");
		}
	}
	
	@SuppressWarnings("PMD.PreserveStackTrace")
	public HttpResponse sendRequest(
		final HttpRequest httpRequest,
		final long timeout,
		final TimeUnit unit,
		final boolean ignoreErrors)
	{
		HttpResponse httpResponse = null;
		try
		{
			httpResponse = this.sendRequest(httpRequest).get(timeout, unit);
		}
		catch(final TimeoutException e)
		{
			if(!ignoreErrors)
			{
				throw new SocketCommunicationException(
					"Response was not received from MockServer after " + this.configuration.maxSocketTimeoutInMillis()
						+ " milliseconds, to wait longer please use \"mockserver.maxSocketTimeout\" system property or"
						+ " ConfigurationProperties.maxSocketTimeout(long milliseconds)",
					e.getCause());
			}
		}
		catch(final InterruptedException | ExecutionException ex)
		{
			if(!ignoreErrors)
			{
				final Throwable cause = ex.getCause();
				if(cause instanceof SocketConnectionException)
				{
					throw (SocketConnectionException)cause;
				}
				else if(cause instanceof ConnectException)
				{
					throw new SocketConnectionException(
						"Unable to connect to socket " + httpRequest.socketAddressFromHostHeader(),
						cause);
				}
				else if(cause instanceof UnknownHostException)
				{
					throw new SocketConnectionException(
						"Unable to resolve host " + httpRequest.socketAddressFromHostHeader(),
						cause);
				}
				else if(cause instanceof IOException)
				{
					throw new SocketConnectionException(cause.getMessage(), cause);
				}
				else
				{
					throw new RuntimeException("Exception while sending request - " + ex.getMessage(), ex);
				}
			}
		}
		return httpResponse;
	}
}
