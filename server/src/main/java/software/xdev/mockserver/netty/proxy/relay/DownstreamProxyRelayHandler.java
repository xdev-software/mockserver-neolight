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
package software.xdev.mockserver.netty.proxy.relay;

import static software.xdev.mockserver.exception.ExceptionHandling.closeOnFlush;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;


public class DownstreamProxyRelayHandler extends SimpleChannelInboundHandler<FullHttpResponse>
{
	private static final Logger LOG = LoggerFactory.getLogger(DownstreamProxyRelayHandler.class);
	private final Channel upstreamChannel;
	
	public DownstreamProxyRelayHandler(final Channel upstreamChannel)
	{
		super(false);
		this.upstreamChannel = upstreamChannel;
	}
	
	@Override
	public void channelActive(final ChannelHandlerContext ctx)
	{
		ctx.read();
		ctx.write(Unpooled.EMPTY_BUFFER);
	}
	
	@Override
	public void channelRead0(final ChannelHandlerContext ctx, final FullHttpResponse response)
	{
		this.upstreamChannel.writeAndFlush(response).addListener((ChannelFutureListener)future -> {
			if(future.isSuccess())
			{
				ctx.read();
			}
			else
			{
				if(this.isNotSocketClosedException(future.cause()))
				{
					LOG.error("Exception while returning writing {}", response, future.cause());
				}
				future.channel().close();
			}
		});
	}
	
	private boolean isNotSocketClosedException(final Throwable cause)
	{
		return !(cause instanceof ClosedChannelException || cause instanceof ClosedSelectorException);
	}
	
	@Override
	public void channelInactive(final ChannelHandlerContext ctx)
	{
		closeOnFlush(this.upstreamChannel);
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	{
		if(connectionClosedException(cause))
		{
			LOG.error("Exception caught by downstream relay handler -> closing pipeline {}", ctx.channel(), cause);
		}
		closeOnFlush(ctx.channel());
	}
}
