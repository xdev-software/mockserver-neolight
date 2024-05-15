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
package software.xdev.mockserver.serialization.curl;

import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mappers.MockServerHttpRequestToFullHttpRequest;
import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.NottableString;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static org.apache.commons.lang3.StringUtils.*;

public class HttpRequestToCurlSerializer {

    private final MockServerLogger mockServerLogger;

    public HttpRequestToCurlSerializer(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
    }

    public String toCurl(HttpRequest request) {
        return toCurl(request, null);
    }

    public String toCurl(HttpRequest request, InetSocketAddress remoteAddress) {
        StringBuilder curlString = new StringBuilder();
        if (request != null) {
            if (isNotBlank(request.getFirstHeader(HOST.toString())) || remoteAddress != null) {
                boolean isSsl = request.isSecure() != null && request.isSecure();
                curlString.append("curl -v");
                curlString.append(" ");
                curlString.append("'");
                curlString.append((isSsl ? "https" : "http"));
                curlString.append("://");
                curlString.append(getHostAndPort(request, remoteAddress));
                curlString.append(getUri(request));
                curlString.append("'");
                if (!hasDefaultMethod(request)) {
                    curlString.append(" -X ").append(request.getMethod().getValue());
                }
                for (Header header : request.getHeaderList()) {
                    for (NottableString headerValue : header.getValues()) {
                        curlString.append(" -H '").append(header.getName().getValue()).append(": ").append(headerValue.getValue()).append("'");
                        if (header.getName().getValue().toLowerCase().contains("Accept-Encoding".toLowerCase())) {
                            if (headerValue.getValue().toLowerCase().contains("gzip")
                                || headerValue.getValue().toLowerCase().contains("deflate")
                                || headerValue.getValue().toLowerCase().contains("sdch")) {
                                curlString.append(" ");
                                curlString.append("--compress");
                            }
                        }
                    }
                }
                curlString.append(getCookieHeader(request));
                if (isNotBlank(request.getBodyAsString())) {
                    curlString.append(" --data '").append(request.getBodyAsString().replace("'", "\\'")).append("'");
                }
            } else {
                curlString.append("no host header or remote address specified");
            }
        } else {
            curlString.append("null HttpRequest");
        }
        return curlString.toString();
    }

    private boolean hasDefaultMethod(HttpRequest request) {
        return request.getMethod() == null || isBlank(request.getMethod().getValue()) || request.getMethod().getValue().equalsIgnoreCase("GET");
    }

    private String getUri(HttpRequest request) {
        String uri = new MockServerHttpRequestToFullHttpRequest(mockServerLogger, null).getURI(request, null);
        if (isBlank(uri)) {
            uri = "/";
        } else if (!startsWith(uri, "/")) {
            uri = "/" + uri;
        }
        return uri;
    }

    private String getHostAndPort(HttpRequest request, InetSocketAddress remoteAddress) {
        String host = request.getFirstHeader("Host");
        if (isBlank(host)) {
            host = remoteAddress.getHostName() + ":" + remoteAddress.getPort();
        }
        return host;
    }

    private String getCookieHeader(HttpRequest request) {
        List<Cookie> cookies = new ArrayList<Cookie>();
        for (software.xdev.mockserver.model.Cookie cookie : request.getCookieList()) {
            cookies.add(new DefaultCookie(cookie.getName().getValue(), cookie.getValue().getValue()));
        }
        if (cookies.size() > 0) {
            return " -H '" + COOKIE + ": " + ClientCookieEncoder.LAX.encode(cookies) + "'";
        } else {
            return "";
        }
    }
}
