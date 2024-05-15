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
import software.xdev.mockserver.model.StringBody;

import java.io.IOException;

public class StringBodySerializer extends StdSerializer<StringBody> {

    private final boolean serialiseDefaultValues;

    public StringBodySerializer(boolean serialiseDefaultValues) {
        super(StringBody.class);
        this.serialiseDefaultValues = serialiseDefaultValues;
    }

    @Override
    public void serialize(StringBody stringBody, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        boolean notFieldSetAndNotDefault = stringBody.getNot() != null && stringBody.getNot();
        boolean optionalFieldSetAndNotDefault = stringBody.getOptional() != null && stringBody.getOptional();
        boolean subStringFieldNotDefault = stringBody.isSubString();
        boolean contentTypeFieldSet = stringBody.getContentType() != null;
        if (serialiseDefaultValues || notFieldSetAndNotDefault || optionalFieldSetAndNotDefault || contentTypeFieldSet || subStringFieldNotDefault) {
            jgen.writeStartObject();
            if (notFieldSetAndNotDefault) {
                jgen.writeBooleanField("not", true);
            }
            if (optionalFieldSetAndNotDefault) {
                jgen.writeBooleanField("optional", true);
            }
            jgen.writeStringField("type", stringBody.getType().name());
            jgen.writeStringField("string", stringBody.getValue());
            if (subStringFieldNotDefault) {
                jgen.writeBooleanField("subString", true);
            }
            if (contentTypeFieldSet) {
                jgen.writeStringField("contentType", stringBody.getContentType());
            }
            jgen.writeEndObject();
        } else {
            jgen.writeString(stringBody.getValue());
        }
    }
}
