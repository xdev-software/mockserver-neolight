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
package software.xdev.mockserver.serialization.deserializers.collections;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import software.xdev.mockserver.model.KeyMatchStyle;
import software.xdev.mockserver.model.KeysToMultiValues;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.ParameterStyle;

import java.io.IOException;

import static software.xdev.mockserver.model.NottableString.string;

public abstract class KeysToMultiValuesDeserializer<T extends KeysToMultiValues<?, ?>> extends StdDeserializer<T> {

    KeysToMultiValuesDeserializer(Class<T> valueClass) {
        super(valueClass);
    }

    public abstract T build();

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.isExpectedStartArrayToken()) {
            return deserializeArray(p, ctxt);
        } else if (p.isExpectedStartObjectToken()) {
            return deserializeObject(p, ctxt);
        } else {
            return null;
        }
    }

    private T deserializeObject(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        T entries = build();
        NottableString key = string("");
        while (true) {
            JsonToken token = jsonParser.nextToken();
            switch (token) {
                case FIELD_NAME:
                    key = string(jsonParser.getText());
                    if ("keyMatchStyle".equals(key.getValue())) {
                        jsonParser.nextToken();
                        entries.withKeyMatchStyle(ctxt.readValue(jsonParser, KeyMatchStyle.class));
                    }
                    break;
                case START_OBJECT:
                    // parse parameterStyle and value
                    jsonParser.nextToken();
                    ParameterStyle parameterStyle = ParameterStyle.FORM_EXPLODED;
                    NottableString[] values = null;
                    while (token != JsonToken.END_OBJECT) {
                        String fieldName = jsonParser.getCurrentName();
                        if ("values".equals(fieldName)) {
                            jsonParser.nextToken();
                            values = ctxt.readValue(jsonParser, NottableString[].class);
                        } else if ("parameterStyle".equals(fieldName)) {
                            jsonParser.nextToken();
                            parameterStyle = ctxt.readValue(jsonParser, ParameterStyle.class);
                        }
                        token = jsonParser.nextToken();
                    }
                    entries.withEntry(key.withStyle(parameterStyle), values);
                    break;
                case START_ARRAY:
                    entries.withEntry(key, ctxt.readValue(jsonParser, NottableString[].class));
                    break;
                case VALUE_STRING:
                    entries.withEntry(key, ctxt.readValue(jsonParser, NottableString.class));
                    break;
                case END_OBJECT:
                    return entries;
                default:
                    throw new RuntimeException("Unexpected token: \"" + token + "\" id: \"" + token.id() + "\" text: \"" + jsonParser.getText());
            }
        }
    }

    private T deserializeArray(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        T entries = build();
        NottableString key = string("");
        NottableString[] values = null;
        while (true) {
            JsonToken token = jsonParser.nextToken();
            switch (token) {
                case START_ARRAY:
                    values = ctxt.readValue(jsonParser, NottableString[].class);
                    break;
                case END_ARRAY:
                    return entries;
                case START_OBJECT:
                    if (key != null) {
                        key = null;
                    } else {
                        key = ctxt.readValue(jsonParser, NottableString.class);
                    }
                    values = null;
                    break;
                case END_OBJECT:
                    entries.withEntry(key, values);
                    break;
                case FIELD_NAME:
                    break;
                case VALUE_STRING:
                    key = ctxt.readValue(jsonParser, NottableString.class);
                    break;
                default:
                    throw new RuntimeException("Unexpected token: \"" + token + "\" id: \"" + token.id() + "\" text: \"" + jsonParser.getText());
            }
        }
    }
}
