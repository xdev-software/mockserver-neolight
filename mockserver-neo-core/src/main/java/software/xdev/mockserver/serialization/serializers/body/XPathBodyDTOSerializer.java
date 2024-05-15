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
import software.xdev.mockserver.serialization.model.XPathBodyDTO;

import java.io.IOException;
import java.util.Map;

public class XPathBodyDTOSerializer extends StdSerializer<XPathBodyDTO> {

    public XPathBodyDTOSerializer() {
        super(XPathBodyDTO.class);
    }

    @Override
    public void serialize(XPathBodyDTO xPathBodyDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (xPathBodyDTO.getNot() != null && xPathBodyDTO.getNot()) {
            jgen.writeBooleanField("not", xPathBodyDTO.getNot());
        }
        if (xPathBodyDTO.getOptional() != null && xPathBodyDTO.getOptional()) {
            jgen.writeBooleanField("optional", xPathBodyDTO.getOptional());
        }
        jgen.writeStringField("type", xPathBodyDTO.getType().name());
        jgen.writeStringField("xpath", xPathBodyDTO.getXPath());

        if (xPathBodyDTO.getNamespacePrefixes() != null) {
          jgen.writeObjectFieldStart("namespacePrefixes");
          for (Map.Entry<String, String> entry : xPathBodyDTO.getNamespacePrefixes().entrySet()) {
            jgen.writeStringField(entry.getKey(), entry.getValue());
          }
          jgen.writeEndObject();
        }
        jgen.writeEndObject();
    }
}
