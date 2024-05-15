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
package software.xdev.mockserver.templates.engine.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.serialization.model.DTO;
import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import software.xdev.mockserver.serialization.model.HttpResponseDTO;
import software.xdev.mockserver.validator.jsonschema.JsonSchemaHttpRequestValidator;
import software.xdev.mockserver.validator.jsonschema.JsonSchemaHttpResponseValidator;
import org.slf4j.event.Level;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaHttpRequestValidator.jsonSchemaHttpRequestValidator;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaHttpResponseValidator.jsonSchemaHttpResponseValidator;

public class HttpTemplateOutputDeserializer {

    private static ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private final MockServerLogger mockServerLogger;
    private JsonSchemaHttpRequestValidator httpRequestValidator;
    private JsonSchemaHttpResponseValidator httpResponseValidator;

    public HttpTemplateOutputDeserializer(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
        httpRequestValidator = jsonSchemaHttpRequestValidator(mockServerLogger);
        httpResponseValidator = jsonSchemaHttpResponseValidator(mockServerLogger);
    }

    public <T> T deserializer(HttpRequest request, String json, Class<? extends DTO<T>> dtoClass) {
        T result = null;
        try {
            String validationErrors = "";
            if (dtoClass.isAssignableFrom(HttpResponseDTO.class)) {
                validationErrors = httpResponseValidator.isValid(json);
            } else if (dtoClass.isAssignableFrom(HttpRequestDTO.class)) {
                validationErrors = httpRequestValidator.isValid(json);
            }
            if (isEmpty(validationErrors)) {
                result = objectMapper.readValue(json, dtoClass).buildObject();
            } else {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setHttpRequest(request)
                        .setMessageFormat("validation failed:{}" + uncapitalize(dtoClass.getSimpleName()) + ":{}")
                        .setArguments(validationErrors, json)
                );
            }
        } catch (Exception e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setHttpRequest(request)
                    .setMessageFormat("exception transforming json:{}")
                    .setArguments(json)
                    .setThrowable(e)
            );
        }
        return result;
    }
}
