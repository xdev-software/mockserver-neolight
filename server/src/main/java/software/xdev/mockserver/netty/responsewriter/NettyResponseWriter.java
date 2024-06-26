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
package software.xdev.mockserver.netty.responsewriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.model.ConnectionOptions;
import software.xdev.mockserver.model.Delay;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.scheduler.Scheduler;


public class NettyResponseWriter extends ResponseWriter
{
	private static final Logger LOG = LoggerFactory.getLogger(NettyResponseWriter.class);
	
	private final ChannelHandlerContext ctx;
	private final Scheduler scheduler;
	
	public NettyResponseWriter(
		final ServerConfiguration configuration,
		final ChannelHandlerContext ctx,
		final Scheduler scheduler)
	{
		super(configuration);
		this.ctx = ctx;
		this.scheduler = scheduler;
	}
	
	@Override
	public void sendResponse(final HttpRequest request, final HttpResponse response)
	{
		this.writeAndCloseSocket(this.ctx, request, response);
	}
	
	private void writeAndCloseSocket(
		final ChannelHandlerContext ctx,
		final HttpRequest request,
		final HttpResponse response)
	{
		final boolean closeChannel;
		
		final ConnectionOptions connectionOptions = response.getConnectionOptions();
		if(connectionOptions != null && connectionOptions.getCloseSocket() != null)
		{
			closeChannel = connectionOptions.getCloseSocket();
		}
		else
		{
			closeChannel = !(request.isKeepAlive() != null && request.isKeepAlive());
		}
		
		final ChannelFuture channelFuture = ctx.writeAndFlush(response);
		if(closeChannel || this.configuration.alwaysCloseSocketConnections())
		{
			channelFuture.addListener((ChannelFutureListener)future -> {
				final Delay closeSocketDelay =
					connectionOptions != null ? connectionOptions.getCloseSocketDelay() : null;
				if(closeSocketDelay == null)
				{
					this.disconnectAndCloseChannel(future);
				}
				else
				{
					this.scheduler.schedule(() -> this.disconnectAndCloseChannel(future), false, closeSocketDelay);
				}
			});
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	private void disconnectAndCloseChannel(final ChannelFuture future)
	{
		future
			.channel()
			.disconnect()
			.addListener(disconnectFuture -> {
					if(disconnectFuture.isSuccess())
					{
						future
							.channel()
							.close()
							.addListener(closeFuture -> {
								if(disconnectFuture.isSuccess())
								{
									if(LOG.isTraceEnabled())
									{
										LOG.trace(
											"Disconnected and closed socket {}",
											future.channel().localAddress());
									}
								}
								else
								{
									if(LOG.isWarnEnabled())
									{
										LOG.warn("Exception closing socket {}", future.channel().localAddress());
									}
								}
							});
					}
					else if(LOG.isWarnEnabled())
					{
						LOG.warn(
							"Exception disconnecting socket {}",
							future.channel().localAddress(),
							disconnectFuture.cause());
					}
				}
			);
	}
}
