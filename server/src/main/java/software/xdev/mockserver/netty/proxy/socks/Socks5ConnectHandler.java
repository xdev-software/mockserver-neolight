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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.LifeCycle;


@ChannelHandler.Sharable
public final class Socks5ConnectHandler extends SocksConnectHandler<Socks5CommandRequest>
{
	public Socks5ConnectHandler(
		final ServerConfiguration configuration,
		final LifeCycle server,
		final String host,
		final int port)
	{
		super(configuration, server, host, port);
	}
	
	@Override
	protected void removeCodecSupport(final ChannelHandlerContext ctx)
	{
		super.removeCodecSupport(ctx);
		this.removeHandler(ctx.pipeline(), Socks5ServerEncoder.class);
	}
	
	@Override
	protected Object successResponse(final Object request)
	{
		return new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.DOMAIN, this.host,
			this.port);
	}
	
	@Override
	protected Object failureResponse(final Object request)
	{
		return new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.DOMAIN, this.host,
			this.port);
	}
}
