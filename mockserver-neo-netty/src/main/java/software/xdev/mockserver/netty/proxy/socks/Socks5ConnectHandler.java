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
import io.netty.handler.codec.socksx.v5.*;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.logging.MockServerLogger;

@ChannelHandler.Sharable
public final class Socks5ConnectHandler extends SocksConnectHandler<Socks5CommandRequest> {

    public Socks5ConnectHandler(Configuration configuration, MockServerLogger mockServerLogger, LifeCycle server, String host, int port) {
        super(configuration, mockServerLogger, server, host, port);
    }

    protected void removeCodecSupport(ChannelHandlerContext ctx) {
        super.removeCodecSupport(ctx);
        removeHandler(ctx.pipeline(), Socks5ServerEncoder.class);
    }

    protected Object successResponse(Object request) {
        return new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.DOMAIN, host, port);
    }

    protected Object failureResponse(Object request) {
        return new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.DOMAIN, host, port);
    }
}
