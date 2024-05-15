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
import software.xdev.mockserver.serialization.model.VerificationDTO;
import software.xdev.mockserver.validator.jsonschema.JsonSchemaVerificationValidator;
import software.xdev.mockserver.verify.Verification;
import org.slf4j.event.Level;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.formatting.StringFormatter.formatLogMessage;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaValidator.OPEN_API_SPECIFICATION_URL;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaVerificationValidator.jsonSchemaVerificationValidator;

@SuppressWarnings("FieldMayBeFinal")
public class VerificationSerializer implements Serializer<Verification> {
    private final MockServerLogger mockServerLogger;
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonSchemaVerificationValidator verificationValidator;

    public VerificationSerializer(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
    }

    private JsonSchemaVerificationValidator getValidator() {
        if (verificationValidator == null) {
            verificationValidator = jsonSchemaVerificationValidator(mockServerLogger);
        }
        return verificationValidator;
    }

    public String serialize(Verification verification) {
        try {
            return objectWriter.writeValueAsString(new VerificationDTO(verification));
        } catch (Exception e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception while serializing verification to JSON with value " + verification)
                    .setThrowable(e)
            );
            throw new RuntimeException("Exception while serializing verification to JSON with value " + verification, e);
        }
    }

    public Verification deserialize(String jsonVerification) {
        if (isBlank(jsonVerification)) {
            throw new IllegalArgumentException(
                "1 error:" + NEW_LINE +
                    " - a verification is required but value was \"" + jsonVerification + "\"" + NEW_LINE +
                    NEW_LINE +
                    OPEN_API_SPECIFICATION_URL
            );
        } else {
            String validationErrors = getValidator().isValid(jsonVerification);
            if (validationErrors.isEmpty()) {
                Verification verification = null;
                try {
                    VerificationDTO verificationDTO = objectMapper.readValue(jsonVerification, VerificationDTO.class);
                    if (verificationDTO != null) {
                        verification = verificationDTO.buildObject();
                    }
                } catch (Throwable throwable) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setMessageFormat("exception while parsing{}for Verification " + throwable.getMessage())
                            .setArguments(jsonVerification)
                            .setThrowable(throwable)
                    );
                    throw new IllegalArgumentException("exception while parsing [" + jsonVerification + "] for Verification", throwable);
                }
                return verification;
            } else {
                throw new IllegalArgumentException(StringUtils.removeEndIgnoreCase(formatLogMessage("incorrect verification json format for:{}schema validation errors:{}", jsonVerification, validationErrors), "\n"));
            }
        }
    }

    @Override
    public Class<Verification> supportsType() {
        return Verification.class;
    }

}