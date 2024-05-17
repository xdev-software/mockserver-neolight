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

import software.xdev.mockserver.model.HttpRequestModifier;
import software.xdev.mockserver.model.PathModifier;

public class HttpRequestModifierDTO implements DTO<HttpRequestModifier> {

    private PathModifier path;
    private QueryParametersModifierDTO queryStringParameters;
    private HeadersModifierDTO headers;
    private CookiesModifierDTO cookies;

    public HttpRequestModifierDTO() {
    }

    public HttpRequestModifierDTO(HttpRequestModifier httpRequestModifier) {
        if (httpRequestModifier != null) {
            path = httpRequestModifier.getPath();
            queryStringParameters = httpRequestModifier.getQueryStringParameters() != null ? new QueryParametersModifierDTO(httpRequestModifier.getQueryStringParameters()) : null;
            headers = httpRequestModifier.getHeaders() != null ? new HeadersModifierDTO(httpRequestModifier.getHeaders()) : null;
            cookies = httpRequestModifier.getCookies() != null ? new CookiesModifierDTO(httpRequestModifier.getCookies()) : null;
        }
    }

    public HttpRequestModifier buildObject() {
        return new HttpRequestModifier()
            .withPath(path)
            .withQueryStringParameters(queryStringParameters != null ? queryStringParameters.buildObject() : null)
            .withHeaders(headers != null ? headers.buildObject() : null)
            .withCookies(cookies != null ? cookies.buildObject() : null);
    }

    public PathModifier getPath() {
        return path;
    }

    public HttpRequestModifierDTO setPath(PathModifier path) {
        this.path = path;
        return this;
    }

    public QueryParametersModifierDTO getQueryStringParameters() {
        return queryStringParameters;
    }

    public HttpRequestModifierDTO setQueryStringParameters(QueryParametersModifierDTO queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
        return this;
    }

    public HeadersModifierDTO getHeaders() {
        return headers;
    }

    public HttpRequestModifierDTO setHeaders(HeadersModifierDTO headers) {
        this.headers = headers;
        return this;
    }

    public CookiesModifierDTO getCookies() {
        return cookies;
    }

    public HttpRequestModifierDTO setCookies(CookiesModifierDTO cookies) {
        this.cookies = cookies;
        return this;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final HttpRequestModifierDTO that))
        {
            return false;
        }
		return Objects.equals(getPath(), that.getPath())
            && Objects.equals(getQueryStringParameters(), that.getQueryStringParameters())
            && Objects.equals(getHeaders(), that.getHeaders())
            && Objects.equals(getCookies(), that.getCookies());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(getPath(), getQueryStringParameters(), getHeaders(), getCookies());
    }
}
