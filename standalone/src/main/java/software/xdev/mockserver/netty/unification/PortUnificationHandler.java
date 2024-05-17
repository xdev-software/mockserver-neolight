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
package software.xdev.mockserver.netty.unification;

import static java.util.Collections.unmodifiableSet;
import static software.xdev.mockserver.exception.ExceptionHandling.closeOnFlush;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.exception.ExceptionHandling.sslHandshakeException;
import static software.xdev.mockserver.mock.action.http.HttpActionHandler.setRemoteAddress;
import static software.xdev.mockserver.netty.HttpRequestHandler.LOCAL_HOST_HEADERS;
import static software.xdev.mockserver.netty.HttpRequestHandler.setProxyingRequest;
import static software.xdev.mockserver.netty.proxy.relay.RelayConnectHandler.PROXIED;
import static software.xdev.mockserver.netty.proxy.relay.RelayConnectHandler.PROXIED_RESPONSE;
import static software.xdev.mockserver.netty.proxy.relay.RelayConnectHandler.PROXIED_SECURE;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import software.xdev.mockserver.codec.MockServerHttpServerCodec;
import software.xdev.mockserver.codec.PreserveHeadersNettyRemoves;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.logging.LoggingHandler;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.mock.action.http.HttpActionHandler;
import software.xdev.mockserver.netty.HttpRequestHandler;
import software.xdev.mockserver.netty.proxy.BinaryRequestProxyingHandler;
import software.xdev.mockserver.netty.proxy.socks.Socks4ProxyHandler;
import software.xdev.mockserver.netty.proxy.socks.Socks5ProxyHandler;
import software.xdev.mockserver.netty.proxy.socks.SocksDetector;
import software.xdev.mockserver.netty.websocketregistry.CallbackWebSocketServerHandler;
import software.xdev.mockserver.util.StringUtils;


public class PortUnificationHandler extends ReplayingDecoder<Void>
{
	private static final Logger LOG = LoggerFactory.getLogger(PortUnificationHandler.class);
	
	private static final AttributeKey<Boolean> TLS_ENABLED_UPSTREAM = AttributeKey.valueOf("TLS_ENABLED_UPSTREAM");
	private static final AttributeKey<Boolean> HTTP_ENABLED = AttributeKey.valueOf("HTTP_ENABLED");
	private static final Map<PortBinding, Set<String>> localAddressesCache = new ConcurrentHashMap<>();
	
	private final LoggingHandler loggingHandler = new LoggingHandler(PortUnificationHandler.class.getName() +
		"-first");
	private final HttpContentLengthRemover httpContentLengthRemover = new HttpContentLengthRemover();
	private final PreserveHeadersNettyRemoves preserveHeadersNettyRemoves = new PreserveHeadersNettyRemoves();
	private final ServerConfiguration configuration;
	private final LifeCycle server;
	private final HttpState httpState;
	private final HttpActionHandler actionHandler;
	
	public PortUnificationHandler(
		final ServerConfiguration configuration,
		final LifeCycle server,
		final HttpState httpState,
		final HttpActionHandler actionHandler)
	{
		this.configuration = configuration;
		this.server = server;
		this.httpState = httpState;
		this.actionHandler = actionHandler;
	}
	
	public static void enableSslUpstreamAndDownstream(final Channel channel)
	{
		channel.attr(TLS_ENABLED_UPSTREAM).set(Boolean.TRUE);
	}
	
	public static boolean isSslEnabledUpstream(final Channel channel)
	{
		if(channel.attr(TLS_ENABLED_UPSTREAM).get() != null)
		{
			return channel.attr(TLS_ENABLED_UPSTREAM).get();
		}
		else
		{
			return false;
		}
	}
	
	public static void httpEnabled(final Channel channel)
	{
		channel.attr(HTTP_ENABLED).set(Boolean.TRUE);
	}
	
	public static boolean isHttpEnabled(final Channel channel)
	{
		if(channel.attr(HTTP_ENABLED).get() != null)
		{
			return channel.attr(HTTP_ENABLED).get();
		}
		else
		{
			return false;
		}
	}
	
	@Override
	protected void decode(final ChannelHandlerContext ctx, final ByteBuf msg, final List<Object> out)
	{
		if(SocksDetector.isSocks4(msg, this.actualReadableBytes()))
		{
			this.logStage(ctx, "adding SOCKS4 decoders");
			this.enableSocks4(ctx, msg);
		}
		else if(SocksDetector.isSocks5(msg, this.actualReadableBytes()))
		{
			this.logStage(ctx, "adding SOCKS5 decoders");
			this.enableSocks5(ctx, msg);
		}
		else if(this.isTls(msg))
		{
			this.logStage(ctx, "adding TLS decoders");
			this.enableTls(ctx, msg);
		}
		else if(this.isHttp(msg))
		{
			this.logStage(ctx, "adding HTTP decoders");
			this.switchToHttp(ctx, msg);
		}
		else if(this.isProxyConnected(msg))
		{
			this.logStage(ctx, "setting proxy connected");
			this.switchToProxyConnected(ctx, msg);
		}
		else if(this.configuration.assumeAllRequestsAreHttp())
		{
			this.logStage(ctx, "adding HTTP decoders");
			this.switchToHttp(ctx, msg);
		}
		else
		{
			this.logStage(ctx, "adding binary decoder");
			this.switchToBinaryRequestProxying(ctx, msg);
		}
		
		if(LOG.isTraceEnabled())
		{
			this.loggingHandler.addLoggingHandler(ctx);
		}
	}
	
