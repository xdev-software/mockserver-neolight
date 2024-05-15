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

import java.util.Arrays;
import java.util.Objects;

public class HttpError extends Action<HttpError> {
    private int hashCode;
    private Boolean dropConnection;
    private byte[] responseBytes;

    public static HttpError error() {
        return new HttpError();
    }

    /**
     * Forces the connection to be dropped without any response being returned
     *
     * @param dropConnection if true the connection is drop without any response being returned
     */
    public HttpError withDropConnection(Boolean dropConnection) {
        this.dropConnection = dropConnection;
        this.hashCode = 0;
        return this;
    }

    public Boolean getDropConnection() {
        return dropConnection;
    }

    /**
     * The raw response to be returned, allowing the expectation to specify any invalid response as a raw byte[]
     *
     * @param responseBytes the exact bytes that will be returned
     */
    public HttpError withResponseBytes(byte[] responseBytes) {
        this.responseBytes = responseBytes;
        this.hashCode = 0;
        return this;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }

    @Override
    @JsonIgnore
    public Type getType() {
        return Type.ERROR;
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
        HttpError httpError = (HttpError) o;
        return Objects.equals(dropConnection, httpError.dropConnection) &&
            Arrays.equals(responseBytes, httpError.responseBytes);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = Objects.hash(super.hashCode(), dropConnection);
            hashCode = 31 * result + Arrays.hashCode(responseBytes);
        }
        return hashCode;
    }
}

