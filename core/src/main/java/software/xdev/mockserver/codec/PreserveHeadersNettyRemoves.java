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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import software.xdev.mockserver.model.Header;

import java.util.List;

public class PreserveHeadersNettyRemoves extends MessageToMessageDecoder<HttpObject> {

    private static final AttributeKey<List<Header>> PRESERVED_HEADERS = AttributeKey.valueOf("PRESERVED_HEADERS");

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject httpObject, List<Object> out) throws Exception {
        if (httpObject instanceof HttpMessage httpMessage) {
            final HttpHeaders headers = httpMessage.headers();
            if (headers.contains(HttpHeaderNames.CONTENT_ENCODING)) {
                ctx.channel().attr(PRESERVED_HEADERS).set(List.of(
                    new Header(HttpHeaderNames.CONTENT_ENCODING.toString(), headers.getAll(HttpHeaderNames.CONTENT_ENCODING))
                ));
            }
        }
        ReferenceCountUtil.retain(httpObject);
        out.add(httpObject);
    }

    public static List<Header> preservedHeaders(Channel channel) {
        if (channel.attr(PRESERVED_HEADERS) != null && channel.attr(PRESERVED_HEADERS).get() != null) {
            return channel.attr(PRESERVED_HEADERS).get();
        }
        return List.of();
    }

}
