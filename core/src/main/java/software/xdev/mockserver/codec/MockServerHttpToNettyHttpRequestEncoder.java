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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import software.xdev.mockserver.mappers.MockServerHttpRequestToFullHttpRequest;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;

import java.util.List;
import java.util.Map;

public class MockServerHttpToNettyHttpRequestEncoder extends MessageToMessageEncoder<HttpRequest> {

    private final MockServerHttpRequestToFullHttpRequest mockServerHttpRequestToFullHttpRequest;

    MockServerHttpToNettyHttpRequestEncoder(MockServerLogger mockServerLogger, Map<ProxyConfiguration.Type, ProxyConfiguration> proxyConfigurations) {
        mockServerHttpRequestToFullHttpRequest = new MockServerHttpRequestToFullHttpRequest(mockServerLogger, proxyConfigurations);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpRequest httpRequest, List<Object> out) {
        out.add(mockServerHttpRequestToFullHttpRequest.mapMockServerRequestToNettyRequest(httpRequest));
    }

}
