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

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import software.xdev.mockserver.codec.BodyServletDecoderEncoder;
import software.xdev.mockserver.codec.ExpandedParameterDecoder;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class HttpServletRequestToMockServerHttpRequestDecoder {

    private final BodyServletDecoderEncoder bodyDecoderEncoder;
    private final ExpandedParameterDecoder formParameterParser;

    public HttpServletRequestToMockServerHttpRequestDecoder(Configuration configuration, MockServerLogger mockServerLogger) {
        bodyDecoderEncoder = new BodyServletDecoderEncoder(mockServerLogger);
        formParameterParser = new ExpandedParameterDecoder(configuration, mockServerLogger);
    }

    public HttpRequest mapHttpServletRequestToMockServerRequest(HttpServletRequest httpServletRequest) {
        HttpRequest request = new HttpRequest();
        setMethod(request, httpServletRequest);

        setPath(request, httpServletRequest);
        setQueryString(request, httpServletRequest);

        setBody(request, httpServletRequest);
        setHeaders(request, httpServletRequest);
        setCookies(request, httpServletRequest);
        setSocketAddress(request, httpServletRequest);

        request.withKeepAlive(isKeepAlive(httpServletRequest));
        request.withSecure(httpServletRequest.isSecure());
        request.withProtocol(Protocol.HTTP_1_1);
        request.withLocalAddress(httpServletRequest.getLocalAddr() + ":" + httpServletRequest.getLocalPort());
        request.withRemoteAddress(httpServletRequest.getRemoteHost() + ":" + httpServletRequest.getRemotePort());
        return request;
    }

    private void setMethod(HttpRequest httpRequest, HttpServletRequest httpServletRequest) {
        httpRequest.withMethod(httpServletRequest.getMethod());
    }

    private void setPath(HttpRequest httpRequest, HttpServletRequest httpServletRequest) {
        httpRequest.withPath(httpServletRequest.getPathInfo() != null && httpServletRequest.getContextPath() != null ? httpServletRequest.getPathInfo() : httpServletRequest.getRequestURI());
    }

    private void setQueryString(HttpRequest httpRequest, HttpServletRequest httpServletRequest) {
        if (isNotBlank(httpServletRequest.getQueryString())) {
            httpRequest.withQueryStringParameters(formParameterParser.retrieveQueryParameters(httpServletRequest.getQueryString(), false));
        }
    }

    private void setBody(HttpRequest httpRequest, HttpServletRequest httpServletRequest) {
        httpRequest.withBody(bodyDecoderEncoder.servletRequestToBody(httpServletRequest));
    }

    private void setHeaders(HttpRequest httpRequest, HttpServletRequest httpServletRequest) {
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        if (headerNames.hasMoreElements()) {
            Headers headers = new Headers();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                List<String> mappedHeaderValues = new ArrayList<>();
                Enumeration<String> headerValues = httpServletRequest.getHeaders(headerName);
                while (headerValues.hasMoreElements()) {
                    mappedHeaderValues.add(headerValues.nextElement());
                }
                headers.withEntry(headerName, mappedHeaderValues);
            }
            httpRequest.withHeaders(headers);
        }
    }

    private void setCookies(HttpRequest httpRequest, HttpServletRequest httpServletRequest) {
        jakarta.servlet.http.Cookie[] httpServletRequestCookies = httpServletRequest.getCookies();
        if (httpServletRequestCookies != null && httpServletRequestCookies.length > 0) {
            Cookies cookies = new Cookies();
            for (jakarta.servlet.http.Cookie cookie : httpServletRequestCookies) {
                cookies.withEntry(new Cookie(cookie.getName(), cookie.getValue()));
            }
            httpRequest.withCookies(cookies);
        }
    }

    private void setSocketAddress(HttpRequest httpRequest, HttpServletRequest httpServletRequest) {
        httpRequest.withSocketAddress(httpServletRequest.isSecure(), httpServletRequest.getHeader("host"), httpServletRequest.getLocalPort());
    }

    public boolean isKeepAlive(HttpServletRequest httpServletRequest) {
        CharSequence connection = httpServletRequest.getHeader(HttpHeaderNames.CONNECTION.toString());
        if (HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(connection)) {
            return false;
        }

        if (httpServletRequest.getProtocol().equals("HTTP/1.1")) {
            return !HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(connection);
        } else {
            return HttpHeaderValues.KEEP_ALIVE.contentEqualsIgnoreCase(connection);
        }
    }
}
