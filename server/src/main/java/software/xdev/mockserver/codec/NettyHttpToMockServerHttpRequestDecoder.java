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
package software.xdev.mockserver.codec;

import java.net.SocketAddress;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.mappers.FullHttpRequestToMockServerHttpRequest;
import software.xdev.mockserver.model.Header;


public class NettyHttpToMockServerHttpRequestDecoder extends MessageToMessageDecoder<FullHttpRequest>
{
	private final FullHttpRequestToMockServerHttpRequest fullHttpRequestToMockServerRequest;
	
	public NettyHttpToMockServerHttpRequestDecoder(final ServerConfiguration configuration, final Integer port)
	{
		this.fullHttpRequestToMockServerRequest = new FullHttpRequestToMockServerHttpRequest(configuration, port);
	}
	
	@Override
	protected void decode(
		final ChannelHandlerContext ctx,
		final FullHttpRequest fullHttpRequest,
		final List<Object> out)
	{
		List<Header> preservedHeaders = null;
		SocketAddress localAddress = null;
		SocketAddress remoteAddress = null;
		if(ctx != null && ctx.channel() != null)
		{
			preservedHeaders = PreserveHeadersNettyRemoves.preservedHeaders(ctx.channel());
			localAddress = ctx.channel().localAddress();
			remoteAddress = ctx.channel().remoteAddress();
		}
		out.add(this.fullHttpRequestToMockServerRequest.mapFullHttpRequestToMockServerRequest(
			fullHttpRequest, preservedHeaders, localAddress, remoteAddress));
	}
}
