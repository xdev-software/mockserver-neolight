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

import java.util.Objects;

public class HttpForward extends Action<HttpForward> {
    private int hashCode;
    private String host;
    private Integer port = 80;
    private Scheme scheme = Scheme.HTTP;

    /**
     * Static builder to create a forward.
     */
    public static HttpForward forward() {
        return new HttpForward();
    }

    @Override
    @JsonIgnore
    public Type getType() {
        return Type.FORWARD;
    }

    public String getHost() {
        return host;
    }

    /**
     * The host or ip address to forward the request to i.e. "www.mock-server.com"
     *
     * @param host a hostname or ip address as a string
     */
    public HttpForward withHost(String host) {
        this.host = host;
        this.hashCode = 0;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    /**
     * The port to forward the request to i.e. 80.  If not specified the port defaults to 80.
     *
     * @param port a port as an integer
     */
    public HttpForward withPort(Integer port) {
        this.port = port;
        this.hashCode = 0;
        return this;
    }

    public Scheme getScheme() {
        return scheme;
    }

    /**
     * The scheme to use when forwarded the request, either HTTP or HTTPS.  If not specified the scheme defaults to HTTP.
     *
     * @param scheme the scheme as a HttpForward.Scheme value
     */
    public HttpForward withScheme(Scheme scheme) {
        this.scheme = scheme;
        this.hashCode = 0;
        return this;
    }

    public enum Scheme {
        HTTP,
        HTTPS
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
        HttpForward that = (HttpForward) o;
        return Objects.equals(host, that.host) &&
            Objects.equals(port, that.port) &&
            scheme == that.scheme;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), host, port, scheme);
        }
        return hashCode;
    }
}
