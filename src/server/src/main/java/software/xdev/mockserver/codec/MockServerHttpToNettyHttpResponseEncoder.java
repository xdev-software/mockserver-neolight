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

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import software.xdev.mockserver.mappers.MockServerHttpResponseToFullHttpResponse;
import software.xdev.mockserver.model.HttpResponse;


public class MockServerHttpToNettyHttpResponseEncoder extends MessageToMessageEncoder<HttpResponse>
{
	private final MockServerHttpResponseToFullHttpResponse mockServerHttpResponseToFullHttpResponse;
	
	public MockServerHttpToNettyHttpResponseEncoder()
	{
		this.mockServerHttpResponseToFullHttpResponse = new MockServerHttpResponseToFullHttpResponse();
	}
	
	@Override
	protected void encode(final ChannelHandlerContext ctx, final HttpResponse response, final List<Object> out)
	{
		out.addAll(this.mockServerHttpResponseToFullHttpResponse.mapMockServerResponseToNettyResponse(response));
	}
}
