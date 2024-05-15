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
package software.xdev.mockserver.serialization;

import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;

import java.nio.charset.Charset;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Base64Converter extends ObjectWithReflectiveEqualsHashCodeToString {

    private static final String BASE64_PATTERN = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$";
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public byte[] base64StringToBytes(String data) {
        if (data == null) {
            return new byte[0];
        }
        if (!data.matches(BASE64_PATTERN)) {
            return data.getBytes(UTF_8);
        }
        return DECODER.decode(data.getBytes(UTF_8));
    }

    public String bytesToBase64String(byte[] data) {
        return bytesToBase64String(data, UTF_8);
    }

    public String bytesToBase64String(byte[] data, Charset charset) {
        return new String(ENCODER.encode(data), charset);
    }

}
