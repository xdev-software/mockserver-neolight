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
package software.xdev.mockserver.serialization.deserializers.body;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.serialization.model.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.serialization.ObjectMapperFactory.buildObjectMapperWithoutRemovingEmptyValues;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.TRACE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BodyWithContentTypeDTODeserializer extends StdDeserializer<BodyWithContentTypeDTO> {
    
    private static final Logger LOG = LoggerFactory.getLogger(BodyWithContentTypeDTODeserializer.class);
    
    private static final Map<String, Body.Type> fieldNameToType = new HashMap<>();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    
    static {
        fieldNameToType.put("base64Bytes".toLowerCase(), Body.Type.BINARY);
        fieldNameToType.put("string".toLowerCase(), Body.Type.STRING);
    }

    public BodyWithContentTypeDTODeserializer() {
        super(BodyWithContentTypeDTO.class);
    }

    @Override
    public BodyWithContentTypeDTO deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        BodyWithContentTypeDTO result = null;
        JsonToken currentToken = jsonParser.getCurrentToken();
        String valueJsonValue = "";
        byte[] rawBytes = null;
        Body.Type type = null;
        Boolean not = null;
        Boolean optional = null;
        MediaType contentType = null;
        Charset charset = null;
        if (currentToken == JsonToken.START_OBJECT) {
            @SuppressWarnings("unchecked") Map<Object, Object> body = (Map<Object, Object>) ctxt.readValue(jsonParser, Map.class);
            for (Map.Entry<Object, Object> entry : body.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    if (key.equalsIgnoreCase("type")) {
                        try {
                            type = Body.Type.valueOf(String.valueOf(entry.getValue()));
                        } catch (IllegalArgumentException iae) {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Ignoring invalid value for \"type\" field of \"{}\"",
                                    entry.getValue(), iae);
                            }
                        }
                    }
                    if (containsIgnoreCase(key, "string", "regex", "base64Bytes") && type != Body.Type.PARAMETERS) {
                        String fieldName = String.valueOf(entry.getKey()).toLowerCase();
                        if (fieldNameToType.containsKey(fieldName)) {
                            type = fieldNameToType.get(fieldName);
                        }
                        valueJsonValue = String.valueOf(entry.getValue());
                    }
                    if (containsIgnoreCase(key, "rawBytes", "base64Bytes")) {
                        if (entry.getValue() instanceof String s) {
                            try {
                                rawBytes = BASE64_DECODER.decode(s);
                            } catch (Exception ex) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Invalid base64 encoded rawBytes with value \"{}\"",
                                        entry.getValue(), ex);
                                }
                            }
                        }
                    }
                    if (key.equalsIgnoreCase("not")) {
                        not = Boolean.parseBoolean(String.valueOf(entry.getValue()));
                    }
                    if (key.equalsIgnoreCase("optional")) {
                        optional = Boolean.parseBoolean(String.valueOf(entry.getValue()));
                    }
                    if (key.equalsIgnoreCase("contentType")) {
                        try {
                            String mediaTypeHeader = String.valueOf(entry.getValue());
                            if (isNotBlank(mediaTypeHeader)) {
                                MediaType parsedMediaTypeHeader = MediaType.parse(mediaTypeHeader);
                                if (isNotBlank(parsedMediaTypeHeader.toString())) {
                                    contentType = parsedMediaTypeHeader;
                                }
                            }
                        } catch (IllegalArgumentException uce) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Ignoring unsupported MediaType with value \"{}\"",
                                    entry.getValue(), uce);
                            }
                        }
                    }
                    if (key.equalsIgnoreCase("charset")) {
                        try {
                            charset = Charset.forName(String.valueOf(entry.getValue()));
                        } catch (UnsupportedCharsetException uce) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Ignoring unsupported Charset with value \"{}\"",
                                    entry.getValue(), uce);
                            }
                        } catch (IllegalCharsetNameException icne) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Ignoring invalid Charset with value \"{}\"",
                                    entry.getValue(), icne);
                            }
                        }
                    }
                }
            }
            if (type != null) {
                switch (type) {
                    case BINARY:
                        if (contentType != null && isNotBlank(contentType.toString())) {
                            result = new BinaryBodyDTO(new BinaryBody(rawBytes, contentType), not);
                            break;
                        } else {
                            result = new BinaryBodyDTO(new BinaryBody(rawBytes), not);
                            break;
                        }
                    case STRING:
                        if (contentType != null && isNotBlank(contentType.toString())) {
                            result = new StringBodyDTO(new StringBody(valueJsonValue, rawBytes, false, contentType), not);
                            break;
                        } else if (charset != null) {
                            result = new StringBodyDTO(new StringBody(valueJsonValue, rawBytes, false, StringBody.DEFAULT_CONTENT_TYPE.withCharset(charset)), not);
                            break;
                        } else {
                            result = new StringBodyDTO(new StringBody(valueJsonValue, rawBytes, false, null), not);
                            break;
                        }
                }
            }
        } else if (currentToken == JsonToken.VALUE_STRING) {
            result = new StringBodyDTO(new StringBody(jsonParser.getText()));
        }
        if (result != null) {
            result.withOptional(optional);
        }
        return result;
    }

    private boolean containsIgnoreCase(String valueToMatch, String... listOfValues) {
        for (String item : listOfValues) {
            if (item.equalsIgnoreCase(valueToMatch)) {
                return true;
            }
        }
        return false;
    }
}
