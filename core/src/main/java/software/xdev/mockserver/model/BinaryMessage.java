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

import java.time.LocalDateTime;

public class BinaryMessage implements Message {

    private byte[] bytes;
    private LocalDateTime timestamp;

    public static BinaryMessage bytes(byte[] bytes) {
        return new BinaryMessage()
            .withBytes(bytes)
            .withTimestamp(LocalDateTime.now());
    }

    public BinaryMessage withBytes(byte[] bytes) {
        this.bytes = bytes;
        return this;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public BinaryMessage withTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
