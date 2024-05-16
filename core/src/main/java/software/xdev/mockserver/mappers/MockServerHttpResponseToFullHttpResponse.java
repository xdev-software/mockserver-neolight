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
package software.xdev.mockserver.mappers;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http2.HttpConversionUtil;
import software.xdev.mockserver.codec.BodyDecoderEncoder;
import software.xdev.mockserver.model.ConnectionOptions;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.NottableString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isEmpty;

public class MockServerHttpResponseToFullHttpResponse {
    
    private static final Logger LOG = LoggerFactory.getLogger(MockServerHttpResponseToFullHttpResponse.class);
    private final BodyDecoderEncoder bodyDecoderEncoder;

    public MockServerHttpResponseToFullHttpResponse() {
        this.bodyDecoderEncoder = new BodyDecoderEncoder();
    }

    public List<DefaultHttpObject> mapMockServerResponseToNettyResponse(HttpResponse httpResponse) {
        try {
            ConnectionOptions connectionOptions = httpResponse.getConnectionOptions();
            if (connectionOptions != null && connectionOptions.getChunkSize() != null && connectionOptions.getChunkSize() > 0) {
                List<DefaultHttpObject> httpMessages = new ArrayList<>();
                ByteBuf body = getBody(httpResponse);
                DefaultHttpResponse defaultHttpResponse = new DefaultHttpResponse(
                    HttpVersion.HTTP_1_1,
                    getStatus(httpResponse)
                );
                setHeaders(httpResponse, defaultHttpResponse, body);
                HttpUtil.setTransferEncodingChunked(defaultHttpResponse, true);
                setCookies(httpResponse, defaultHttpResponse);
                httpMessages.add(defaultHttpResponse);

                ByteBuf[] chunks = bodyDecoderEncoder.bodyToByteBuf(httpResponse.getBody(), httpResponse.getFirstHeader(CONTENT_TYPE.toString()), connectionOptions.getChunkSize());
                for (int i = 0; i < chunks.length - 1; i++) {
                    DefaultHttpContent defaultHttpContent = new DefaultHttpContent(chunks[i]);
                    httpMessages.add(defaultHttpContent);
                }
                httpMessages.add(new DefaultLastHttpContent(chunks[chunks.length - 1]));
                return httpMessages;
            } else {
                ByteBuf body = getBody(httpResponse);
                DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    getStatus(httpResponse),
                    body
                );
                setHeaders(httpResponse, defaultFullHttpResponse, body);
                setCookies(httpResponse, defaultFullHttpResponse);
                return Collections.singletonList(defaultFullHttpResponse);
            }
        } catch (Exception ex) {
            LOG.error("Exception encoding response {}", httpResponse, ex);
            return Collections.singletonList(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, getStatus(httpResponse)));
        }
    }

    private HttpResponseStatus getStatus(HttpResponse httpResponse) {
        int statusCode = httpResponse.getStatusCode() != null ? httpResponse.getStatusCode() : 200;
        if (!isEmpty(httpResponse.getReasonPhrase())) {
            return new HttpResponseStatus(statusCode, httpResponse.getReasonPhrase());
        } else {
            return HttpResponseStatus.valueOf(statusCode);
        }
    }

    private ByteBuf getBody(HttpResponse httpResponse) {
        return bodyDecoderEncoder.bodyToByteBuf(httpResponse.getBody(), httpResponse.getFirstHeader(CONTENT_TYPE.toString()));
    }

    private void setHeaders(HttpResponse httpResponse, DefaultHttpResponse response, ByteBuf body) {
        if (httpResponse.getHeaderMultimap() != null) {
            httpResponse
                .getHeaderMultimap()
                .entrySet()
                .stream()
                .flatMap(e -> e.getValue().stream().map(e2 -> Map.entry(e.getKey(), e2.getValue())))
                .forEach(entry ->
                    response
                        .headers()
                        .add(entry.getKey().getValue(), entry.getValue())
                );
        }

        // Content-Type
        if (isBlank(httpResponse.getFirstHeader(CONTENT_TYPE.toString()))) {
            if (httpResponse.getBody() != null
                && httpResponse.getBody().getContentType() != null) {
                response.headers().set(CONTENT_TYPE, httpResponse.getBody().getContentType());
            }
        }

        // Content-Length
        ConnectionOptions connectionOptions = httpResponse.getConnectionOptions();
        if (isBlank(httpResponse.getFirstHeader(CONTENT_LENGTH.toString()))) {
            boolean overrideContentLength = connectionOptions != null && connectionOptions.getContentLengthHeaderOverride() != null;
            boolean addContentLength = connectionOptions == null || !Boolean.TRUE.equals(connectionOptions.getSuppressContentLengthHeader());
            boolean chunkedEncoding = (connectionOptions != null && connectionOptions.getChunkSize() != null) || response.headers().contains(HttpHeaderNames.TRANSFER_ENCODING);
            if (overrideContentLength) {
                response.headers().set(CONTENT_LENGTH, connectionOptions.getContentLengthHeaderOverride());
            } else if (addContentLength && !chunkedEncoding) {
                response.headers().set(CONTENT_LENGTH, body.readableBytes());
            }
            if (chunkedEncoding) {
                response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            }
        }

        // HTTP2 extension headers
        Integer streamId = httpResponse.getStreamId();
        if (streamId != null) {
            response.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
        }
    }

    private void setCookies(HttpResponse httpResponse, DefaultHttpResponse response) {
        if (httpResponse.getCookieMap() != null) {
            for (Map.Entry<NottableString, NottableString> cookie : httpResponse.getCookieMap().entrySet()) {
                if (httpResponse.cookieHeaderDoesNotAlreadyExists(cookie.getKey().getValue(), cookie.getValue().getValue())) {
                    response.headers().add(SET_COOKIE, io.netty.handler.codec.http.cookie.ServerCookieEncoder.LAX.encode(new DefaultCookie(cookie.getKey().getValue(), cookie.getValue().getValue())));
                }
            }
        }
    }
}