	private void logStage(final ChannelHandlerContext ctx, final String message)
	{
		if(LOG.isTraceEnabled())
		{
			LOG.trace("{} for channel: {} pipeline: {}", message, ctx.channel().toString(), ctx.pipeline().names());
		}
	}
	
	private void enableSocks4(final ChannelHandlerContext ctx, final ByteBuf msg)
	{
		this.enableSocks(
			ctx,
			msg,
			new Socks4ServerDecoder(),
			new Socks4ProxyHandler(this.configuration, this.server),
			Socks4ServerEncoder.INSTANCE);
	}
	
	private void enableSocks5(final ChannelHandlerContext ctx, final ByteBuf msg)
	{
		this.enableSocks(
			ctx,
			msg,
			new Socks5InitialRequestDecoder(),
			new Socks5ProxyHandler(this.configuration, this.server),
			Socks5ServerEncoder.DEFAULT);
	}
	
	private void enableSocks(
		final ChannelHandlerContext ctx,
		final ByteBuf msg,
		final ReplayingDecoder<?> socksInitialRequestDecoder,
		final ChannelHandler... channelHandlers)
	{
		final ChannelPipeline pipeline = ctx.pipeline();
		for(final ChannelHandler channelHandler : channelHandlers)
		{
			if(isSslEnabledUpstream(ctx.channel()))
			{
				pipeline.addAfter("SslHandler#0", null, channelHandler);
			}
			else
			{
				pipeline.addFirst(channelHandler);
			}
		}
		pipeline.addFirst(socksInitialRequestDecoder);
		
		setProxyingRequest(ctx, Boolean.TRUE);
		
		// re-unify (with SOCKS5 enabled)
		ctx.pipeline().fireChannelRead(msg.readBytes(this.actualReadableBytes()));
	}
	
	private boolean isTls(final ByteBuf buf)
	{
		return SslHandler.isEncrypted(buf);
	}
	
	private void enableTls(final ChannelHandlerContext ctx, final ByteBuf msg)
	{
		enableSslUpstreamAndDownstream(ctx.channel());
		
		// re-unify (with SSL enabled)
		ctx.pipeline().fireChannelRead(msg.readBytes(this.actualReadableBytes()));
	}
	
	private boolean isHttp(final ByteBuf msg)
	{
		final String method = msg.toString(msg.readerIndex(), 8, StandardCharsets.US_ASCII);
		return method.startsWith("GET ") ||
			method.startsWith("POST ") ||
			method.startsWith("PUT ") ||
			method.startsWith("HEAD ") ||
			method.startsWith("OPTIONS ") ||
			method.startsWith("PATCH ") ||
			method.startsWith("DELETE ") ||
			method.startsWith("TRACE ") ||
			method.startsWith("CONNECT ");
	}
	
	private void switchToHttp(final ChannelHandlerContext ctx, final ByteBuf msg)
	{
		if(!isHttpEnabled(ctx.channel()))
		{
			httpEnabled(ctx.channel());
			
			final ChannelPipeline pipeline = ctx.pipeline();
			
			this.addLastIfNotPresent(pipeline, new HttpServerCodec(
				this.configuration.maxInitialLineLength(),
				this.configuration.maxHeaderSize(),
				this.configuration.maxChunkSize()
			));
			this.addLastIfNotPresent(pipeline, this.preserveHeadersNettyRemoves);
			this.addLastIfNotPresent(pipeline, new HttpContentDecompressor());
			this.addLastIfNotPresent(pipeline, this.httpContentLengthRemover);
			this.addLastIfNotPresent(pipeline, new HttpObjectAggregator(Integer.MAX_VALUE));
			this.addLastIfNotPresent(pipeline, new CallbackWebSocketServerHandler(this.httpState));
			this.addLastIfNotPresent(
				pipeline,
				new MockServerHttpServerCodec(this.configuration, ctx.channel().localAddress()));
			this.addLastIfNotPresent(pipeline, new HttpRequestHandler(this.configuration,
				this.server, this.httpState, this.actionHandler));
			pipeline.remove(this);
			
			ctx.channel().attr(LOCAL_HOST_HEADERS).set(this.getLocalAddresses(ctx));
			
			// fire message back through pipeline
			ctx.fireChannelRead(msg.readBytes(this.actualReadableBytes()));
		}
	}
	
	private boolean isProxyConnected(final ByteBuf msg)
	{
		return msg.toString(msg.readerIndex(), 8, StandardCharsets.US_ASCII).startsWith(PROXIED);
	}
	
