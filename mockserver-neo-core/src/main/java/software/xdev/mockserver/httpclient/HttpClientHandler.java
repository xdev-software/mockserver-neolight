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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.NotSslRecordException;
import software.xdev.mockserver.model.Message;

import javax.net.ssl.SSLException;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.httpclient.NettyHttpClient.RESPONSE_FUTURE;

@ChannelHandler.Sharable
public class HttpClientHandler extends SimpleChannelInboundHandler<Message> {

    private final List<String> connectionClosedStrings = Arrays.asList(
        "Broken pipe",
        "(broken pipe)",
        "Connection reset"
    );

    HttpClientHandler() {
        super(false);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message response) {
        ctx.channel().attr(RESPONSE_FUTURE).get().complete(response);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (isNotSslException(cause) && isNotConnectionReset(cause)) {
            cause.printStackTrace();
        }
        ctx.channel().attr(RESPONSE_FUTURE).get().completeExceptionally(cause);
        ctx.close();
    }

    private boolean isNotSslException(Throwable cause) {
        return !(cause.getCause() instanceof SSLException || cause instanceof DecoderException | cause instanceof NotSslRecordException);
    }

    private boolean isNotConnectionReset(Throwable cause) {
        return connectionClosedStrings.stream().noneMatch(connectionClosedString ->
            (isNotBlank(cause.getMessage()) && cause.getMessage().contains(connectionClosedString))
                || (cause.getCause() != null && isNotBlank(cause.getCause().getMessage()) && cause.getCause().getMessage().contains(connectionClosedString)));
    }
}
