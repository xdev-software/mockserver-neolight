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
package software.xdev.mockserver.serialization.model;

import software.xdev.mockserver.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HttpRequestPrettyPrintedDTO extends RequestDefinition {
    private int hashCode;
    private String method = "";
    private String path = "";
    private final Map<String, List<String>> queryStringParameters = new HashMap<>();
    private BodyDTO body = null;
    private final Map<String, String> cookies = new HashMap<>();
    private final Map<String, List<String>> headers = new HashMap<>();
    private Boolean keepAlive = null;
    private Boolean secure = null;

    public HttpRequestPrettyPrintedDTO(HttpRequest httpRequest) {
        if (httpRequest != null) {
            method = httpRequest.getMethod().getValue();
            path = httpRequest.getPath().getValue();
            for (Header header : httpRequest.getHeaderList()) {
                headers.put(header.getName().getValue(), header.getValues().stream().map(NottableString::getValue).collect(Collectors.toList()));
            }
            for (Cookie cookie : httpRequest.getCookieList()) {
                cookies.put(cookie.getName().getValue(), cookie.getValue().getValue());
            }
            for (Parameter parameter : httpRequest.getQueryStringParameterList()) {
                queryStringParameters.put(parameter.getName().getValue(), parameter.getValues().stream().map(NottableString::getValue).collect(Collectors.toList()));
            }
            body = BodyDTO.createDTO(httpRequest.getBody());
            keepAlive = httpRequest.isKeepAlive();
            secure = httpRequest.isSecure();
            setNot(httpRequest.getNot());
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, List<String>> getQueryStringParameters() {
        return queryStringParameters;
    }

    public BodyDTO getBody() {
        return body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public Boolean getSecure() {
        return secure;
    }

    public HttpRequestPrettyPrintedDTO shallowClone() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (hashCode() != o.hashCode()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        HttpRequestPrettyPrintedDTO that = (HttpRequestPrettyPrintedDTO) o;
        return Objects.equals(method, that.method) &&
            Objects.equals(path, that.path) &&
            Objects.equals(queryStringParameters, that.queryStringParameters) &&
            Objects.equals(body, that.body) &&
            Objects.equals(cookies, that.cookies) &&
            Objects.equals(headers, that.headers) &&
            Objects.equals(keepAlive, that.keepAlive) &&
            Objects.equals(secure, that.secure);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), method, path, queryStringParameters, body, cookies, headers, keepAlive, secure);
        }
        return hashCode;
    }
}
