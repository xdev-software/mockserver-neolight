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
package software.xdev.mockserver.serialization.serializers.expectation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.mock.OpenAPIExpectation;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class OpenAPIExpectationSerializer extends StdSerializer<OpenAPIExpectation> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();

    public OpenAPIExpectationSerializer() {
        super(OpenAPIExpectation.class);
    }

    @Override
    public void serialize(OpenAPIExpectation openAPIDefinition, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (isNotBlank(openAPIDefinition.getSpecUrlOrPayload())) {
            if (openAPIDefinition.getSpecUrlOrPayload().trim().startsWith("{")) {
                jgen.writeObjectField("specUrlOrPayload", OBJECT_MAPPER.readTree(openAPIDefinition.getSpecUrlOrPayload()));
            } else {
                jgen.writeObjectField("specUrlOrPayload", openAPIDefinition.getSpecUrlOrPayload());
            }
        }
        if (openAPIDefinition.getOperationsAndResponses() != null && !openAPIDefinition.getOperationsAndResponses().isEmpty()) {
            jgen.writeObjectField("operationsAndResponses", openAPIDefinition.getOperationsAndResponses());
        }
        jgen.writeEndObject();
    }
}
