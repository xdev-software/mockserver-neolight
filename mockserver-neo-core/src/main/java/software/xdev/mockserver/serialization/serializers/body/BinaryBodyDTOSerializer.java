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
package software.xdev.mockserver.serialization.serializers.body;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.serialization.model.BinaryBodyDTO;

import java.io.IOException;

public class BinaryBodyDTOSerializer extends StdSerializer<BinaryBodyDTO> {

    public BinaryBodyDTOSerializer() {
        super(BinaryBodyDTO.class);
    }

    @Override
    public void serialize(BinaryBodyDTO binaryBodyDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (binaryBodyDTO.getNot() != null && binaryBodyDTO.getNot()) {
            jgen.writeBooleanField("not", binaryBodyDTO.getNot());
        }
        if (binaryBodyDTO.getOptional() != null && binaryBodyDTO.getOptional()) {
            jgen.writeBooleanField("optional", binaryBodyDTO.getOptional());
        }
        jgen.writeStringField("type", binaryBodyDTO.getType().name());
        jgen.writeObjectField("base64Bytes", binaryBodyDTO.getBase64Bytes());
        if (binaryBodyDTO.getContentType() != null) {
            jgen.writeStringField("contentType", binaryBodyDTO.getContentType());
        }
        jgen.writeEndObject();
    }
}