	private void switchToProxyConnected(final ChannelHandlerContext ctx, final ByteBuf msg)
	{
		final String message = this.readMessage(msg);
		if(message.startsWith(PROXIED_SECURE))
		{
			final String[] hostParts = StringUtils.substringAfter(message, PROXIED_SECURE).split(":");
			final int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 443;
			enableSslUpstreamAndDownstream(ctx.channel());
			setProxyingRequest(ctx, Boolean.TRUE);
			setRemoteAddress(ctx, new InetSocketAddress(hostParts[0], port));
		}
		else if(message.startsWith(PROXIED))
		{
			final String[] hostParts = StringUtils.substringAfter(message, PROXIED).split(":");
			final int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 80;
			setProxyingRequest(ctx, Boolean.TRUE);
			setRemoteAddress(ctx, new InetSocketAddress(hostParts[0], port));
		}
		ctx.writeAndFlush(Unpooled.copiedBuffer((PROXIED_RESPONSE + message).getBytes(StandardCharsets.UTF_8)))
			.awaitUninterruptibly();
	}
	
	private String readMessage(final ByteBuf msg)
	{
		final byte[] bytes = new byte[this.actualReadableBytes()];
		msg.readBytes(bytes);
		return new String(bytes, StandardCharsets.US_ASCII);
	}
	
	private void switchToBinaryRequestProxying(final ChannelHandlerContext ctx, final ByteBuf msg)
	{
		this.addLastIfNotPresent(ctx.pipeline(), new BinaryRequestProxyingHandler(
			this.configuration,
			this.httpState.getScheduler(),
			this.actionHandler.getHttpClient(),
			this.httpState.getEventBus())
		);
		
		// fire message back through pipeline
		ctx.fireChannelRead(msg.readBytes(this.actualReadableBytes()));
	}
	
	private Set<String> getLocalAddresses(final ChannelHandlerContext ctx)
	{
		final SocketAddress localAddress = ctx.channel().localAddress();
		Set<String> localAddresses = null;
		if(localAddress instanceof final InetSocketAddress inetSocketAddress)
		{
			final String portExtension =
				this.calculatePortExtension(inetSocketAddress, isSslEnabledUpstream(ctx.channel()));
			final PortBinding cacheKey = new PortBinding(inetSocketAddress, portExtension);
			localAddresses = localAddressesCache.get(cacheKey);
			if(localAddresses == null)
			{
				localAddresses = this.calculateLocalAddresses(inetSocketAddress, portExtension);
				localAddressesCache.put(cacheKey, localAddresses);
			}
		}
		return (localAddresses == null) ? Collections.emptySet() : localAddresses;
	}
	
	private String calculatePortExtension(final InetSocketAddress inetSocketAddress, final boolean sslEnabledUpstream)
	{
		final String portExtension;
		if(((inetSocketAddress.getPort() == 443) && sslEnabledUpstream)
			|| ((inetSocketAddress.getPort() == 80) && !sslEnabledUpstream))
		{
			portExtension = "";
		}
		else
		{
			portExtension = ":" + inetSocketAddress.getPort();
		}
		return portExtension;
	}
	
	private Set<String> calculateLocalAddresses(final InetSocketAddress localAddress, final String portExtension)
	{
		final InetAddress socketAddress = localAddress.getAddress();
		final Set<String> localAddresses = new HashSet<>();
		localAddresses.add(socketAddress.getHostAddress() + portExtension);
		localAddresses.add(socketAddress.getCanonicalHostName() + portExtension);
		localAddresses.add(socketAddress.getHostName() + portExtension);
		localAddresses.add("localhost" + portExtension);
		localAddresses.add("127.0.0.1" + portExtension);
		return unmodifiableSet(localAddresses);
	}
	
	private void addLastIfNotPresent(final ChannelPipeline pipeline, final ChannelHandler channelHandler)
	{
		if(pipeline.get(channelHandler.getClass()) == null)
		{
			pipeline.addLast(channelHandler);
		}
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable throwable)
	{
		if(connectionClosedException(throwable))
		{
			LOG.error("Exception caught by port unification handler -> closing pipeline {}",
				ctx.channel(), throwable);
		}
		else if(sslHandshakeException(throwable))
		{
			if(throwable.getMessage().contains("certificate_unknown")
				|| throwable.getMessage().toLowerCase().contains("unknown_ca"))
			{
				if(LOG.isWarnEnabled())
				{
					LOG.warn("TLS handshake failure: Client does not trust MockServer Certificate Authority for: {} "
							+ "See http://mock-server.com/mock_server/HTTPS_TLS.html to enable the client to trust "
							+ "MocksServer Certificate Authority.",
						ctx.channel(), throwable);
				}
			}
			else if(!throwable.getMessage().contains("close_notify during handshake"))
			{
				LOG.error("TLS handshake failure while a client attempted to connect to {}", ctx.channel(), throwable);
			}
		}
		closeOnFlush(ctx.channel());
	}
}
