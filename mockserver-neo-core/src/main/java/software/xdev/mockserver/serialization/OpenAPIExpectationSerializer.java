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
package software.xdev.mockserver.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.mock.OpenAPIExpectation;
import software.xdev.mockserver.openapi.OpenAPIConverter;
import software.xdev.mockserver.serialization.model.OpenAPIExpectationDTO;
import software.xdev.mockserver.validator.jsonschema.JsonSchemaOpenAPIExpectationValidator;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.formatting.StringFormatter.formatLogMessage;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaOpenAPIExpectationValidator.jsonSchemaOpenAPIExpectationValidator;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaValidator.OPEN_API_SPECIFICATION_URL;

@SuppressWarnings("FieldMayBeFinal")
public class OpenAPIExpectationSerializer implements Serializer<OpenAPIExpectation> {
    private final MockServerLogger mockServerLogger;
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
    private JsonSchemaOpenAPIExpectationValidator expectationValidator;
    private OpenAPIConverter openAPIConverter;

    public OpenAPIExpectationSerializer(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
        this.openAPIConverter = new OpenAPIConverter(mockServerLogger);
    }

    private JsonSchemaOpenAPIExpectationValidator getValidator() {
        if (expectationValidator == null) {
            expectationValidator = jsonSchemaOpenAPIExpectationValidator(mockServerLogger);
        }
        return expectationValidator;
    }

    public String serialize(OpenAPIExpectation expectation) {
        if (expectation != null) {
            try {
                return objectWriter
                    .writeValueAsString(new OpenAPIExpectationDTO(expectation));
            } catch (Exception e) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception while serializing expectation to JSON with value " + expectation)
                        .setThrowable(e)
                );
                throw new RuntimeException("Exception while serializing expectation to JSON with value " + expectation, e);
            }
        } else {
            return "";
        }
    }

    public String serialize(List<OpenAPIExpectation> expectations) {
        return serialize(expectations.toArray(new OpenAPIExpectation[0]));
    }

    public String serialize(OpenAPIExpectation... expectations) {
        try {
            if (expectations != null && expectations.length > 0) {
                OpenAPIExpectationDTO[] expectationDTOs = new OpenAPIExpectationDTO[expectations.length];
                for (int i = 0; i < expectations.length; i++) {
                    expectationDTOs[i] = new OpenAPIExpectationDTO(expectations[i]);
                }
                return objectWriter
                    .writeValueAsString(expectationDTOs);
            } else {
                return "[]";
            }
        } catch (Exception e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception while serializing expectation to JSON with value " + Arrays.asList(expectations))
                    .setThrowable(e)
            );
            throw new RuntimeException("Exception while serializing expectation to JSON with value " + Arrays.asList(expectations), e);
        }
    }

    public OpenAPIExpectation deserialize(String jsonOpenAPIExpectation) {
        if (isBlank(jsonOpenAPIExpectation)) {
            throw new IllegalArgumentException(
                "1 error:" + NEW_LINE
                    + " - an expectation is required but value was \"" + jsonOpenAPIExpectation + "\"" + NEW_LINE +
                    NEW_LINE +
                    OPEN_API_SPECIFICATION_URL
            );
        } else {
            String validationErrors = getValidator().isValid(jsonOpenAPIExpectation);
            if (validationErrors.isEmpty()) {
                OpenAPIExpectation expectation = null;
                try {
                    OpenAPIExpectationDTO expectationDTO = objectMapper.readValue(jsonOpenAPIExpectation, OpenAPIExpectationDTO.class);
                    if (expectationDTO != null) {
                        expectation = expectationDTO.buildObject();
                    }
                } catch (Throwable throwable) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setMessageFormat("exception while parsing{}for OpenAPIExpectation " + throwable.getMessage())
                            .setArguments(jsonOpenAPIExpectation)
                            .setThrowable(throwable)
                    );
                    throw new IllegalArgumentException("exception while parsing [" + jsonOpenAPIExpectation + "] for OpenAPIExpectation", throwable);
                }
                return expectation;
            } else {
                throw new IllegalArgumentException(StringUtils.removeEndIgnoreCase(formatLogMessage("incorrect openapi expectation json format for:{}schema validation errors:{}", jsonOpenAPIExpectation, validationErrors), "\n"));
            }
        }
    }

    public List<Expectation> deserializeToExpectations(String jsonOpenAPIExpectation) {
        OpenAPIExpectation openAPIExpectation = deserialize(jsonOpenAPIExpectation);
        return openAPIConverter.buildExpectations(openAPIExpectation.getSpecUrlOrPayload(), openAPIExpectation.getOperationsAndResponses());
    }

    @Override
    public Class<OpenAPIExpectation> supportsType() {
        return OpenAPIExpectation.class;
    }

    public OpenAPIExpectation[] deserializeArray(String jsonOpenAPIExpectations, boolean allowEmpty) {
        List<OpenAPIExpectation> expectations = new ArrayList<>();
        if (isBlank(jsonOpenAPIExpectations)) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - an expectation or expectation array is required but value was \"" + jsonOpenAPIExpectations + "\"");
        } else {
            List<String> jsonOpenAPIExpectationList = jsonArraySerializer.splitJSONArray(jsonOpenAPIExpectations);
            if (!jsonOpenAPIExpectationList.isEmpty()) {
                List<String> validationErrorsList = new ArrayList<String>();
                for (String jsonExpectation : jsonOpenAPIExpectationList) {
                    try {
                        expectations.add(deserialize(jsonExpectation));
                    } catch (IllegalArgumentException iae) {
                        validationErrorsList.add(iae.getMessage());
                    }
                }
                if (!validationErrorsList.isEmpty()) {
                    if (validationErrorsList.size() > 1) {
                        throw new IllegalArgumentException(("[" + NEW_LINE + Joiner.on("," + NEW_LINE + NEW_LINE).join(validationErrorsList)).replaceAll(NEW_LINE, NEW_LINE + "  ") + NEW_LINE + "]");
                    } else {
                        throw new IllegalArgumentException(validationErrorsList.get(0));
                    }
                }
            } else if (!allowEmpty) {
                throw new IllegalArgumentException("1 error:" + NEW_LINE + " - an expectation or array of expectations is required");
            }
        }
        return expectations.toArray(new OpenAPIExpectation[0]);
    }

}
