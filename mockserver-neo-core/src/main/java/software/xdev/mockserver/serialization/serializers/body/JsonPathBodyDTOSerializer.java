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
import software.xdev.mockserver.serialization.model.JsonPathBodyDTO;

import java.io.IOException;

public class JsonPathBodyDTOSerializer extends StdSerializer<JsonPathBodyDTO> {

    public JsonPathBodyDTOSerializer() {
        super(JsonPathBodyDTO.class);
    }

    @Override
    public void serialize(JsonPathBodyDTO jsonPathBodyDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (jsonPathBodyDTO.getNot() != null && jsonPathBodyDTO.getNot()) {
            jgen.writeBooleanField("not", jsonPathBodyDTO.getNot());
        }
        if (jsonPathBodyDTO.getOptional() != null && jsonPathBodyDTO.getOptional()) {
            jgen.writeBooleanField("optional", jsonPathBodyDTO.getOptional());
        }
        jgen.writeStringField("type", jsonPathBodyDTO.getType().name());
        jgen.writeStringField("jsonPath", jsonPathBodyDTO.getJsonPath());
        jgen.writeEndObject();
    }
}
