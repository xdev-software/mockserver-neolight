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

import software.xdev.mockserver.model.Delay;
import software.xdev.mockserver.model.HttpForward;
import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;

public class HttpForwardDTO extends ObjectWithReflectiveEqualsHashCodeToString implements DTO<HttpForward> {
    private String host;
    private Integer port;
    private HttpForward.Scheme scheme;
    private DelayDTO delay;

    public HttpForwardDTO(HttpForward httpForward) {
        if (httpForward != null) {
            host = httpForward.getHost();
            port = httpForward.getPort();
            scheme = httpForward.getScheme();
            if (httpForward.getDelay() != null) {
                delay = new DelayDTO(httpForward.getDelay());
            }
        }
    }

    public HttpForwardDTO() {
    }

    public HttpForward buildObject() {
        Delay delay = null;
        if (this.delay != null) {
            delay = this.delay.buildObject();
        }
        return new HttpForward()
            .withHost(host)
            .withPort(port != null ? port : 80)
            .withScheme((scheme != null ? scheme : HttpForward.Scheme.HTTP))
            .withDelay(delay);
    }

    public String getHost() {
        return host;
    }

    public HttpForwardDTO setHost(String host) {
        this.host = host;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public HttpForwardDTO setPort(Integer port) {
        this.port = port;
        return this;
    }

    public HttpForward.Scheme getScheme() {
        return scheme;
    }

    public HttpForwardDTO setScheme(HttpForward.Scheme scheme) {
        this.scheme = scheme;
        return this;
    }

    public DelayDTO getDelay() {
        return delay;
    }

    public void setDelay(DelayDTO delay) {
        this.delay = delay;
    }
}

