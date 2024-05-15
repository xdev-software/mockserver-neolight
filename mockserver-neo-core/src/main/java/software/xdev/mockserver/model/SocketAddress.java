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

public class SocketAddress extends ObjectWithJsonToString {
    private String host;
    private Integer port = 80;
    private Scheme scheme = Scheme.HTTP;

    /**
     * Static builder to create a socketAddress.
     */
    public static SocketAddress socketAddress() {
        return new SocketAddress();
    }

    public String getHost() {
        return host;
    }

    /**
     * The host or ip address to use when connecting to the socket to i.e. "www.mock-server.com"
     *
     * @param host a hostname or ip address as a string
     */
    public SocketAddress withHost(String host) {
        this.host = host;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    /**
     * The port to use when connecting to the socket i.e. 80.  If not specified the port defaults to 80.
     *
     * @param port a port as an integer
     */
    public SocketAddress withPort(Integer port) {
        this.port = port;
        return this;
    }

    public SocketAddress.Scheme getScheme() {
        return scheme;
    }

    /**
     * The scheme to use when connecting to the socket, either HTTP or HTTPS.  If not specified the scheme defaults to HTTP.
     *
     * @param scheme the scheme as a SocketAddress.Scheme value
     */
    public SocketAddress withScheme(Scheme scheme) {
        this.scheme = scheme;
        return this;
    }

    public enum Scheme {
        HTTP,
        HTTPS
    }
}
