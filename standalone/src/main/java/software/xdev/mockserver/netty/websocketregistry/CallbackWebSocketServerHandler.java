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
package software.xdev.mockserver.netty.websocketregistry;

import static software.xdev.mockserver.closurecallback.websocketclient.WebSocketClient.CLIENT_REGISTRATION_ID_HEADER;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.isSslEnabledUpstream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.codec.MockServerHttpServerCodec;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.netty.HttpRequestHandler;
import software.xdev.mockserver.uuid.UUIDService;


@ChannelHandler.Sharable
public class CallbackWebSocketServerHandler extends ChannelInboundHandlerAdapter
{
	private static final Logger LOG = LoggerFactory.getLogger(CallbackWebSocketServerHandler.class);
	
	private static final AttributeKey<Boolean> CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET =
		AttributeKey.valueOf("CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET");
	private static final String UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI = "/_mockserver_callback_websocket";
	private WebSocketServerHandshaker handshaker;
	private final WebSocketClientRegistry webSocketClientRegistry;
	
	public CallbackWebSocketServerHandler(final HttpState httpStateHandler)
	{
		this.webSocketClientRegistry = httpStateHandler.getWebSocketClientRegistry();
	}
	
	@Override
	public void channelRead(final ChannelHandlerContext ctx, final Object msg)
	{
		boolean release = true;
		try
		{
			if(msg instanceof final FullHttpRequest fullHttpRequest && fullHttpRequest.uri()
				.equals(UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI))
			{
				this.upgradeChannel(ctx, fullHttpRequest);
				ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).set(true);
			}
			else if(ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).get() != null &&
				ctx.channel().attr(CHANNEL_UPGRADED_FOR_CALLBACK_WEB_SOCKET).get() &&
				msg instanceof final WebSocketFrame webSocketFrame)
			{
				this.handleWebSocketFrame(ctx, webSocketFrame);
			}
			else
			{
				release = false;
				ctx.fireChannelRead(msg);
			}
		}
		finally
		{
			if(release)
			{
				ReferenceCountUtil.release(msg);
			}
		}
	}
	
	@Override
	public void channelReadComplete(final ChannelHandlerContext ctx)
	{
		ctx.flush();
	}
	
	private void upgradeChannel(final ChannelHandlerContext ctx, final FullHttpRequest httpRequest)
	{
		this.handshaker = new WebSocketServerHandshakerFactory(
			(isSslEnabledUpstream(ctx.channel()) ? "wss" : "ws") + "://" + httpRequest.headers().get("Host")
				+ UPGRADE_CHANNEL_FOR_CALLBACK_WEB_SOCKET_URI,
			null,
			true,
			Integer.MAX_VALUE
		).newHandshaker(httpRequest);
		if(this.handshaker == null)
		{
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		}
		else
		{
			final String clientId = httpRequest.headers().contains(CLIENT_REGISTRATION_ID_HEADER) ?
				httpRequest.headers().get(CLIENT_REGISTRATION_ID_HEADER) :
				UUIDService.getUUID();
			if(LocalCallbackRegistry.responseClientExists(clientId)
				|| LocalCallbackRegistry.forwardClientExists(clientId))
			{
				// found locally to indicate to client
				final HttpResponse res = new DefaultFullHttpResponse(
					HttpVersion.HTTP_1_1,
					HttpResponseStatus.RESET_CONTENT,
					ctx.channel().alloc().buffer(0));
				HttpUtil.setContentLength(res, 0);
				ctx.channel().writeAndFlush(res, ctx.channel().newPromise());
			}
			else
			{
				this.handshaker
					.handshake(
						ctx.channel(),
						httpRequest,
						new DefaultHttpHeaders().add(CLIENT_REGISTRATION_ID_HEADER, clientId),
						ctx.channel().newPromise()
					)
					.addListener((ChannelFutureListener)future -> {
						ctx.pipeline().remove(MockServerHttpServerCodec.class);
						ctx.pipeline().remove(HttpRequestHandler.class);
						if(LOG.isTraceEnabled())
						{
							LOG.trace("Registering client {}", clientId);
						}
						this.webSocketClientRegistry.registerClient(clientId, ctx);
						future.channel().closeFuture().addListener((ChannelFutureListener)closeFuture -> {
							if(LOG.isTraceEnabled())
							{
								LOG.trace("Unregistering callback for client {}", clientId);
							}
							this.webSocketClientRegistry.unregisterClient(clientId);
						});
					});
			}
		}
	}
	
	private void handleWebSocketFrame(final ChannelHandlerContext ctx, final WebSocketFrame frame)
	{
		if(frame instanceof CloseWebSocketFrame)
		{
			this.handshaker.close(ctx.channel(), (CloseWebSocketFrame)frame.retain());
		}
		else if(frame instanceof final TextWebSocketFrame txtWebSocketFrame)
		{
			this.webSocketClientRegistry.receivedTextWebSocketFrame(txtWebSocketFrame);
		}
		else if(frame instanceof PingWebSocketFrame)
		{
			ctx.write(new PongWebSocketFrame(frame.content().retain()));
		}
		else
		{
			throw new UnsupportedOperationException(frame.getClass().getName() + " frame types not supported");
		}
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	{
		if(connectionClosedException(cause))
		{
			LOG.error("Web socket server caught exception", cause);
		}
		ctx.close();
	}
}
