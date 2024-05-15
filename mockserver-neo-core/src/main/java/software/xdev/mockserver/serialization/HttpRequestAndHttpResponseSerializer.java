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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.serialization.model.HttpRequestAndHttpResponseDTO;
import software.xdev.mockserver.validator.jsonschema.JsonSchemaHttpRequestAndHttpResponseValidator;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.formatting.StringFormatter.formatLogMessage;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaHttpRequestAndHttpResponseValidator.jsonSchemaHttpRequestAndHttpResponseValidator;
import static software.xdev.mockserver.validator.jsonschema.JsonSchemaValidator.OPEN_API_SPECIFICATION_URL;

@SuppressWarnings("FieldMayBeFinal")
public class HttpRequestAndHttpResponseSerializer implements Serializer<HttpRequestAndHttpResponse> {
    private final MockServerLogger mockServerLogger;
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
    private JsonSchemaHttpRequestAndHttpResponseValidator jsonSchemaHttpRequestAndHttpResponseValidator;

    public HttpRequestAndHttpResponseSerializer(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;

    }

    private JsonSchemaHttpRequestAndHttpResponseValidator getValidator() {
        if (jsonSchemaHttpRequestAndHttpResponseValidator == null) {
            jsonSchemaHttpRequestAndHttpResponseValidator = jsonSchemaHttpRequestAndHttpResponseValidator(mockServerLogger);
        }
        return jsonSchemaHttpRequestAndHttpResponseValidator;
    }

    public String serialize(HttpRequestAndHttpResponse httpRequestAndHttpResponse) {
        try {
            return objectWriter.writeValueAsString(new HttpRequestAndHttpResponseDTO(httpRequestAndHttpResponse));
        } catch (Exception e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception while serializing HttpRequestAndHttpResponse to JSON with value " + httpRequestAndHttpResponse)
                    .setThrowable(e)
            );
            throw new RuntimeException("Exception while serializing HttpRequestAndHttpResponse to JSON with value " + httpRequestAndHttpResponse, e);
        }
    }

    public String serialize(List<HttpRequestAndHttpResponse> httpRequests) {
        return serialize(httpRequests.toArray(new HttpRequestAndHttpResponse[0]));
    }

    public String serialize(HttpRequestAndHttpResponse... httpRequests) {
        try {
            if (httpRequests != null && httpRequests.length > 0) {
                HttpRequestAndHttpResponseDTO[] httpRequestDTOs = new HttpRequestAndHttpResponseDTO[httpRequests.length];
                for (int i = 0; i < httpRequests.length; i++) {
                    httpRequestDTOs[i] = new HttpRequestAndHttpResponseDTO(httpRequests[i]);
                }
                return objectWriter.writeValueAsString(httpRequestDTOs);
            } else {
                return "[]";
            }
        } catch (Exception e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception while serializing HttpRequestAndHttpResponse to JSON with value " + Arrays.asList(httpRequests))
                    .setThrowable(e)
            );
            throw new RuntimeException("Exception while serializing HttpRequestAndHttpResponse to JSON with value " + Arrays.asList(httpRequests), e);
        }
    }

    public HttpRequestAndHttpResponse deserialize(String jsonHttpRequest) {
        if (isBlank(jsonHttpRequest)) {
            throw new IllegalArgumentException(
                "1 error:" + NEW_LINE
                    + " - a request is required but value was \"" + jsonHttpRequest + "\"" + NEW_LINE +
                    NEW_LINE +
                    OPEN_API_SPECIFICATION_URL
            );
        } else {
            if (jsonHttpRequest.contains("\"httpRequestAndHttpResponse\"")) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(jsonHttpRequest);
                    if (jsonNode.has("httpRequestAndHttpResponse")) {
                        jsonHttpRequest = jsonNode.get("httpRequestAndHttpResponse").toString();
                    }
                } catch (Throwable throwable) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setMessageFormat("exception while parsing{}for HttpRequestAndHttpResponse " + throwable.getMessage())
                            .setArguments(jsonHttpRequest)
                            .setThrowable(throwable)
                    );
                    throw new IllegalArgumentException("exception while parsing [" + jsonHttpRequest + "] for HttpRequestAndHttpResponse", throwable);
                }
            }
            String validationErrors = getValidator().isValid(jsonHttpRequest);
            if (validationErrors.isEmpty()) {
                HttpRequestAndHttpResponse httpRequestAndHttpResponse = null;
                try {
                    HttpRequestAndHttpResponseDTO httpRequestDTO = objectMapper.readValue(jsonHttpRequest, HttpRequestAndHttpResponseDTO.class);
                    if (httpRequestDTO != null) {
                        httpRequestAndHttpResponse = httpRequestDTO.buildObject();
                    }
                } catch (Throwable throwable) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setMessageFormat("exception while parsing{}for HttpRequestAndHttpResponse " + throwable.getMessage())
                            .setArguments(jsonHttpRequest)
                            .setThrowable(throwable)
                    );
                    throw new IllegalArgumentException("exception while parsing [" + jsonHttpRequest + "] for HttpRequestAndHttpResponse", throwable);
                }
                return httpRequestAndHttpResponse;
            } else {
                throw new IllegalArgumentException(StringUtils.removeEndIgnoreCase(formatLogMessage("incorrect json format for:{}schema validation errors:{}", jsonHttpRequest, validationErrors), "\n"));
            }
        }
    }

    @Override
    public Class<HttpRequestAndHttpResponse> supportsType() {
        return HttpRequestAndHttpResponse.class;
    }

    public HttpRequestAndHttpResponse[] deserializeArray(String jsonHttpRequests) {
        List<HttpRequestAndHttpResponse> httpRequests = new ArrayList<>();
        if (isBlank(jsonHttpRequests)) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request or request array is required but value was \"" + jsonHttpRequests + "\"");
        } else {
            List<String> jsonRequestList = jsonArraySerializer.splitJSONArray(jsonHttpRequests);
            if (jsonRequestList.isEmpty()) {
                throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request or array of request is required");
            } else {
                List<String> validationErrorsList = new ArrayList<>();
                for (String jsonRequest : jsonRequestList) {
                    try {
                        httpRequests.add(deserialize(jsonRequest));
                    } catch (IllegalArgumentException iae) {
                        validationErrorsList.add(iae.getMessage());
                    }

                }
                if (!validationErrorsList.isEmpty()) {
                    throw new IllegalArgumentException((validationErrorsList.size() > 1 ? "[" : "") + Joiner.on("," + NEW_LINE).join(validationErrorsList) + (validationErrorsList.size() > 1 ? "]" : ""));
                }
            }
        }
        return httpRequests.toArray(new HttpRequestAndHttpResponse[0]);
    }

}
