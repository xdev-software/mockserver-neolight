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
package software.xdev.mockserver.dashboard;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static software.xdev.mockserver.mock.HttpState.PATH_PREFIX;
import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;
import static software.xdev.mockserver.model.HttpResponse.response;

public class DashboardHandler {

    private static final Map<String, String> MIME_MAP = new HashMap<>();
    private static final List<String> IS_STRING_CONTENT = ImmutableList.of(
        "css",
        "js",
        "map",
        "json",
        "html"
    );

    public DashboardHandler() {
        MIME_MAP.put("css", "text/css; charset=utf-8");
        MIME_MAP.put("js", "application/javascript; charset=UTF-8");
        MIME_MAP.put("map", "application/json; charset=UTF-8");
        MIME_MAP.put("json", "application/json; charset=UTF-8");
        MIME_MAP.put("html", "text/html; charset=utf-8");
        MIME_MAP.put("ico", "image/x-icon");
        MIME_MAP.put("woff2", "application/font-woff2");
        MIME_MAP.put("ttf", "application/octet-stream");
        MIME_MAP.put("png", "image/png");
    }

    public void renderDashboard(final ChannelHandlerContext ctx, final HttpRequest request) throws Exception {
        HttpResponse response = notFoundResponse();
        if (request.getMethod().getValue().equals("GET")) {
            String path = substringAfter(request.getPath().getValue(), PATH_PREFIX + "/dashboard");
            if (path.isEmpty() || path.equals("/")) {
                path = "/index.html";
            }
            try (InputStream contentStream = DashboardHandler.class.getResourceAsStream("/org/mockserver/dashboard" + path)) {
                if (contentStream != null) {
                    final String extension = substringAfterLast(path, ".");
                    if (IS_STRING_CONTENT.contains(extension)) {
                        final String content = new String(ByteStreams.toByteArray(contentStream), UTF_8.name());
                        response =
                            response()
                                .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MIME_MAP.get(extension))
                                .withHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), String.valueOf(content.getBytes().length))
                                .withBody(content);
                    } else {
                        final byte[] bytes = ByteStreams.toByteArray(contentStream);
                        response =
                            response()
                                .withHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MIME_MAP.get(extension))
                                .withHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), String.valueOf(bytes.length))
                                .withBody(bytes);
                    }
                    if (request.isKeepAlive()) {
                        response.withHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
                    }
                }
            }
        }
        if (!request.isKeepAlive()) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }
}
