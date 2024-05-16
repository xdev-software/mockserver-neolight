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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.matchers.MatchType;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.serialization.model.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.serialization.ObjectMapperFactory.buildObjectMapperWithoutRemovingEmptyValues;
import static org.slf4j.event.Level.DEBUG;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BodyDTODeserializer extends StdDeserializer<BodyDTO> {
    
    private static final Logger LOG = LoggerFactory.getLogger(BodyDTODeserializer.class);
    
    private static final Map<String, Body.Type> fieldNameToType = new HashMap<>();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private static ObjectWriter objectWriter;
    private static ObjectMapper objectMapper;
    private static ObjectWriter jsonBodyObjectWriter;

    static {
        fieldNameToType.put("base64Bytes".toLowerCase(), Body.Type.BINARY);
        fieldNameToType.put("parameters".toLowerCase(), Body.Type.PARAMETERS);
        fieldNameToType.put("regex".toLowerCase(), Body.Type.REGEX);
        fieldNameToType.put("string".toLowerCase(), Body.Type.STRING);
    }

    public BodyDTODeserializer() {
        super(BodyDTO.class);
    }

    @Override
    public BodyDTO deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        BodyDTO result = null;
        JsonToken currentToken = jsonParser.getCurrentToken();
        String valueJsonValue = "";
        byte[] rawBytes = null;
        Body.Type type = null;
        Boolean not = null;
        Boolean optional = null;
        MediaType contentType = null;
        Charset charset = null;
        boolean subString = false;
        Parameters parameters = null;
        Map<String, ParameterStyle> parameterStyles = null;
        Map<String, String> namespacePrefixes = null;
        if (currentToken == JsonToken.START_OBJECT) {
            @SuppressWarnings("unchecked") Map<Object, Object> body = (Map<Object, Object>) ctxt.readValue(jsonParser, Map.class);
            for (Map.Entry<Object, Object> entry : body.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String key = (String) entry.getKey();
                    if (key.equalsIgnoreCase("type")) {
                        try {
                            type = Body.Type.valueOf(String.valueOf(entry.getValue()));
                        } catch (IllegalArgumentException iae) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Ignoring invalid value for \"type\" field of \"{}\"",  entry.getValue());
                            }
                        }
                    }
                    if (containsIgnoreCase(key, "string", "regex", "base64Bytes") && type != Body.Type.PARAMETERS) {
                        String fieldName = String.valueOf(entry.getKey()).toLowerCase();
                        if (fieldNameToType.containsKey(fieldName)) {
                            type = fieldNameToType.get(fieldName);
                        }
                        if (Map.class.isAssignableFrom(entry.getValue().getClass()) ||
                            containsIgnoreCase(key, "json", "jsonSchema") && !String.class.isAssignableFrom(entry.getValue().getClass())) {
                            if (jsonBodyObjectWriter == null) {
                                jsonBodyObjectWriter = buildObjectMapperWithoutRemovingEmptyValues().writerWithDefaultPrettyPrinter();
                            }
                            valueJsonValue = jsonBodyObjectWriter.writeValueAsString(entry.getValue());
                        } else {
                            valueJsonValue = String.valueOf(entry.getValue());
                        }
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
                    if (key.equalsIgnoreCase("subString")) {
                        try {
                            subString = Boolean.parseBoolean(String.valueOf(entry.getValue()));
                        } catch (IllegalArgumentException uce) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Ignoring unsupported boolean with value \"{}\"",
                                    entry.getValue(), uce);
                            }
                        }
                    }
                    if (key.equalsIgnoreCase("parameterStyles") && entry.getValue() instanceof Map) {
                        try {
                            parameterStyles = new HashMap<>();
                            for (Map.Entry<?, ?> parameterStyle : ((Map<?, ?>) entry.getValue()).entrySet()) {
                                parameterStyles.put(String.valueOf(parameterStyle.getKey()), ParameterStyle.valueOf(String.valueOf(parameterStyle.getValue())));
                            }
                        } catch (IllegalArgumentException uce) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Ignoring unsupported boolean with value \"{}\"",
                                    entry.getValue(), uce);
                            }
                        }
                    }
                    if (key.equalsIgnoreCase("namespacePrefixes") && entry.getValue() instanceof Map) {
                      try {
                          namespacePrefixes = new HashMap<>();
                          for (Map.Entry<?, ?> namespacePrefixEntry : ((Map<?, ?>) entry.getValue()).entrySet()) {
                              namespacePrefixes.put(String.valueOf(namespacePrefixEntry.getKey()), String.valueOf(namespacePrefixEntry.getValue()));
                          }
                      } catch (IllegalArgumentException uce) {
                          if (LOG.isDebugEnabled()) {
                              LOG.debug("Ignoring unsupported namespacePrefixEntry with value \"{}\"",
                                  entry.getValue(), uce);
                          }
                      }
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
                    if (key.equalsIgnoreCase("parameters")) {
                        if (objectMapper == null) {
                            objectMapper = ObjectMapperFactory.createObjectMapper();
                        }
                        if (objectWriter == null) {
                            objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
                        }
                        parameters = objectMapper.readValue(objectWriter.writeValueAsString(entry.getValue()), Parameters.class);
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
                    case PARAMETERS:
                        result = new ParameterBodyDTO(new ParameterBody(parameters), not);
                        break;
                    case REGEX:
                        result = new RegexBodyDTO(new RegexBody(valueJsonValue), not);
                        break;
                    case STRING:
                        if (contentType != null && isNotBlank(contentType.toString())) {
                            result = new StringBodyDTO(new StringBody(valueJsonValue, rawBytes, subString, contentType), not);
                            break;
                        } else if (charset != null) {
                            result = new StringBodyDTO(new StringBody(valueJsonValue, rawBytes, subString, StringBody.DEFAULT_CONTENT_TYPE.withCharset(charset)), not);
                            break;
                        } else {
                            result = new StringBodyDTO(new StringBody(valueJsonValue, rawBytes, subString, null), not);
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
