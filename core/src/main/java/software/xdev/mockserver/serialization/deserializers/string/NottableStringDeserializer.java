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
package software.xdev.mockserver.serialization.deserializers.string;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.ParameterStyle;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.model.NottableOptionalString.optional;
import static software.xdev.mockserver.model.NottableString.string;

public class NottableStringDeserializer extends StdDeserializer<NottableString> {

    public NottableStringDeserializer() {
        super(NottableString.class);
    }

    @Override
    public NottableString deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT) {
            Boolean not = null;
            Boolean optional = null;
            String value = null;
            ParameterStyle parameterStyle = null;

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jsonParser.getCurrentName();
                if ("not".equals(fieldName)) {
                    jsonParser.nextToken();
                    not = jsonParser.getBooleanValue();
                } else if ("optional".equals(fieldName)) {
                    jsonParser.nextToken();
                    optional = jsonParser.getBooleanValue();
                } else if ("value".equals(fieldName)) {
                    jsonParser.nextToken();
                    value = ctxt.readValue(jsonParser, String.class);
                } else if ("parameterStyle".equals(fieldName)) {
                    jsonParser.nextToken();
                    parameterStyle = ctxt.readValue(jsonParser, ParameterStyle.class);
                }
            }

            NottableString result = null;
            if (Boolean.TRUE.equals(optional)) {
                result = optional(value, not);
            } else if (isNotBlank(value)) {
                result = string(value, not);
            }

            if (result != null && parameterStyle != null) {
                result.withStyle(parameterStyle);
            }

            return result;
        } else if (jsonParser.getCurrentToken() == JsonToken.VALUE_STRING || jsonParser.getCurrentToken() == JsonToken.FIELD_NAME) {
            return string(ctxt.readValue(jsonParser, String.class));
        }
        return null;
    }

}
