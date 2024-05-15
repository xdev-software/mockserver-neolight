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
package software.xdev.mockserver.serialization.serializers.string;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.model.NottableSchemaString;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static software.xdev.mockserver.model.NottableString.serialiseNottableString;

public class NottableStringSerializer extends StdSerializer<NottableString> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();

    public NottableStringSerializer() {
        super(NottableString.class);
    }

    @Override
    public void serialize(NottableString nottableString, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (nottableString instanceof NottableSchemaString) {
            writeObject(nottableString, jgen, "schema", OBJECT_MAPPER.readTree(nottableString.getValue()));
        } else if (nottableString.getParameterStyle() != null) {
            writeObject(nottableString, jgen, "value", nottableString.getValue());
        } else {
            jgen.writeString(serialiseNottableString(nottableString));
        }
    }

    private void writeObject(NottableString nottableString, JsonGenerator jgen, String valueFieldName, Object value) throws IOException {
        jgen.writeStartObject();
        if (Boolean.TRUE.equals(nottableString.isNot())) {
            jgen.writeBooleanField("not", true);
        }
        if (Boolean.TRUE.equals(nottableString.isOptional())) {
            jgen.writeBooleanField("optional", true);
        }
        if (nottableString.getParameterStyle() != null) {
            jgen.writeObjectField("parameterStyle", nottableString.getParameterStyle());
        }
        jgen.writeObjectField(valueFieldName, value);
        jgen.writeEndObject();
    }

}
