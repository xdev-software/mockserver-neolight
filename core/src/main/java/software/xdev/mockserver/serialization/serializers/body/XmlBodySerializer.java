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
import software.xdev.mockserver.model.XmlBody;

import java.io.IOException;

public class XmlBodySerializer extends StdSerializer<XmlBody> {

    public XmlBodySerializer() {
        super(XmlBody.class);
    }

    @Override
    public void serialize(XmlBody xmlBody, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (xmlBody.getNot() != null && xmlBody.getNot()) {
            jgen.writeBooleanField("not", xmlBody.getNot());
        }
        if (xmlBody.getOptional() != null && xmlBody.getOptional()) {
            jgen.writeBooleanField("optional", xmlBody.getOptional());
        }
        if (xmlBody.getContentType() != null && !xmlBody.getContentType().equals(XmlBody.DEFAULT_XML_CONTENT_TYPE.toString())) {
            jgen.writeStringField("contentType", xmlBody.getContentType());
        }
        jgen.writeStringField("type", xmlBody.getType().name());
        jgen.writeStringField("xml", xmlBody.getValue());
        jgen.writeEndObject();
    }
}
