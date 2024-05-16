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

import java.util.List;

import static software.xdev.mockserver.model.NottableString.string;

@SuppressWarnings("UnusedReturnValue")
public class HttpRequestDTO extends RequestDefinitionDTO implements DTO<HttpRequest> {
    private NottableString method = string("");
    private NottableString path = string("");
    private Parameters pathParameters;
    private Parameters queryStringParameters;
    private BodyDTO body;
    private Cookies cookies;
    private Headers headers;
    private Boolean keepAlive;
    private Protocol protocol;
    private SocketAddress socketAddress;
    private String localAddress;
    private String remoteAddress;

    public HttpRequestDTO() {
        super(null);
    }

    public HttpRequestDTO(HttpRequest httpRequest) {
        super(httpRequest != null ? httpRequest.getNot() : null);
        if (httpRequest != null) {
            method = httpRequest.getMethod();
            path = httpRequest.getPath();
            headers = httpRequest.getHeaders();
            cookies = httpRequest.getCookies();
            pathParameters = httpRequest.getPathParameters();
            queryStringParameters = httpRequest.getQueryStringParameters();
            body = BodyDTO.createDTO(httpRequest.getBody());
            keepAlive = httpRequest.isKeepAlive();
            protocol = httpRequest.getProtocol();
            socketAddress = httpRequest.getSocketAddress();
            localAddress = httpRequest.getLocalAddress();
            remoteAddress = httpRequest.getRemoteAddress();
        }
    }

    public HttpRequest buildObject() {
        return (HttpRequest) new HttpRequest()
            .withMethod(method)
            .withPath(path)
            .withPathParameters(pathParameters)
            .withQueryStringParameters(queryStringParameters)
            .withBody((body != null ? Not.not(body.buildObject(), body.getNot()) : null))
            .withHeaders(headers)
            .withCookies(cookies)
            .withProtocol(protocol)
            .withKeepAlive(keepAlive)
            .withSocketAddress(socketAddress)
            .withLocalAddress(localAddress)
            .withRemoteAddress(remoteAddress)
            .withNot(getNot());
    }

    public NottableString getMethod() {
        return method;
    }

    public HttpRequestDTO setMethod(NottableString method) {
        this.method = method;
        return this;
    }

    public NottableString getPath() {
        return path;
    }

    public HttpRequestDTO setPath(NottableString path) {
        this.path = path;
        return this;
    }

    public Parameters getPathParameters() {
        return pathParameters;
    }

    public HttpRequestDTO setPathParameters(Parameters pathParameters) {
        this.pathParameters = pathParameters;
        return this;
    }

    public Parameters getQueryStringParameters() {
        return queryStringParameters;
    }

    public HttpRequestDTO setQueryStringParameters(Parameters queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
        return this;
    }

    public BodyDTO getBody() {
        return body;
    }

    public HttpRequestDTO setBody(BodyDTO body) {
        this.body = body;
        return this;
    }

    public Headers getHeaders() {
        return headers;
    }

    public HttpRequestDTO setHeaders(Headers headers) {
        this.headers = headers;
        return this;
    }

    public Cookies getCookies() {
        return cookies;
    }

    public HttpRequestDTO setCookies(Cookies cookies) {
        this.cookies = cookies;
        return this;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public HttpRequestDTO setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public HttpRequestDTO setProtocol(Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public HttpRequestDTO setSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
        return this;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public HttpRequestDTO setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public HttpRequestDTO setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }
}
