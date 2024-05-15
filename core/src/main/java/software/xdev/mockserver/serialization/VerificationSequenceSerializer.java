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
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.serialization.model.VerificationSequenceDTO;
import software.xdev.mockserver.validator.jsonschema.JsonSchemaVerificationSequenceValidator;
import software.xdev.mockserver.verify.VerificationSequence;
import org.slf4j.event.Level;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.formatting.StringFormatter.formatLogMessage;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaValidator.OPEN_API_SPECIFICATION_URL;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaVerificationSequenceValidator.jsonSchemaVerificationSequenceValidator;

@SuppressWarnings("FieldMayBeFinal")
public class VerificationSequenceSerializer implements Serializer<VerificationSequence> {
    private final MockServerLogger mockServerLogger;
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonSchemaVerificationSequenceValidator verificationSequenceValidator;

    public VerificationSequenceSerializer(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
    }

    private JsonSchemaVerificationSequenceValidator getValidator() {
        if (verificationSequenceValidator == null) {
            verificationSequenceValidator = jsonSchemaVerificationSequenceValidator(mockServerLogger);
        }
        return verificationSequenceValidator;
    }

    public String serialize(VerificationSequence verificationSequence) {
        try {
            return objectWriter.writeValueAsString(new VerificationSequenceDTO(verificationSequence));
        } catch (Exception e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception while serializing verificationSequence to JSON with value " + verificationSequence)
                    .setThrowable(e)
            );
            throw new RuntimeException("Exception while serializing verificationSequence to JSON with value " + verificationSequence, e);
        }
    }

    public VerificationSequence deserialize(String jsonVerificationSequence) {
        if (isBlank(jsonVerificationSequence)) {
            throw new IllegalArgumentException(
                "1 error:" + NEW_LINE +
                    " - a verification sequence is required but value was \"" + jsonVerificationSequence + "\"" + NEW_LINE +
                    NEW_LINE +
                    OPEN_API_SPECIFICATION_URL
            );
        } else {
            String validationErrors = getValidator().isValid(jsonVerificationSequence);
            if (validationErrors.isEmpty()) {
                VerificationSequence verificationSequence = null;
                try {
                    VerificationSequenceDTO verificationDTO = objectMapper.readValue(jsonVerificationSequence, VerificationSequenceDTO.class);
                    if (verificationDTO != null) {
                        verificationSequence = verificationDTO.buildObject();
                    }
                } catch (Throwable throwable) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setMessageFormat("exception while parsing{}for VerificationSequence " + throwable.getMessage())
                            .setArguments(jsonVerificationSequence)
                            .setThrowable(throwable)
                    );
                    throw new IllegalArgumentException("exception while parsing [" + jsonVerificationSequence + "] for VerificationSequence", throwable);
                }
                return verificationSequence;
            } else {
                throw new IllegalArgumentException(StringUtils.removeEndIgnoreCase(formatLogMessage("incorrect verification sequence json format for:{}schema validation errors:{}", jsonVerificationSequence, validationErrors), "\n"));
            }
        }
    }

    @Override
    public Class<VerificationSequence> supportsType() {
        return VerificationSequence.class;
    }

}