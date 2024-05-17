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
import software.xdev.mockserver.serialization.Base64Converter;

import java.util.Arrays;
import java.util.Objects;

public class BinaryBody extends BodyWithContentType<byte[]> {
    private int hashCode;
    private final byte[] bytes;

    public BinaryBody(byte[] bytes) {
        this(bytes, null);
    }

    public BinaryBody(byte[] bytes, MediaType contentType) {
        super(Type.BINARY, contentType);
        this.bytes = bytes;
    }

    public static BinaryBody binary(byte[] body) {
        return new BinaryBody(body);
    }

    public static BinaryBody binary(byte[] body, MediaType contentType) {
        return new BinaryBody(body, contentType);
    }

    public byte[] getValue() {
        return bytes;
    }

    @JsonIgnore
    public byte[] getRawBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return bytes != null ? Base64Converter.bytesToBase64String(bytes) : null;
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
        BinaryBody that = (BinaryBody) o;
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = Objects.hash(super.hashCode());
            hashCode = 31 * result + Arrays.hashCode(bytes);
        }
        return hashCode;
    }
}
