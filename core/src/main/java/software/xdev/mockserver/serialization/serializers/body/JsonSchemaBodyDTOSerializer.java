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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.serialization.model.JsonSchemaBodyDTO;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JsonSchemaBodyDTOSerializer extends StdSerializer<JsonSchemaBodyDTO> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();

    public JsonSchemaBodyDTOSerializer() {
        super(JsonSchemaBodyDTO.class);
    }

    @Override
    public void serialize(JsonSchemaBodyDTO jsonSchemaBodyDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (jsonSchemaBodyDTO.getNot() != null && jsonSchemaBodyDTO.getNot()) {
            jgen.writeBooleanField("not", jsonSchemaBodyDTO.getNot());
        }
        if (jsonSchemaBodyDTO.getOptional() != null && jsonSchemaBodyDTO.getOptional()) {
            jgen.writeBooleanField("optional", jsonSchemaBodyDTO.getOptional());
        }
        jgen.writeStringField("type", jsonSchemaBodyDTO.getType().name());
        jgen.writeObjectField("jsonSchema", OBJECT_MAPPER.readTree(jsonSchemaBodyDTO.getJson()));
        if (jsonSchemaBodyDTO.getParameterStyles() != null) {
            jgen.writeObjectField("parameterStyles", jsonSchemaBodyDTO.getParameterStyles());
        }
        jgen.writeEndObject();
    }
}
