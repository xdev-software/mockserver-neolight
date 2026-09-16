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
package software.xdev.mockserver.logging;

import static software.xdev.mockserver.character.Character.NEW_LINE;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;


@Sharable
@SuppressWarnings("checkstyle:MagicNumber")
public class LoggingHandler extends ChannelDuplexHandler
{
	private static final String[] BYTE2HEX = new String[256];
	private static final String[] HEXPADDING = new String[16];
	private static final String[] BYTEPADDING = new String[16];
	private static final char[] BYTE2CHAR = new char[256];
	
	static
	{
		int i;
		
		// Generate the lookup table for byte-to-hex-dump conversion
		for(i = 0; i < 10; i++)
		{
			BYTE2HEX[i] = " 0" + i;
		}
		for(; i < 16; i++)
		{
			BYTE2HEX[i] = " 0" + (char)('a' + i - 10);
		}
		for(; i < BYTE2HEX.length; i++)
		{
			BYTE2HEX[i] = " " + Integer.toHexString(i);
		}
		
		// Generate the lookup table for hex dump paddings
		for(i = 0; i < HEXPADDING.length; i++)
		{
			final int padding = HEXPADDING.length - i;
			HEXPADDING[i] = "   ".repeat(Math.max(0, padding));
		}
		
		// Generate the lookup table for byte dump paddings
		for(i = 0; i < BYTEPADDING.length; i++)
		{
			final int padding = BYTEPADDING.length - i;
			BYTEPADDING[i] = " ".repeat(Math.max(0, padding));
		}
		
		// Generate the lookup table for byte-to-char conversion
		for(i = 0; i < BYTE2CHAR.length; i++)
		{
			if(i <= 0x1f || i >= 0x7f)
			{
				BYTE2CHAR[i] = '.';
			}
			else
			{
				BYTE2CHAR[i] = (char)i;
			}
		}
	}
	
	protected final Logger logger;
	
	public LoggingHandler(final String loggerName)
	{
		this.logger = LoggerFactory.getLogger(loggerName);
	}
	
	public void addLoggingHandler(final ChannelHandlerContext ctx)
	{
		final ChannelPipeline pipeline = ctx.pipeline();
		if(pipeline.get(LoggingHandler.class) != null)
		{
			pipeline.remove(LoggingHandler.class);
		}
		final ChannelHandlerContext context = pipeline.context(SslHandler.class);
		if(context != null)
		{
			pipeline.addAfter(context.name(), "LoggingHandler#0", this);
		}
		else
		{
			pipeline.addFirst(this);
		}
	}
	
