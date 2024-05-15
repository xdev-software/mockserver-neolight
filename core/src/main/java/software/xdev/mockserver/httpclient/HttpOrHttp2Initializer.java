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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

import java.util.function.Consumer;

public class HttpOrHttp2Initializer extends ApplicationProtocolNegotiationHandler {

    private final Consumer<ChannelPipeline> http2Initializer;
    private final Consumer<ChannelPipeline> http1Initializer;

    protected HttpOrHttp2Initializer(Consumer<ChannelPipeline> http1Initializer, Consumer<ChannelPipeline> http2Initializer) {
        super("");
        this.http2Initializer = http2Initializer;
        this.http1Initializer = http1Initializer;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
        ChannelPipeline pipeline = ctx.pipeline();
        if (pipeline.get(HttpOrHttp2Initializer.class) != null) {
            pipeline.remove(HttpOrHttp2Initializer.class);
        }
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            http2Initializer.accept(pipeline);
        } else {
            http1Initializer.accept(pipeline);
        }
    }
}
