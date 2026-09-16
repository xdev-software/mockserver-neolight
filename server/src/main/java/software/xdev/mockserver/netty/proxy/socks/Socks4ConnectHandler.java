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
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.LifeCycle;


@ChannelHandler.Sharable
public final class Socks4ConnectHandler extends SocksConnectHandler<Socks4CommandRequest>
{
	public Socks4ConnectHandler(
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
		this.removeHandler(ctx.pipeline(), Socks4ServerEncoder.class);
	}
	
	@Override
	protected Object successResponse(final Object request)
	{
		return new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS, this.host, this.port);
	}
	
	@Override
	protected Object failureResponse(final Object request)
	{
		return new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED, this.host, this.port);
	}
}