	@Override
	public void channelRegistered(final ChannelHandlerContext ctx) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "REGISTERED"));
		}
		super.channelRegistered(ctx);
	}
	
	@Override
	public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "UNREGISTERED"));
		}
		super.channelUnregistered(ctx);
	}
	
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "ACTIVE"));
		}
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(final ChannelHandlerContext ctx) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "INACTIVE"));
		}
		super.channelInactive(ctx);
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "EXCEPTION: " + cause), cause);
		}
		super.exceptionCaught(ctx, cause);
	}
	
	@Override
	public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			if(evt instanceof final SslHandshakeCompletionEvent sslHandshakeCompletionEvent)
			{
				this.logger.trace(
					this.format(ctx, "SslHandshakeCompletionEvent: "),
					sslHandshakeCompletionEvent.cause());
			}
			else if(evt instanceof final Exception ex)
			{
				this.logger.trace(this.format(ctx, "Exception: "), ex);
			}
			else
			{
				this.logger.trace(this.format(ctx, "USER_EVENT: " + evt));
			}
		}
		super.userEventTriggered(ctx, evt);
	}
	
	@Override
	public void bind(final ChannelHandlerContext ctx, final SocketAddress localAddress, final ChannelPromise promise)
		throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "BIND(" + localAddress + ')'));
		}
		super.bind(ctx, localAddress, promise);
	}
	
	@Override
	public void connect(
		final ChannelHandlerContext ctx,
		final SocketAddress remoteAddress,
		final SocketAddress localAddress,
		final ChannelPromise promise) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "CONNECT(" + remoteAddress + ", " + localAddress + ')'));
		}
		super.connect(ctx, remoteAddress, localAddress, promise);
	}
	
	@Override
	public void disconnect(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "DISCONNECT()"));
		}
		super.disconnect(ctx, promise);
	}
	
	@Override
	public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "CLOSE()"));
		}
		super.close(ctx, promise);
	}
	
	@Override
	public void deregister(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "DEREGISTER()"));
		}
		super.deregister(ctx, promise);
	}
	
	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg)
	{
		this.logMessage(ctx, "RECEIVED", msg);
		ctx.fireChannelRead(msg);
	}
	
	@Override
	public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
	{
		this.logMessage(ctx, "WRITE", msg);
		ctx.write(msg, promise);
	}
	
	@Override
	public void flush(final ChannelHandlerContext ctx)
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, "FLUSH"));
		}
		ctx.flush();
	}
	
	private void logMessage(final ChannelHandlerContext ctx, final String eventName, final Object msg)
	{
		if(this.logger.isTraceEnabled())
		{
			this.logger.trace(this.format(ctx, this.formatMessage(eventName, msg)));
		}
	}
	
	protected String format(final ChannelHandlerContext ctx, final String message)
	{
		String chStr = ctx.channel().toString() + ' ' + message;
		if(this.logger.isTraceEnabled())
		{
			chStr += NEW_LINE
				+ "channel: " + ctx.channel().id() + NEW_LINE
				+ "localAddress: " + ctx.channel().localAddress() + NEW_LINE
				+ "remoteAddress: " + ctx.channel().remoteAddress() + NEW_LINE
				+ "current: " + ctx.name() + NEW_LINE
				+ "pipeline: " + ctx.pipeline().names() + NEW_LINE;
		}
		return chStr;
	}
	
	private String formatMessage(final String eventName, final Object msg)
	{
		if(msg instanceof final ByteBuf byteBuf)
		{
			return this.formatByteBuf(eventName, byteBuf);
		}
		else if(msg instanceof final ByteBufHolder byteBufHolder)
		{
			return this.formatByteBufHolder(eventName, byteBufHolder);
		}
		return this.formatNonByteBuf(eventName, msg);
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.AvoidStringBuilderOrBuffer"})
	private String formatByteBuf(final String eventName, final ByteBuf buf)
	{
		final int length = buf.readableBytes();
		final int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
		final StringBuilder dump = new StringBuilder(rows * 80 + eventName.length() + 16);
		
		dump.append(eventName).append('(').append(length).append("B)")
			.append(NEW_LINE)
			.append("         +-------------------------------------------------+")
			.append(NEW_LINE)
			.append("         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |")
			.append(NEW_LINE)
			.append("+--------+-------------------------------------------------+----------------+");
		
		final int startIndex = buf.readerIndex();
		final int endIndex = buf.writerIndex();
		
		int i;
		for(i = startIndex; i < endIndex; i++)
		{
			final int relIdx = i - startIndex;
			final int relIdxMod16 = relIdx & 15;
			if(relIdxMod16 == 0)
			{
				dump.append(NEW_LINE)
					.append(Long.toHexString(relIdx & 0xFFFFFFFFL | 0x100000000L));
				dump.setCharAt(dump.length() - 9, '|');
				dump.append('|');
			}
			dump.append(BYTE2HEX[buf.getUnsignedByte(i)]);
			if(relIdxMod16 == 15)
			{
				dump.append(" |");
				if(i > 15 && buf.readableBytes() > i)
				{
					dump.append(buf.toString(i - 15, 16, StandardCharsets.UTF_8)
						.replaceAll(NEW_LINE, "/")
						.replace("\r", "/"));
				}
				else
				{
					for(int j = i - 15; j <= i; j++)
					{
						dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
					}
				}
				dump.append('|');
			}
		}
		
		if((i - startIndex & 15) != 0)
		{
			final int remainder = length & 15;
			dump.append(HEXPADDING[remainder])
				.append(" |");
			for(int j = i - remainder; j < i; j++)
			{
				dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
			}
			dump.append(BYTEPADDING[remainder])
				.append('|');
		}
		
		dump.append(NEW_LINE).append("+--------+-------------------------------------------------+----------------+");
		
		return dump.toString();
	}
	
	private String formatByteBufHolder(final String eventName, final ByteBufHolder msg)
	{
		return this.formatByteBuf(eventName, msg.content());
	}
	
	private String formatNonByteBuf(final String eventName, final Object msg)
	{
		final String msgAsString = msg.toString();
		return eventName + "(rel:" + msgAsString.length() + ")" + ": " + msgAsString;
	}
}
