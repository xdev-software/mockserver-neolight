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
import software.xdev.mockserver.model.JsonSchemaBody;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JsonSchemaBodySerializer extends StdSerializer<JsonSchemaBody> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();

    public JsonSchemaBodySerializer() {
        super(JsonSchemaBody.class);
    }

    @Override
    public void serialize(JsonSchemaBody jsonSchemaBody, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (jsonSchemaBody.getNot() != null && jsonSchemaBody.getNot()) {
            jgen.writeBooleanField("not", jsonSchemaBody.getNot());
        }
        if (jsonSchemaBody.getOptional() != null && jsonSchemaBody.getOptional()) {
            jgen.writeBooleanField("optional", jsonSchemaBody.getOptional());
        }
        jgen.writeStringField("type", jsonSchemaBody.getType().name());
        jgen.writeObjectField("jsonSchema", OBJECT_MAPPER.readTree(jsonSchemaBody.getValue()));
        if (jsonSchemaBody.getParameterStyles() != null) {
            jgen.writeObjectField("parameterStyles", jsonSchemaBody.getParameterStyles());
        }
        jgen.writeEndObject();
    }
}
