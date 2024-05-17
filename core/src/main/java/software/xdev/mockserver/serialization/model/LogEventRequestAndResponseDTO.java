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

import software.xdev.mockserver.model.*;

public class LogEventRequestAndResponseDTO extends ObjectWithJsonToString implements DTO<LogEventRequestAndResponse> {

    private String timestamp;
    private RequestDefinitionDTO httpRequest;
    private HttpResponseDTO httpResponse;

    public LogEventRequestAndResponseDTO() {
    }

    public LogEventRequestAndResponseDTO(LogEventRequestAndResponse httpRequestAndHttpResponse) {
        if (httpRequestAndHttpResponse != null) {
            RequestDefinition httpRequest = httpRequestAndHttpResponse.getHttpRequest();
            if (httpRequest instanceof HttpRequest) {
                this.httpRequest = new HttpRequestDTO((HttpRequest) httpRequest);
            }
            HttpResponse httpResponse = httpRequestAndHttpResponse.getHttpResponse();
            if (httpResponse != null) {
                this.httpResponse = new HttpResponseDTO(httpResponse);
            }
            timestamp = httpRequestAndHttpResponse.getTimestamp();
        }
    }

    @Override
    public LogEventRequestAndResponse buildObject() {
        RequestDefinition httpRequest = null;
        HttpResponse httpResponse = null;
        if (this.httpRequest != null) {
            httpRequest = this.httpRequest.buildObject();
        }
        if (this.httpResponse != null) {
            httpResponse = this.httpResponse.buildObject();
        }
        return new LogEventRequestAndResponse()
            .withHttpRequest(httpRequest)
            .withHttpResponse(httpResponse)
            .withTimestamp(timestamp);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public RequestDefinitionDTO getHttpRequest() {
        return httpRequest;
    }

    public void setHttpRequest(HttpRequestDTO httpRequest) {
        this.httpRequest = httpRequest;
    }

    public HttpResponseDTO getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpResponseDTO httpResponse) {
        this.httpResponse = httpResponse;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final LogEventRequestAndResponseDTO that))
        {
            return false;
        }
		return Objects.equals(getTimestamp(), that.getTimestamp())
            && Objects.equals(getHttpRequest(), that.getHttpRequest())
            && Objects.equals(getHttpResponse(), that.getHttpResponse());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(getTimestamp(), getHttpRequest(), getHttpResponse());
    }
}
