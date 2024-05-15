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
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.NottableString;

import java.io.IOException;

import static software.xdev.mockserver.model.NottableString.string;

public class CookiesDeserializer extends StdDeserializer<Cookies> {

    public CookiesDeserializer() {
        super(Cookies.class);
    }

    @Override
    public Cookies deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.isExpectedStartArrayToken()) {
            return deserializeArray(p, ctxt, ctxt.getNodeFactory());
        } else if (p.isExpectedStartObjectToken()) {
            return deserializeObject(p, ctxt, ctxt.getNodeFactory());
        } else {
            return (Cookies) ctxt.handleUnexpectedToken(Cookies.class, p);
        }
    }

    private Cookies deserializeObject(JsonParser jsonParser, DeserializationContext ctxt, JsonNodeFactory nodeFactory) throws IOException {
        Cookies cookies = new Cookies();
        NottableString key = string("");
        while (true) {
            JsonToken t = jsonParser.nextToken();
            switch (t.id()) {
                case JsonTokenId.ID_FIELD_NAME:
                    key = string(jsonParser.getText());
                    break;
                case JsonTokenId.ID_STRING:
                    cookies.withEntry(key, ctxt.readValue(jsonParser, NottableString.class));
                    break;
                case JsonTokenId.ID_START_OBJECT:
                    cookies.withEntry(key, ctxt.readValue(jsonParser, NottableString.class));
                    break;
                case JsonTokenId.ID_END_OBJECT:
                    return cookies;
                default:
                    throw new RuntimeException("Unexpected token: \"" + t + "\" id: \"" + t.id() + "\" text: \"" + jsonParser.getText());
            }
        }
    }

    private Cookies deserializeArray(JsonParser jsonParser, DeserializationContext ctxt, JsonNodeFactory nodeFactory) throws IOException {
        Cookies headers = new Cookies();
        NottableString key = null;
        NottableString value = null;
        String fieldName = null;
        while (true) {
            JsonToken t = jsonParser.nextToken();
            switch (t.id()) {
                case JsonTokenId.ID_END_ARRAY:
                    return headers;
                case JsonTokenId.ID_START_OBJECT:
                    key = null;
                    value = null;
                    break;
                case JsonTokenId.ID_FIELD_NAME:
                    fieldName = jsonParser.getText();
                    break;
                case JsonTokenId.ID_STRING:
                    if ("name".equals(fieldName)) {
                        key = string(ctxt.readValue(jsonParser, String.class));
                    } else if ("value".equals(fieldName)) {
                        value = ctxt.readValue(jsonParser, NottableString.class);
                    }
                    break;
                case JsonTokenId.ID_END_OBJECT:
                    headers.withEntry(key, value);
                    break;
                default:
                    throw new RuntimeException("Unexpected token: \"" + t + "\" id: \"" + t.id() + "\" text: \"" + jsonParser.getText());
            }
        }
    }
}
