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
package software.xdev.mockserver.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class HttpOverrideForwardedRequest extends Action<HttpOverrideForwardedRequest> {
    private int hashCode;
    @JsonAlias("httpRequest")
    private HttpRequest requestOverride;
    private HttpRequestModifier requestModifier;
    @JsonAlias("httpResponse")
    private HttpResponse responseOverride;
    private HttpResponseModifier responseModifier;

    /**
     * Static builder which will allow overriding proxied request with the specified request.
     */
    public static HttpOverrideForwardedRequest forwardOverriddenRequest() {
        return new HttpOverrideForwardedRequest();
    }

    /**
     * Static builder which will allow overriding proxied request with the specified request.
     *
     * @param httpRequest the HttpRequest specifying what to override
     */
    public static HttpOverrideForwardedRequest forwardOverriddenRequest(HttpRequest httpRequest) {
        return new HttpOverrideForwardedRequest().withRequestOverride(httpRequest);
    }

    /**
     * Static builder which will allow overriding or modifying proxied request with the specified request.
     *
     * @param httpRequest     the HttpRequest specifying what to override
     * @param requestModifier what to modify in the request
     */
    public static HttpOverrideForwardedRequest forwardOverriddenRequest(HttpRequest httpRequest, HttpRequestModifier requestModifier) {
        return new HttpOverrideForwardedRequest()
            .withRequestOverride(httpRequest)
            .withRequestModifier(requestModifier);
    }

    /**
     * Static builder which will allow overriding proxied request with the specified request.
     *
     * @param httpRequest  the HttpRequest specifying what to override
     * @param httpResponse the HttpRequest specifying what to override
     */
    public static HttpOverrideForwardedRequest forwardOverriddenRequest(HttpRequest httpRequest, HttpResponse httpResponse) {
        return new HttpOverrideForwardedRequest()
            .withRequestOverride(httpRequest)
            .withResponseOverride(httpResponse);
    }


    /**
     * Static builder which will allow overriding proxied request with the specified request.
     *
     * @param httpRequest      the HttpRequest specifying what to override
     * @param requestModifier  what to modify in the request
     * @param httpResponse     the HttpRequest specifying what to override
     * @param responseModifier what to modify in the response
     */
    public static HttpOverrideForwardedRequest forwardOverriddenRequest(HttpRequest httpRequest, HttpRequestModifier requestModifier, HttpResponse httpResponse, HttpResponseModifier responseModifier) {
        return new HttpOverrideForwardedRequest()
            .withRequestOverride(httpRequest)
            .withResponseModifier(responseModifier)
            .withResponseOverride(httpResponse)
            .withRequestModifier(requestModifier);
    }

    public HttpRequest getRequestOverride() {
        return requestOverride;
    }

    /**
     * All fields, headers, cookies, etc of the provided request will be overridden
     *
     * @param httpRequest the HttpRequest specifying what to override
     */
    public HttpOverrideForwardedRequest withRequestOverride(HttpRequest httpRequest) {
        this.requestOverride = httpRequest;
        this.hashCode = 0;
        return this;
    }

    public HttpRequestModifier getRequestModifier() {
        return requestModifier;
    }

    /**
     * Allow path, query parameters, headers and cookies to be modified
     *
     * @param requestModifier what to modify
     */
    public HttpOverrideForwardedRequest withRequestModifier(HttpRequestModifier requestModifier) {
        this.requestModifier = requestModifier;
        this.hashCode = 0;
        return this;
    }

    public HttpResponse getResponseOverride() {
        return responseOverride;
    }

    /**
     * All fields, headers, cookies, etc of the provided response will be overridden
     *
     * @param httpResponse the HttpResponse specifying what to override
     */
    public HttpOverrideForwardedRequest withResponseOverride(HttpResponse httpResponse) {
        this.responseOverride = httpResponse;
        this.hashCode = 0;
        return this;
    }

    public HttpResponseModifier getResponseModifier() {
        return responseModifier;
    }

    /**
     * Allow headers and cookies to be modified
     *
     * @param responseModifier what to modify
     */
    public HttpOverrideForwardedRequest withResponseModifier(HttpResponseModifier responseModifier) {
        this.responseModifier = responseModifier;
        this.hashCode = 0;
        return this;
    }

    @Override
    @JsonIgnore
    public Type getType() {
        return Type.FORWARD_REPLACE;
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
        HttpOverrideForwardedRequest that = (HttpOverrideForwardedRequest) o;
        return Objects.equals(requestOverride, that.requestOverride) &&
            Objects.equals(requestModifier, that.requestModifier) &&
            Objects.equals(responseOverride, that.responseOverride) &&
            Objects.equals(responseModifier, that.responseModifier);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), requestOverride, requestModifier, responseOverride, responseModifier);
        }
        return hashCode;
    }
}
