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
import software.xdev.mockserver.serialization.model.XmlBodyDTO;
import software.xdev.mockserver.model.XmlBody;

import java.io.IOException;

public class XmlBodyDTOSerializer extends StdSerializer<XmlBodyDTO> {

    public XmlBodyDTOSerializer() {
        super(XmlBodyDTO.class);
    }

    @Override
    public void serialize(XmlBodyDTO xmlBodyDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (xmlBodyDTO.getNot() != null && xmlBodyDTO.getNot()) {
            jgen.writeBooleanField("not", true);
        }
        if (xmlBodyDTO.getOptional() != null && xmlBodyDTO.getOptional()) {
            jgen.writeBooleanField("optional", xmlBodyDTO.getOptional());
        }
        jgen.writeStringField("type", xmlBodyDTO.getType().name());
        jgen.writeStringField("xml", xmlBodyDTO.getXml());
        if (xmlBodyDTO.getRawBytes() != null) {
            jgen.writeObjectField("rawBytes", xmlBodyDTO.getRawBytes());
        }
        if (xmlBodyDTO.getContentType() != null && !xmlBodyDTO.getContentType().equals(XmlBody.DEFAULT_XML_CONTENT_TYPE.toString())) {
            jgen.writeStringField("contentType", xmlBodyDTO.getContentType());
        }
        jgen.writeEndObject();
    }
}
