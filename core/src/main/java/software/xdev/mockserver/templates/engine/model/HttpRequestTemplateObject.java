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
package software.xdev.mockserver.templates.engine.model;

import software.xdev.mockserver.model.*;
import software.xdev.mockserver.serialization.model.BodyDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HttpRequestTemplateObject extends RequestDefinition {
    private int hashCode;
    private String method = "";
    private String path = "";
    private final Map<String, List<String>> pathParameters = new HashMap<>();
    private final Map<String, List<String>> queryStringParameters = new HashMap<>();
    private final Map<String, String> cookies = new HashMap<>();
    private final Map<String, List<String>> headers = new HashMap<>();
    private BodyDTO body = null;
    private Boolean secure = null;
    private List<X509Certificate> clientCertificateChain = null;
    private String localAddress = null;
    private String remoteAddress = null;
    private Boolean keepAlive = null;

    public HttpRequestTemplateObject(HttpRequest httpRequest) {
        if (httpRequest != null) {
            method = httpRequest.getMethod().getValue();
            path = httpRequest.getPath().getValue();
            for (Parameter parameter : httpRequest.getPathParameterList()) {
                pathParameters.put(parameter.getName().getValue(), parameter.getValues().stream().map(NottableString::getValue).collect(Collectors.toList()));
            }
            for (Parameter parameter : httpRequest.getQueryStringParameterList()) {
                queryStringParameters.put(parameter.getName().getValue(), parameter.getValues().stream().map(NottableString::getValue).collect(Collectors.toList()));
            }
            for (Header header : httpRequest.getHeaderList()) {
                headers.put(header.getName().getValue(), header.getValues().stream().map(NottableString::getValue).collect(Collectors.toList()));
            }
            for (Cookie cookie : httpRequest.getCookieList()) {
                cookies.put(cookie.getName().getValue(), cookie.getValue().getValue());
            }
            body = BodyDTO.createDTO(httpRequest.getBody());
            secure = httpRequest.isSecure();
            clientCertificateChain = httpRequest.getClientCertificateChain();
            localAddress = httpRequest.getLocalAddress();
            remoteAddress = httpRequest.getRemoteAddress();
            keepAlive = httpRequest.isKeepAlive();
            setNot(httpRequest.getNot());
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, List<String>> getPathParameters() {
        return pathParameters;
    }

    public Map<String, List<String>> getQueryStringParameters() {
        return queryStringParameters;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getBody() {
        return BodyDTO.toString(body);
    }

    @Deprecated
    public String getBodyAsString() {
        return BodyDTO.toString(body);
    }

    public Boolean getSecure() {
        return secure;
    }

    public List<X509Certificate> getClientCertificateChain() {
        return clientCertificateChain;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public HttpRequestTemplateObject shallowClone() {
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
        HttpRequestTemplateObject that = (HttpRequestTemplateObject) o;
        return Objects.equals(method, that.method) &&
            Objects.equals(path, that.path) &&
            Objects.equals(pathParameters, that.pathParameters) &&
            Objects.equals(queryStringParameters, that.queryStringParameters) &&
            Objects.equals(cookies, that.cookies) &&
            Objects.equals(headers, that.headers) &&
            Objects.equals(body, that.body) &&
            Objects.equals(secure, that.secure) &&
            Objects.equals(localAddress, that.localAddress) &&
            Objects.equals(remoteAddress, that.remoteAddress) &&
            Objects.equals(keepAlive, that.keepAlive);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), method, path, pathParameters, queryStringParameters, cookies, headers, body, secure, localAddress, remoteAddress, keepAlive);
        }
        return hashCode;
    }
}
