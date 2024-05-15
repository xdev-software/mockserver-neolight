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
package software.xdev.mockserver.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.validator.jsonschema.JsonSchemaValidator;
import org.slf4j.event.Level;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;

import static io.swagger.v3.parser.util.SchemaTypeUtil.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class NottableSchemaString extends NottableString {

    private final static MockServerLogger MOCK_SERVER_LOGGER = new MockServerLogger(NottableSchemaString.class);
    private final static ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();
    //    private final static JsonValidator VALIDATOR = JsonSchemaFactory.byDefault().getValidator();
    private final static DateTimeFormatter RFC3339 = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .appendLiteral('T')
        .appendPattern("HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
        .optionalEnd()
        .appendOffset("+HH:mm", "Z").toFormatter();
    private static final String TRUE = "true";
    private static final String TYPE = "type";
    private final ObjectNode schemaJsonNode;
    private final String type;
    private final String json;
    private final JsonSchemaValidator jsonSchemaValidator;

    private static JsonNode convertToJsonNode(final String value, final String type, final String format) throws IOException {
        if ("null".equalsIgnoreCase(value)) {
            return OBJECT_MAPPER.readTree("null");
        }
        if (DATE_TIME_FORMAT.equalsIgnoreCase(format)) {
            String result;
            try {
                // reformat to RFC3339 version that avoid schema validator errors
                result = LocalDateTime.parse(value, RFC3339).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            } catch (final DateTimeParseException e) {
                // not a valid RFC3339 format schema validator will throw correct error
                result = value;
            }
            return new TextNode(result);
        }
        if (STRING_TYPE.equalsIgnoreCase(type)) {
            return new TextNode(value);
        }
        if (NUMBER_TYPE.equalsIgnoreCase(type) ||
            INTEGER_TYPE.equalsIgnoreCase(type)) {
            try {
                // validate double format
                Double.parseDouble(value);
                return OBJECT_MAPPER.readTree(value);
            } catch (final NumberFormatException nfe) {
                return new TextNode(value);
            }
        }
        return OBJECT_MAPPER.readTree(value);
    }

    private ObjectNode getSchemaJsonNode(String schema) {
        try {
            ObjectNode jsonNodes = (ObjectNode) OBJECT_MAPPER.readTree(schema);
            // remove embedded not field (for nottable schema string support)
            jsonNodes.remove("not");
            return jsonNodes;
        } catch (Throwable throwable) {
            MOCK_SERVER_LOGGER.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception loading JSON Schema " + throwable.getMessage())
                    .setThrowable(throwable)
            );
            return null;
        }
    }

    public static NottableSchemaString schemaString(String value, Boolean not) {
        return new NottableSchemaString(value, not);
    }

    public static NottableSchemaString schemaString(String value) {
        return new NottableSchemaString(value);
    }

    public static NottableSchemaString notSchema(String value) {
        return new NottableSchemaString(value, Boolean.TRUE);
    }

    private NottableSchemaString(String schema, Boolean not) {
        super(schema, not);
        if (isNotBlank(schema)) {
            schemaJsonNode = getSchemaJsonNode(getValue());
            type = getNodeValue(schemaJsonNode, TYPE);
        } else {
            schemaJsonNode = null;
            type = null;
        }
        json = (Boolean.TRUE.equals(isNot()) ? NOT_CHAR : "") + schema;
        jsonSchemaValidator = new JsonSchemaValidator(MOCK_SERVER_LOGGER, this.json, this.schemaJsonNode);
    }

    private NottableSchemaString(String schema) {
        super(schema);
        if (isNotBlank(schema)) {
            schemaJsonNode = getSchemaJsonNode(getValue());
            type = getNodeValue(schemaJsonNode, TYPE);
        } else {
            schemaJsonNode = null;
            type = null;
        }
        json = (Boolean.TRUE.equals(isNot()) ? NOT_CHAR : "") + schema;
        if (schemaJsonNode != null) {
            jsonSchemaValidator = new JsonSchemaValidator(MOCK_SERVER_LOGGER, this.json, this.schemaJsonNode);
        } else {
            jsonSchemaValidator = null;
        }
    }

    public boolean matches(MockServerLogger mockServerLogger, MatchDifference context, List<NottableString> json) {
        if (isNotBlank(type) && type.equalsIgnoreCase("array")) {
            try {
                return matches(mockServerLogger, context, OBJECT_MAPPER.writeValueAsString(json));
            } catch (Throwable throwable) {
                MOCK_SERVER_LOGGER.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception validating JSON")
                        .setThrowable(throwable)
                );
                if (!isNot() && context != null) {
                    context.addDifference(mockServerLogger, "schema match failed expect:{}found error:{}for:{}", this.json, throwable.getMessage(), json);
                }
            }
        }
        return false;
    }

    public boolean matches(String json) {
        return matches(null, null, json);
    }

    public boolean matches(MockServerLogger mockServerLogger, MatchDifference context, String json) {
        if (schemaJsonNode != null) {
            try {
                // allow for string values without quotes to be validated as json strings
                if (isNotBlank(type) && type.equals("string") && isNotBlank(json) && !json.startsWith("\"") && !json.endsWith("\"")) {
                    json = "\"" + json + "\"";
                }
                String validationErrors = validate(json);
                boolean result = isNot() != validationErrors.isEmpty();
                if (!result && context != null) {
                    context.addDifference(mockServerLogger, "schema match failed expect:{}found:{}errors:{}", this.json, json, validationErrors);
                }
                return result;
            } catch (Throwable throwable) {
                MOCK_SERVER_LOGGER.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception validating JSON")
                        .setThrowable(throwable)
                );
                if (!isNot() && context != null) {
                    context.addDifference(mockServerLogger, "schema match failed expect:{}found error:{}for:{}", this.json, throwable.getMessage(), json);
                }
            }
            return isNot();
        } else {
            return !isNot();
        }
    }

    private String validate(String json) {
        if (schemaJsonNode.get("nullable") != null && TRUE.equals(schemaJsonNode.get("nullable").asText()) && StringUtils.isBlank(json)) {
            return "";
        } else if (StringUtils.isBlank(json)) {
            return "found blank value and value was not nullable";
        } else {
            return jsonSchemaValidator.isValid(json, false);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static String getNodeValue(ObjectNode node, String field) {
        if (node != null) {
            final JsonNode jsonNode = node.get(field);
            if (jsonNode == null) {
                return null;
            }
            return jsonNode.textValue();
        }
        return null;
    }

    @Override
    public String toString() {
        return json;
    }
}
