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
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import software.xdev.mockserver.codec.MockServerHttpServerCodec;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.netty.proxy.relay.RelayConnectHandler;


@ChannelHandler.Sharable
public abstract class SocksConnectHandler<T> extends RelayConnectHandler<T>
{
	protected SocksConnectHandler(
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
		final ChannelPipeline pipeline = ctx.pipeline();
		this.removeHandler(pipeline, HttpServerCodec.class);
		this.removeHandler(pipeline, HttpContentDecompressor.class);
		this.removeHandler(pipeline, HttpObjectAggregator.class);
		this.removeHandler(pipeline, MockServerHttpServerCodec.class);
		if(pipeline.get(this.getClass()) != null)
		{
			pipeline.remove(this);
		}
	}
}
