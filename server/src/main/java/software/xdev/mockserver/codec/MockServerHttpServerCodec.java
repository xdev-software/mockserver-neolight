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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.channel.CombinedChannelDuplexHandler;
import software.xdev.mockserver.configuration.ServerConfiguration;


public class MockServerHttpServerCodec
	extends CombinedChannelDuplexHandler<NettyHttpToMockServerHttpRequestDecoder,
	MockServerHttpToNettyHttpResponseEncoder>
{
	public MockServerHttpServerCodec(final ServerConfiguration configuration, final SocketAddress socketAddress)
	{
		this(configuration, socketAddress instanceof final InetSocketAddress isa ? isa.getPort() : null);
	}
	
	public MockServerHttpServerCodec(final ServerConfiguration configuration, final Integer port)
	{
		this.init(
			new NettyHttpToMockServerHttpRequestDecoder(configuration, port),
			new MockServerHttpToNettyHttpResponseEncoder());
	}
}
