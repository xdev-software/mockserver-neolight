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
package software.xdev.mockserver.serialization.serializers.request;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.serialization.model.OpenAPIDefinitionDTO;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class OpenAPIDefinitionDTOSerializer extends StdSerializer<OpenAPIDefinitionDTO> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();

    public OpenAPIDefinitionDTOSerializer() {
        super(OpenAPIDefinitionDTO.class);
    }

    @Override
    public void serialize(OpenAPIDefinitionDTO openAPIDefinition, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (openAPIDefinition.getNot() != null && openAPIDefinition.getNot()) {
            jgen.writeBooleanField("not", openAPIDefinition.getNot());
        }
        if (isNotBlank(openAPIDefinition.getOperationId())) {
            jgen.writeObjectField("operationId", openAPIDefinition.getOperationId());
        }
        if (isNotBlank(openAPIDefinition.getSpecUrlOrPayload())) {
            if (openAPIDefinition.getSpecUrlOrPayload().trim().startsWith("{")) {
                jgen.writeObjectField("specUrlOrPayload", OBJECT_MAPPER.readTree(openAPIDefinition.getSpecUrlOrPayload()));
            } else {
                jgen.writeObjectField("specUrlOrPayload", openAPIDefinition.getSpecUrlOrPayload());
            }
        }
        jgen.writeEndObject();
    }
}
