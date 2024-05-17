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

import java.util.Objects;

import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpResponse;

public class HttpResponseDTO implements DTO<HttpResponse> {
    private Integer statusCode;
    private String reasonPhrase;
    private BodyWithContentTypeDTO body;
    private Cookies cookies;
    private Headers headers;
    private DelayDTO delay;
    private ConnectionOptionsDTO connectionOptions;

    public HttpResponseDTO() {
    }

    public HttpResponseDTO(HttpResponse httpResponse) {
        if (httpResponse != null) {
            statusCode = httpResponse.getStatusCode();
            reasonPhrase = httpResponse.getReasonPhrase();
            body = BodyWithContentTypeDTO.createWithContentTypeDTO(httpResponse.getBody());
            headers = httpResponse.getHeaders();
            cookies = httpResponse.getCookies();
            delay = (httpResponse.getDelay() != null ? new DelayDTO(httpResponse.getDelay()) : null);
            connectionOptions = (httpResponse.getConnectionOptions() != null ? new ConnectionOptionsDTO(httpResponse.getConnectionOptions()) : null);
        }
    }

    public HttpResponse buildObject() {
        return new HttpResponse()
            .withStatusCode(statusCode)
            .withReasonPhrase(reasonPhrase)
            .withBody(body != null ? body.buildObject() : null)
            .withHeaders(headers)
            .withCookies(cookies)
            .withDelay((delay != null ? delay.buildObject() : null))
            .withConnectionOptions(connectionOptions != null ? connectionOptions.buildObject() : null);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public HttpResponseDTO setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public HttpResponseDTO setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    public BodyWithContentTypeDTO getBody() {
        return body;
    }

    public HttpResponseDTO setBody(BodyWithContentTypeDTO body) {
        this.body = body;
        return this;
    }

    public Headers getHeaders() {
        return headers;
    }

    public HttpResponseDTO setHeaders(Headers headers) {
        this.headers = headers;
        return this;
    }

    public Cookies getCookies() {
        return cookies;
    }

    public HttpResponseDTO setCookies(Cookies cookies) {
        this.cookies = cookies;
        return this;
    }

    public DelayDTO getDelay() {
        return delay;
    }

    public HttpResponseDTO setDelay(DelayDTO delay) {
        this.delay = delay;
        return this;
    }

    public ConnectionOptionsDTO getConnectionOptions() {
        return connectionOptions;
    }

    public HttpResponseDTO setConnectionOptions(ConnectionOptionsDTO connectionOptions) {
        this.connectionOptions = connectionOptions;
        return this;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final HttpResponseDTO that))
        {
            return false;
        }
		return Objects.equals(getStatusCode(), that.getStatusCode())
            && Objects.equals(getReasonPhrase(), that.getReasonPhrase()) && Objects.equals(
            getBody(),
            that.getBody()) && Objects.equals(getCookies(), that.getCookies()) && Objects.equals(
            getHeaders(),
            that.getHeaders()) && Objects.equals(getDelay(), that.getDelay()) && Objects.equals(
            getConnectionOptions(),
            that.getConnectionOptions());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(
            getStatusCode(),
            getReasonPhrase(),
            getBody(),
            getCookies(),
            getHeaders(),
            getDelay(),
            getConnectionOptions());
    }
}
