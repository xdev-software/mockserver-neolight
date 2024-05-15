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

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http2.HttpConversionUtil;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.codec.BodyDecoderEncoder;
import software.xdev.mockserver.codec.ExpandedParameterDecoder;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.url.URLParser;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

public class FullHttpRequestToMockServerHttpRequest {

    private final MockServerLogger mockServerLogger;
    private final BodyDecoderEncoder bodyDecoderEncoder;
    private final ExpandedParameterDecoder formParameterParser;
    private final boolean isSecure;
    private final Certificate[] clientCertificates;
    private final Integer port;
    private final JDKCertificateToMockServerX509Certificate jdkCertificateToMockServerX509Certificate;

    public FullHttpRequestToMockServerHttpRequest(Configuration configuration, MockServerLogger mockServerLogger, boolean isSecure, Certificate[] clientCertificates, Integer port) {
        this.mockServerLogger = mockServerLogger;
        this.bodyDecoderEncoder = new BodyDecoderEncoder();
        this.formParameterParser = new ExpandedParameterDecoder(configuration, mockServerLogger);
        this.isSecure = isSecure;
        this.clientCertificates = clientCertificates;
        this.port = port;
        this.jdkCertificateToMockServerX509Certificate = new JDKCertificateToMockServerX509Certificate(mockServerLogger);
    }

    public HttpRequest mapFullHttpRequestToMockServerRequest(FullHttpRequest fullHttpRequest, List<Header> preservedHeaders, SocketAddress localAddress, SocketAddress remoteAddress, Protocol protocol) {
        HttpRequest httpRequest = new HttpRequest();
        try {
            if (fullHttpRequest != null) {
                if (fullHttpRequest.decoderResult().isFailure()) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setMessageFormat("exception decoding request " + fullHttpRequest.decoderResult().cause().getMessage())
                            .setThrowable(fullHttpRequest.decoderResult().cause())
                    );
                }
                setMethod(httpRequest, fullHttpRequest);
                httpRequest.withKeepAlive(isKeepAlive(fullHttpRequest));
                httpRequest.withSecure(isSecure);
                httpRequest.withProtocol(protocol == null ? Protocol.HTTP_1_1 : protocol);

                setPath(httpRequest, fullHttpRequest);
                setQueryString(httpRequest, fullHttpRequest);
                setHeaders(httpRequest, fullHttpRequest, preservedHeaders);
                setCookies(httpRequest, fullHttpRequest);
                setBody(httpRequest, fullHttpRequest);
                setSocketAddress(httpRequest, fullHttpRequest, isSecure, port, localAddress, remoteAddress);
                jdkCertificateToMockServerX509Certificate.setClientCertificates(httpRequest, clientCertificates);
            }
        } catch (Throwable throwable) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception decoding request{}")
                    .setArguments(fullHttpRequest)
                    .setThrowable(throwable)
            );
        }
        return httpRequest;
    }

    private void setSocketAddress(HttpRequest httpRequest, FullHttpRequest fullHttpRequest, boolean isSecure, Integer port, SocketAddress localAddress, SocketAddress remoteAddress) {
        httpRequest.withSocketAddress(isSecure, fullHttpRequest.headers().get("host"), port);
        if (remoteAddress instanceof InetSocketAddress) {
            httpRequest.withRemoteAddress(StringUtils.removeStart(remoteAddress.toString(), "/"));
        }
        if (localAddress instanceof InetSocketAddress) {
            httpRequest.withLocalAddress(StringUtils.removeStart(localAddress.toString(), "/"));
        }
    }

    private void setMethod(HttpRequest httpRequest, FullHttpRequest fullHttpResponse) {
        httpRequest.withMethod(fullHttpResponse.method().name());
    }

    private void setPath(HttpRequest httpRequest, FullHttpRequest fullHttpRequest) {
        httpRequest.withPath(URLParser.returnPath(fullHttpRequest.uri()));
    }

    private void setQueryString(HttpRequest httpRequest, FullHttpRequest fullHttpRequest) {
        if (fullHttpRequest.uri().contains("?")) {
            httpRequest.withQueryStringParameters(formParameterParser.retrieveQueryParameters(fullHttpRequest.uri(), true));
        }
    }

    private void setHeaders(HttpRequest httpRequest, FullHttpRequest fullHttpResponse, List<Header> preservedHeaders) {
        HttpHeaders httpHeaders = fullHttpResponse.headers();
        if (!httpHeaders.isEmpty()) {
            Headers headers = new Headers();
            for (String headerName : httpHeaders.names()) {
                headers.withEntry(headerName, httpHeaders.getAll(headerName));
            }
            httpRequest.withHeaders(headers);
        }
        if (preservedHeaders != null && !preservedHeaders.isEmpty()) {
            for (Header preservedHeader : preservedHeaders) {
                httpRequest.withHeader(preservedHeader);
            }
        }
        if (Protocol.HTTP_2.equals(httpRequest.getProtocol())) {
            Integer streamId = fullHttpResponse.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
            httpRequest.withStreamId(streamId);
        }
    }

    private void setCookies(HttpRequest httpRequest, FullHttpRequest fullHttpResponse) {
        List<String> cookieHeaders = fullHttpResponse.headers().getAll(COOKIE);
        if (!cookieHeaders.isEmpty()) {
            Cookies cookies = new Cookies();
            for (String cookieHeader : cookieHeaders) {
                Set<Cookie> decodedCookies = ServerCookieDecoder.LAX.decode(cookieHeader);
                for (io.netty.handler.codec.http.cookie.Cookie decodedCookie : decodedCookies) {
                    cookies.withEntry(
                        decodedCookie.name(),
                        decodedCookie.value()
                    );
                }
            }
            httpRequest.withCookies(cookies);
        }
    }

    private void setBody(HttpRequest httpRequest, FullHttpRequest fullHttpRequest) {
        httpRequest.withBody(bodyDecoderEncoder.byteBufToBody(fullHttpRequest.content(), fullHttpRequest.headers().get(CONTENT_TYPE)));
    }
}
