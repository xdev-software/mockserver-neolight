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

import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import software.xdev.mockserver.codec.BodyServletDecoderEncoder;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.Cookie;
import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.NottableString;

import jakarta.servlet.http.HttpServletResponse;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

public class MockServerHttpResponseToHttpServletResponseEncoder {

    private final BodyServletDecoderEncoder bodyDecoderEncoder;

    public MockServerHttpResponseToHttpServletResponseEncoder(MockServerLogger mockServerLogger) {
        bodyDecoderEncoder = new BodyServletDecoderEncoder(mockServerLogger);
    }

    public void mapMockServerResponseToHttpServletResponse(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        setStatusCode(httpResponse, httpServletResponse);
        setHeaders(httpResponse, httpServletResponse);
        setCookies(httpResponse, httpServletResponse);
        setBody(httpResponse, httpServletResponse);
    }

    @SuppressWarnings("deprecation")
    private void setStatusCode(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        httpServletResponse.setStatus(httpResponse.getStatusCode());
    }

    private void setHeaders(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getHeaderList() != null) {
            for (Header header : httpResponse.getHeaderList()) {
                String headerName = header.getName().getValue();
                if (!headerName.equalsIgnoreCase(CONTENT_LENGTH.toString())
                    && !headerName.equalsIgnoreCase(TRANSFER_ENCODING.toString())
                    && !headerName.equalsIgnoreCase(HOST.toString())
                    && !headerName.equalsIgnoreCase(ACCEPT_ENCODING.toString())
                    && !headerName.equalsIgnoreCase(CONNECTION.toString())) {
                    for (NottableString value : header.getValues()) {
                        httpServletResponse.addHeader(headerName, value.getValue());
                    }
                }
            }
        }
        addContentTypeHeader(httpResponse, httpServletResponse);
    }

    private void setCookies(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpResponse.getCookieList() != null) {
            for (Cookie cookie : httpResponse.getCookieList()) {
                if (httpResponse.cookieHeaderDoesNotAlreadyExists(cookie)) {
                    httpServletResponse.addHeader(SET_COOKIE.toString(), ServerCookieEncoder.LAX.encode(new DefaultCookie(cookie.getName().getValue(), cookie.getValue().getValue())));
                }
            }
        }
    }

    private void setBody(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        bodyDecoderEncoder.bodyToServletResponse(httpServletResponse, httpResponse.getBody(), httpResponse.getFirstHeader(CONTENT_TYPE.toString()));
    }

    private void addContentTypeHeader(HttpResponse httpResponse, HttpServletResponse httpServletResponse) {
        if (httpServletResponse.getContentType() == null
            && httpResponse.getBody() != null
            && httpResponse.getBody().getContentType() != null) {
            httpServletResponse.addHeader(CONTENT_TYPE.toString(), httpResponse.getBody().getContentType());
        }
    }
}
