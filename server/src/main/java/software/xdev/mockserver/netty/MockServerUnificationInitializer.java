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
package software.xdev.mockserver.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.mock.action.http.HttpActionHandler;
import software.xdev.mockserver.netty.unification.PortUnificationHandler;


@ChannelHandler.Sharable
public class MockServerUnificationInitializer extends ChannelHandlerAdapter
{
	private final ServerConfiguration configuration;
	private final LifeCycle server;
	private final HttpState httpState;
	private final HttpActionHandler actionHandler;
	
	public MockServerUnificationInitializer(
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
	
	@Override
	public void handlerAdded(final ChannelHandlerContext ctx)
	{
		ctx.pipeline().replace(this, null, new PortUnificationHandler(
			this.configuration, this.server, this.httpState,
			this.actionHandler));
	}
}
