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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpObject;
import software.xdev.mockserver.log.MockServerEventLog;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.mappers.MockServerHttpResponseToFullHttpResponse;
import software.xdev.mockserver.model.BodyWithContentType;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.RECEIVED_REQUEST;
import static software.xdev.mockserver.model.HttpResponse.response;
import static org.slf4j.event.Level.INFO;

@ChannelHandler.Sharable
public class EchoServerHandler extends SimpleChannelInboundHandler<HttpRequest> {
    
    private static final Logger LOG = LoggerFactory.getLogger(EchoServerHandler.class);
    
    private final EchoServer.Error error;
    private final EchoServer.NextResponse nextResponse;
    private final EchoServer.LastRequest lastRequest;

    EchoServerHandler(EchoServer.Error error, EchoServer.NextResponse nextResponse, EchoServer.LastRequest lastRequest) {
        this.error = error;
        this.nextResponse = nextResponse;
        this.lastRequest = lastRequest;
    }

    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) {
        
        LOG.info("EchoServer received request {}", request);

        if (!lastRequest.httpRequest.get().isDone()) {
            lastRequest.httpRequest.get().complete(request);
        }

        if (!nextResponse.httpResponse.isEmpty()) {
            // WARNING: this logic is only for unit tests that run in series and is NOT thread safe!!!
            DefaultHttpObject httpResponse = new MockServerHttpResponseToFullHttpResponse().mapMockServerResponseToNettyResponse(nextResponse.httpResponse.remove()).get(0);
            ctx.writeAndFlush(httpResponse);
        } else {
            HttpResponse httpResponse =
                response()
                    .withStatusCode(request.getPath().equalsIgnoreCase("/not_found") ? NOT_FOUND.code() : OK.code())
                    .withHeaders(request.getHeaderList());

            if (request.getBody() instanceof BodyWithContentType) {
                httpResponse.withBody((BodyWithContentType<?>) request.getBody());
            } else {
                httpResponse.withBody(request.getBodyAsString());
            }

            // set hop-by-hop headers
            final int length = httpResponse.getBody() != null ? httpResponse.getBody().getRawBytes().length : 0;
            if (error == EchoServer.Error.LARGER_CONTENT_LENGTH) {
                httpResponse.replaceHeader(CONTENT_LENGTH.toString(), String.valueOf(length * 2));
            } else if (error == EchoServer.Error.SMALLER_CONTENT_LENGTH) {
                httpResponse.replaceHeader(CONTENT_LENGTH.toString(), String.valueOf(length / 2));
            } else {
                httpResponse.replaceHeader(CONTENT_LENGTH.toString(), String.valueOf(length));
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("EchoServer returning response {} for request {}",
                    httpResponse, request);
            }

            // write and flush
            ctx.writeAndFlush(httpResponse);

            if (error == EchoServer.Error.LARGER_CONTENT_LENGTH || error == EchoServer.Error.SMALLER_CONTENT_LENGTH) {
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Echo server caught exception", cause);
        if (!lastRequest.httpRequest.get().isDone()) {
            lastRequest.httpRequest.get().completeExceptionally(cause);
        }
        ctx.close();
    }
}
