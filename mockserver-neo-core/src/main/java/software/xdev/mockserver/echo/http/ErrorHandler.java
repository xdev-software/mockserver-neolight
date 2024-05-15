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
package software.xdev.mockserver.echo.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

@ChannelHandler.Sharable
public class ErrorHandler extends ChannelDuplexHandler {

    private final EchoServer.Error error;

    ErrorHandler(EchoServer.Error error) {
        this.error = error;
    }

    @Override
    public void read(ChannelHandlerContext ctx) {
        if (error == EchoServer.Error.CLOSE_CONNECTION) {
            ctx.disconnect();
            ctx.close();
        } else {
            ctx.read();
        }
    }

}
