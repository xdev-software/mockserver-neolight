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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.charset.Charset;
import java.util.List;

@SuppressWarnings("rawtypes")
public interface HttpMessage<T extends HttpMessage, B extends Body> extends Message {

    T withBody(String body);

    T withBody(String body, Charset charset);

    T withBody(byte[] body);

    T withBody(B body);

    B getBody();

    @JsonIgnore
    byte[] getBodyAsRawBytes();

    @JsonIgnore
    String getBodyAsString();

    Headers getHeaders();

    T withHeaders(Headers headers);

    T withHeaders(List<Header> headers);

    T withHeaders(Header... headers);

    T withHeader(Header header);

    T withHeader(String name, String... values);

    T withHeader(NottableString name, NottableString... values);

    T withContentType(MediaType mediaType);

    T replaceHeader(Header header);

    List<Header> getHeaderList();

    List<String> getHeader(String name);

    String getFirstHeader(String name);

    boolean containsHeader(String name);

    T removeHeader(String name);

    T removeHeader(NottableString name);

    Cookies getCookies();

    T withCookies(Cookies cookies);

    T withCookies(List<Cookie> cookies);

    T withCookies(Cookie... cookies);

    T withCookie(Cookie cookie);

    T withCookie(String name, String value);

    T withCookie(NottableString name, NottableString value);

    List<Cookie> getCookieList();
}
