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
import software.xdev.mockserver.serialization.model.StringBodyDTO;

import java.io.IOException;

public class StringBodyDTOSerializer extends StdSerializer<StringBodyDTO> {

    private final boolean serialiseDefaultValues;

    public StringBodyDTOSerializer(boolean serialiseDefaultValues) {
        super(StringBodyDTO.class);
        this.serialiseDefaultValues = serialiseDefaultValues;
    }

    @Override
    public void serialize(StringBodyDTO stringBodyDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        boolean notFieldSetAndNotDefault = stringBodyDTO.getNot() != null && stringBodyDTO.getNot();
        boolean optionalFieldSetAndNotDefault = stringBodyDTO.getOptional() != null && stringBodyDTO.getOptional();
        boolean subStringFieldNotDefault = stringBodyDTO.isSubString();
        boolean contentTypeFieldSet = stringBodyDTO.getContentType() != null;
        if (serialiseDefaultValues || notFieldSetAndNotDefault || optionalFieldSetAndNotDefault || contentTypeFieldSet || subStringFieldNotDefault) {
            jgen.writeStartObject();
            if (notFieldSetAndNotDefault) {
                jgen.writeBooleanField("not", true);
            }
            if (optionalFieldSetAndNotDefault) {
                jgen.writeBooleanField("optional", true);
            }
            jgen.writeStringField("type", stringBodyDTO.getType().name());
            jgen.writeStringField("string", stringBodyDTO.getString());
            if (stringBodyDTO.getRawBytes() != null) {
                jgen.writeObjectField("rawBytes", stringBodyDTO.getRawBytes());
            }
            if (subStringFieldNotDefault) {
                jgen.writeBooleanField("subString", true);
            }
            if (contentTypeFieldSet) {
                jgen.writeStringField("contentType", stringBodyDTO.getContentType());
            }
            jgen.writeEndObject();
        } else {
            jgen.writeString(stringBodyDTO.getString());
        }
    }
}
