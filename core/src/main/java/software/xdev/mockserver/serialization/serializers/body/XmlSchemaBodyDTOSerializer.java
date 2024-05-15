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
import software.xdev.mockserver.serialization.model.XmlSchemaBodyDTO;

import java.io.IOException;

public class XmlSchemaBodyDTOSerializer extends StdSerializer<XmlSchemaBodyDTO> {

    public XmlSchemaBodyDTOSerializer() {
        super(XmlSchemaBodyDTO.class);
    }

    @Override
    public void serialize(XmlSchemaBodyDTO xmlSchemaBodyDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (xmlSchemaBodyDTO.getNot() != null && xmlSchemaBodyDTO.getNot()) {
            jgen.writeBooleanField("not", xmlSchemaBodyDTO.getNot());
        }
        if (xmlSchemaBodyDTO.getOptional() != null && xmlSchemaBodyDTO.getOptional()) {
            jgen.writeBooleanField("optional", xmlSchemaBodyDTO.getOptional());
        }
        jgen.writeStringField("type", xmlSchemaBodyDTO.getType().name());
        jgen.writeStringField("xmlSchema", xmlSchemaBodyDTO.getXml());
        jgen.writeEndObject();
    }
}
