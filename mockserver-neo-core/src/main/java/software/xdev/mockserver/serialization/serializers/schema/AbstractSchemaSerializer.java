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
package software.xdev.mockserver.serialization.serializers.schema;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.models.media.Schema;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
public class AbstractSchemaSerializer<T extends Schema> extends StdSerializer<T> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.buildObjectMapperWithOnlyConfigurationDefaults();
    private static final List<String> fieldsToRemove = ImmutableList.of(
        "exampleSetFlag",
        "types"
    );

    public AbstractSchemaSerializer(Class<T> type) {
        super(type);
    }

    @Override
    public void serialize(T schema, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        ObjectNode jsonNodes = OBJECT_MAPPER.convertValue(schema, ObjectNode.class);
        recurse(jsonNodes, node -> {
            if (node instanceof ObjectNode) {
                ((ObjectNode) node).remove(fieldsToRemove);
            }
        });
        jgen.writeObject(jsonNodes);
    }

    private void recurse(JsonNode node, Consumer<JsonNode> jsonNodeCallable) {
        jsonNodeCallable.accept(node);
        for (JsonNode jsonNode : node) {
            recurse(jsonNode, jsonNodeCallable);
        }
    }
}
