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

import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.ObjectWithJsonToString;

public class HttpRequestAndHttpResponseDTO extends ObjectWithJsonToString implements DTO<HttpRequestAndHttpResponse> {

    private HttpRequestDTO httpRequest;
    private HttpResponseDTO httpResponse;

    public HttpRequestAndHttpResponseDTO() {
    }

    public HttpRequestAndHttpResponseDTO(HttpRequestAndHttpResponse httpRequestAndHttpResponse) {
        if (httpRequestAndHttpResponse != null) {
            HttpRequest httpRequest = httpRequestAndHttpResponse.getHttpRequest();
            if (httpRequest != null) {
                this.httpRequest = new HttpRequestDTO(httpRequest);
            }
            HttpResponse httpResponse = httpRequestAndHttpResponse.getHttpResponse();
            if (httpResponse != null) {
                this.httpResponse = new HttpResponseDTO(httpResponse);
            }
        }
    }

    @Override
    public HttpRequestAndHttpResponse buildObject() {
        HttpRequest httpRequest = null;
        HttpResponse httpResponse = null;
        if (this.httpRequest != null) {
            httpRequest = this.httpRequest.buildObject();
        }
        if (this.httpResponse != null) {
            httpResponse = this.httpResponse.buildObject();
        }
        return new HttpRequestAndHttpResponse()
            .withHttpRequest(httpRequest)
            .withHttpResponse(httpResponse);
    }

    public HttpRequestDTO getHttpRequest() {
        return httpRequest;
    }

    public HttpRequestAndHttpResponseDTO setHttpRequest(HttpRequestDTO httpRequest) {
        this.httpRequest = httpRequest;
        return this;
    }

    public HttpResponseDTO getHttpResponse() {
        return httpResponse;
    }

    public HttpRequestAndHttpResponseDTO setHttpResponse(HttpResponseDTO httpResponse) {
        this.httpResponse = httpResponse;
        return this;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final HttpRequestAndHttpResponseDTO that))
        {
            return false;
        }
		return Objects.equals(getHttpRequest(), that.getHttpRequest())
            && Objects.equals(getHttpResponse(), that.getHttpResponse());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(getHttpRequest(), getHttpResponse());
    }
}
