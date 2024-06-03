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
package software.xdev.mockserver.netty.proxy.socks;

import static software.xdev.mockserver.netty.unification.PortUnificationHandler.isSslEnabledUpstream;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5Message;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.LifeCycle;


@ChannelHandler.Sharable
public class Socks5ProxyHandler extends SocksProxyHandler<Socks5Message>
{
	private static final Logger LOG = LoggerFactory.getLogger(Socks5ProxyHandler.class);
	
	public Socks5ProxyHandler(final ServerConfiguration configuration, final LifeCycle server)
	{
		super(configuration, server);
	}
	
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final Socks5Message socksRequest)
	{
		if(socksRequest instanceof Socks5InitialRequest)
		{
			this.handleInitialRequest(ctx, (Socks5InitialRequest)socksRequest);
		}
		else if(socksRequest instanceof Socks5PasswordAuthRequest)
		{
			this.handlePasswordAuthRequest(ctx, (Socks5PasswordAuthRequest)socksRequest);
		}
		else if(socksRequest instanceof Socks5CommandRequest)
		{
			this.handleCommandRequest(ctx, (Socks5CommandRequest)socksRequest);
		}
		else
		{
			ctx.close();
		}
	}
	
	private void handleInitialRequest(final ChannelHandlerContext ctx, final Socks5InitialRequest initialRequest)
	{
		final String username = this.configuration.proxyAuthenticationUsername();
		final String password = this.configuration.proxyAuthenticationPassword();
		final Socks5AuthMethod requiredAuthMethod;
		final ChannelHandler nextRequestDecoder;
		if(initialRequest.authMethods().contains(Socks5AuthMethod.NO_AUTH))
		{
			requiredAuthMethod = Socks5AuthMethod.NO_AUTH;
			nextRequestDecoder = new Socks5CommandRequestDecoder();
		}
		else if(initialRequest.authMethods().contains(Socks5AuthMethod.PASSWORD))
		{
			if(isNotBlank(username) && isNotBlank(password))
			{
				requiredAuthMethod = Socks5AuthMethod.PASSWORD;
				nextRequestDecoder = new Socks5PasswordAuthRequestDecoder();
			}
			else
			{
				requiredAuthMethod = Socks5AuthMethod.NO_AUTH;
				nextRequestDecoder = new Socks5CommandRequestDecoder();
			}
		}
		else
		{
			requiredAuthMethod = Socks5AuthMethod.NO_AUTH;
			nextRequestDecoder = new Socks5CommandRequestDecoder();
		}
		this.answerInitialRequest(ctx, initialRequest, requiredAuthMethod, nextRequestDecoder);
	}
	
	private void answerInitialRequest(
		final ChannelHandlerContext ctx,
		final Socks5InitialRequest initialRequest,
		final Socks5AuthMethod requiredAuthMethod,
		final ChannelHandler nextRequestDecoder)
	{
		ctx.writeAndFlush(initialRequest
			.authMethods()
			.stream()
			.filter(authMethod -> authMethod.equals(requiredAuthMethod))
			.findFirst()
			.map(authMethod -> {
				if(isSslEnabledUpstream(ctx.channel()))
				{
					ctx.pipeline().addAfter("SslHandler#0", null, nextRequestDecoder);
				}
				else
				{
					ctx.pipeline().addFirst(nextRequestDecoder);
				}
				return new DefaultSocks5InitialResponse(requiredAuthMethod);
			})
			.orElse(new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED))
		);
		ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
	}
	
	private void handlePasswordAuthRequest(
		final ChannelHandlerContext ctx,
		final Socks5PasswordAuthRequest passwordAuthRequest)
	{
		final String username = this.configuration.proxyAuthenticationUsername();
		final String password = this.configuration.proxyAuthenticationPassword();
		// we need the null-check again here, in case the properties got unset between init and auth request
		if(isNotBlank(username) && isNotBlank(password)
			&& username.equals(passwordAuthRequest.username()) && password.equals(passwordAuthRequest.password()))
		{
			ctx.pipeline().replace(Socks5PasswordAuthRequestDecoder.class, null, new Socks5CommandRequestDecoder());
			ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS))
				.awaitUninterruptibly();
		}
		else
		{
			ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE))
				.addListener(ChannelFutureListener.CLOSE);
			LOG.info("Proxy authentication failed so returning SOCKS FAILURE response");
		}
	}
	
	private void handleCommandRequest(final ChannelHandlerContext ctx, final Socks5CommandRequest commandRequest)
	{
		if(commandRequest.type().equals(Socks5CommandType.CONNECT))
		{ // IN HERE
			this.forwardConnection(
				ctx,
				new Socks5ConnectHandler(this.configuration,
					this.server, commandRequest.dstAddr(), commandRequest.dstPort()));
			ctx.fireChannelRead(commandRequest);
		}
		else
		{
			ctx.writeAndFlush(new DefaultSocks5CommandResponse(
				Socks5CommandStatus.COMMAND_UNSUPPORTED,
				Socks5AddressType.DOMAIN,
				"",
				0)).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
