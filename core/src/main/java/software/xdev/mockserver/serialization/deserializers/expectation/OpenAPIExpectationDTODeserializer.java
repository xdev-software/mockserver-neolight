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
package software.xdev.mockserver.serialization.deserializers.expectation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import software.xdev.mockserver.serialization.model.OpenAPIExpectationDTO;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OpenAPIExpectationDTODeserializer extends StdDeserializer<OpenAPIExpectationDTO> {

    public OpenAPIExpectationDTODeserializer() {
        super(OpenAPIExpectationDTO.class);
    }

    @Override
    public OpenAPIExpectationDTO deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT) {
            String specUrlOrPayload = null;
            Map<String, String> operationsAndResponses = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jsonParser.getCurrentName();
                switch (fieldName) {
                    case "specUrlOrPayload":
                        jsonParser.nextToken();
                        JsonNode specUrlOrPayloadField = ctxt.readValue(jsonParser, JsonNode.class);
                        if (specUrlOrPayloadField.isTextual()) {
                            specUrlOrPayload = specUrlOrPayloadField.asText();
                        } else {
                            specUrlOrPayload = specUrlOrPayloadField.toPrettyString();
                        }
                        break;
                    case "operationsAndResponses":
                        jsonParser.nextToken();
                        Map<String, String> value = new HashMap<>();
                        Map<?, ?> map = ctxt.readValue(jsonParser, Map.class);
                        map.keySet().forEach(key -> {
                            if (key instanceof String && map.get(key) instanceof String) {
                                value.put((String) key, (String) map.get(key));
                            }
                        });
                        if (!value.isEmpty()) {
                            operationsAndResponses = value;
                        }
                        break;
                }
            }
            return new OpenAPIExpectationDTO()
                .setSpecUrlOrPayload(specUrlOrPayload)
                .setOperationsAndResponses(operationsAndResponses);
        }
        return null;
    }
}
