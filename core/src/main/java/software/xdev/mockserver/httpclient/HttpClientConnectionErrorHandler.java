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
package software.xdev.mockserver.httpclient;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import software.xdev.mockserver.model.Message;

import java.util.concurrent.CompletableFuture;

import static software.xdev.mockserver.httpclient.NettyHttpClient.ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE;
import static software.xdev.mockserver.httpclient.NettyHttpClient.RESPONSE_FUTURE;

@ChannelHandler.Sharable
public class HttpClientConnectionErrorHandler extends ChannelDuplexHandler {

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        CompletableFuture<? extends Message> responseFuture = ctx.channel().attr(RESPONSE_FUTURE).get();
        if (responseFuture != null && !responseFuture.isDone()) {
            if (ctx.channel().attr(ERROR_IF_CHANNEL_CLOSED_WITHOUT_RESPONSE).get()) {
                responseFuture.completeExceptionally(new SocketConnectionException("Channel handler removed before valid response has been received"));
            } else {
                responseFuture.complete(null);
            }
        }
        super.handlerRemoved(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        CompletableFuture<? extends Message> responseFuture = ctx.channel().attr(RESPONSE_FUTURE).get();
        if (!responseFuture.isDone()) {
            responseFuture.completeExceptionally(cause);
        }
        super.exceptionCaught(ctx, cause);
    }
}
