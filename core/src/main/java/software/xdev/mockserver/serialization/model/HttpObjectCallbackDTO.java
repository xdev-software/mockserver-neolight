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

import software.xdev.mockserver.model.Delay;
import software.xdev.mockserver.model.HttpObjectCallback;

public class HttpObjectCallbackDTO implements DTO<HttpObjectCallback> {

    private String clientId;
    private Boolean responseCallback;
    private DelayDTO delay;

    public HttpObjectCallbackDTO(HttpObjectCallback httpObjectCallback) {
        if (httpObjectCallback != null) {
            clientId = httpObjectCallback.getClientId();
            responseCallback = httpObjectCallback.getResponseCallback();
            if (httpObjectCallback.getDelay() != null) {
                delay = new DelayDTO(httpObjectCallback.getDelay());
            }
        }
    }

    public HttpObjectCallbackDTO() {
    }

    public HttpObjectCallback buildObject() {
        Delay delay = null;
        if (this.delay != null) {
            delay = this.delay.buildObject();
        }
        return new HttpObjectCallback()
            .withClientId(clientId)
            .withResponseCallback(responseCallback)
            .withDelay(delay);
    }

    public String getClientId() {
        return clientId;
    }

    public HttpObjectCallbackDTO setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public Boolean getResponseCallback() {
        return responseCallback;
    }

    public HttpObjectCallbackDTO setResponseCallback(Boolean responseCallback) {
        this.responseCallback = responseCallback;
        return this;
    }

    public DelayDTO getDelay() {
        return delay;
    }

    public void setDelay(DelayDTO delay) {
        this.delay = delay;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final HttpObjectCallbackDTO that))
        {
            return false;
        }
		return Objects.equals(getClientId(), that.getClientId())
            && Objects.equals(getResponseCallback(), that.getResponseCallback()) && Objects.equals(
            getDelay(),
            that.getDelay());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(getClientId(), getResponseCallback(), getDelay());
    }
}

