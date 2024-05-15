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
package software.xdev.mockserver.metrics;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;

import java.io.StringWriter;

import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;
import static software.xdev.mockserver.model.HttpResponse.response;

public class MetricsHandler {

    private final Boolean metricsEnabled;

    public MetricsHandler(Configuration configuration) {
        metricsEnabled = configuration.metricsEnabled();
    }

    public void renderMetrics(final ChannelHandlerContext ctx, final HttpRequest request) throws Exception {
        HttpResponse response = notFoundResponse();
        if (metricsEnabled) {
            StringWriter stringWriter = new StringWriter();
            String contentType = TextFormat.chooseContentType(request.getFirstHeader("Accept"));
            TextFormat.writeFormat(contentType, stringWriter, CollectorRegistry.defaultRegistry.metricFamilySamples());
            String content = stringWriter.toString();
            response =
                response()
                    .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), contentType)
                    .withHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), String.valueOf(content.getBytes().length))
                    .withBody(content);
        }
        if (!request.isKeepAlive()) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }

}
