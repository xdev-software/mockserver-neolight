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

import java.util.Arrays;
import java.util.Objects;

import software.xdev.mockserver.model.HttpError;

public class HttpErrorDTO implements DTO<HttpError> {
    private DelayDTO delay;
    private Boolean dropConnection;
    private byte[] responseBytes;

    public HttpErrorDTO(HttpError httpError) {
        if (httpError != null) {
            if (httpError.getDelay() != null) {
                delay = new DelayDTO(httpError.getDelay());
            }
            dropConnection = httpError.getDropConnection();
            responseBytes = httpError.getResponseBytes();
        }
    }

    public HttpErrorDTO() {
    }

    public HttpError buildObject() {
        return new HttpError()
            .withDelay((delay != null ? delay.buildObject() : null))
            .withDropConnection(dropConnection)
            .withResponseBytes(responseBytes);
    }

    public DelayDTO getDelay() {
        return delay;
    }

    public HttpErrorDTO setDelay(DelayDTO host) {
        this.delay = host;
        return this;
    }

    public Boolean getDropConnection() {
        return dropConnection;
    }

    public HttpErrorDTO setDropConnection(Boolean port) {
        this.dropConnection = port;
        return this;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }

    public HttpErrorDTO setResponseBytes(byte[] scheme) {
        this.responseBytes = scheme;
        return this;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final HttpErrorDTO that))
        {
            return false;
        }
		return Objects.equals(getDelay(), that.getDelay()) && Objects.equals(
            getDropConnection(),
            that.getDropConnection()) && Objects.deepEquals(getResponseBytes(), that.getResponseBytes());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(getDelay(), getDropConnection(), Arrays.hashCode(getResponseBytes()));
    }
}

