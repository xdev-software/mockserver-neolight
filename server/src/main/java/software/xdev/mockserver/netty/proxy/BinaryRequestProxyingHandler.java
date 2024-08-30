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
package software.xdev.mockserver.netty.proxy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.exception.ExceptionHandling.closeOnFlush;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.formatting.StringFormatter.formatBytes;
import static software.xdev.mockserver.mock.action.http.HttpActionHandler.getRemoteAddress;
import static software.xdev.mockserver.model.BinaryMessage.bytes;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.isSslEnabledUpstream;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.event.EventBus;
import software.xdev.mockserver.event.model.EventEntry;
import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.model.BinaryMessage;
import software.xdev.mockserver.model.BinaryProxyListener;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.uuid.UUIDService;


@ChannelHandler.Sharable
public class BinaryRequestProxyingHandler extends SimpleChannelInboundHandler<ByteBuf>
{
	private static final Logger LOG = LoggerFactory.getLogger(BinaryRequestProxyingHandler.class);
	private final ServerConfiguration configuration;
	private final Scheduler scheduler;
	private final NettyHttpClient httpClient;
	private final BinaryProxyListener binaryExchangeCallback;
	private final EventBus eventBus;
	
	public BinaryRequestProxyingHandler(
		final ServerConfiguration configuration,
		final Scheduler scheduler,
		final NettyHttpClient httpClient,
		final EventBus eventBus)
	{
		super(true);
		this.configuration = configuration;
		this.scheduler = scheduler;
		this.httpClient = httpClient;
		this.binaryExchangeCallback = configuration.binaryProxyListener();
		this.eventBus = eventBus;
	}
	
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf byteBuf)
	{
		this.eventBus.add(new EventEntry()
			.setType(EventEntry.EventType.RECEIVED_REQUEST)
			.setCorrelationId(UUIDService.getUUID()));
		
		final BinaryMessage binaryRequest = bytes(ByteBufUtil.getBytes(byteBuf));
		LOG.info("Received binary request: {}", ByteBufUtil.hexDump(binaryRequest.getBytes()));
		final InetSocketAddress remoteAddress = getRemoteAddress(ctx);
		if(remoteAddress != null)
		{ // binary protocol is only supported for proxies request and not mocking
			this.sendMessage(ctx, binaryRequest, remoteAddress);
		}
		else
		{
			if(LOG.isInfoEnabled())
			{
				LOG.info(
					"Unknown message format, only HTTP requests are supported for mocking or "
						+ "HTTP & binary requests for proxying, but request is not being proxied and request is not"
						+ " valid HTTP, found request in binary: {} in utf8 text: {}",
					ByteBufUtil.hexDump(binaryRequest.getBytes()),
					new String(binaryRequest.getBytes(), StandardCharsets.UTF_8));
			}
			ctx.writeAndFlush(Unpooled.copiedBuffer(
				("unknown message format, only HTTP requests are supported for mocking or HTTP & binary requests for "
					+ "proxying, but request is not being proxied and request is not valid HTTP").getBytes(
					StandardCharsets.UTF_8)
			));
			ctx.close();
		}
	}
	
	private void sendMessage(
		final ChannelHandlerContext ctx,
		final BinaryMessage binaryRequest,
		final InetSocketAddress remoteAddress)
	{
		final CompletableFuture<BinaryMessage> binaryResponseFuture = this.httpClient
			.sendRequest(
				binaryRequest,
				isSslEnabledUpstream(ctx.channel()),
				remoteAddress,
				this.configuration.socketConnectionTimeoutInMillis()
			);
		
		if(this.configuration.forwardBinaryRequestsWithoutWaitingForResponse())
		{
			this.processNotWaitingForResponse(ctx, binaryRequest, remoteAddress, binaryResponseFuture);
		}
		else
		{
			this.processWaitingForResponse(ctx, binaryRequest, remoteAddress, binaryResponseFuture);
		}
	}
	
	private void processNotWaitingForResponse(
		final ChannelHandlerContext ctx,
		final BinaryMessage binaryRequest,
		final InetSocketAddress remoteAddress,
		final CompletableFuture<BinaryMessage> binaryResponseFuture)
	{
		if(this.binaryExchangeCallback != null)
		{
			this.binaryExchangeCallback.onProxy(
				binaryRequest,
				binaryResponseFuture,
				remoteAddress,
				ctx.channel().remoteAddress());
		}
		this.scheduler.submit(binaryResponseFuture, () -> {
			try
			{
				final BinaryMessage binaryResponse =
					binaryResponseFuture.get(this.configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
				if(binaryResponse != null)
				{
					if(LOG.isInfoEnabled())
					{
						LOG.info(
							"Returning binary response: {} from: {} for forwarded binary request: {}",
							formatBytes(binaryResponse.getBytes()),
							remoteAddress,
							formatBytes(binaryRequest.getBytes()));
					}
					ctx.writeAndFlush(Unpooled.copiedBuffer(binaryResponse.getBytes()));
				}
			}
			catch(final Exception ex)
			{
				if(LOG.isWarnEnabled())
				{
					LOG.warn("Exception whilst sending hex {} to {} closing connection",
						ByteBufUtil.hexDump(binaryRequest.getBytes()), remoteAddress, ex);
				}
				ctx.close();
			}
		}, false);
	}
	
	private void processWaitingForResponse(
		final ChannelHandlerContext ctx,
		final BinaryMessage binaryRequest,
		final InetSocketAddress remoteAddress,
		final CompletableFuture<BinaryMessage> binaryResponseFuture)
	{
		this.scheduler.submit(binaryResponseFuture, () -> {
			try
			{
				final BinaryMessage binaryResponse =
					binaryResponseFuture.get(this.configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
				if(LOG.isInfoEnabled())
				{
					LOG.info(
						"Returning binary response: {} from: {} for forwarded binary request: {}",
						formatBytes(binaryResponse.getBytes()),
						remoteAddress,
						formatBytes(binaryRequest.getBytes()));
				}
				if(this.binaryExchangeCallback != null)
				{
					this.binaryExchangeCallback.onProxy(
						binaryRequest,
						binaryResponseFuture,
						remoteAddress,
						ctx.channel().remoteAddress());
				}
				ctx.writeAndFlush(Unpooled.copiedBuffer(binaryResponse.getBytes()));
			}
			catch(final Exception ex)
			{
				if(LOG.isWarnEnabled())
				{
					LOG.warn("Exception whilst sending hex {} to {} closing connection",
						ByteBufUtil.hexDump(binaryRequest.getBytes()), remoteAddress, ex);
				}
				ctx.close();
			}
		}, false);
	}
	
	@Override
	public void channelReadComplete(final ChannelHandlerContext ctx)
	{
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	{
		if(connectionClosedException(cause))
		{
			LOG.error("Exception caught by {} handler -> closing pipeline {}",
				this.getClass(), ctx.channel(), cause);
		}
		closeOnFlush(ctx.channel());
	}
}
