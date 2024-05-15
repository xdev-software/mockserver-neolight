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
package software.xdev.mockserver.openapi;

import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.openapi.examples.ExampleBuilder;
import software.xdev.mockserver.openapi.examples.JsonNodeExampleSerializer;
import software.xdev.mockserver.openapi.examples.models.StringExample;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.xdev.mockserver.model.HttpResponse.response;
import static software.xdev.mockserver.model.JsonBody.json;
import static software.xdev.mockserver.model.OpenAPIDefinition.openAPI;
import static software.xdev.mockserver.openapi.OpenAPIParser.buildOpenAPI;
import static org.slf4j.event.Level.ERROR;

public class OpenAPIConverter {

    private static final ObjectWriter OBJECT_WRITER = ObjectMapperFactory.createObjectMapper(new JsonNodeExampleSerializer()).writerWithDefaultPrettyPrinter();
    private final MockServerLogger mockServerLogger;

    public OpenAPIConverter(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
    }

    public List<Expectation> buildExpectations(String specUrlOrPayload, Map<String, String> operationsAndResponses) {
        OpenAPI openAPI = buildOpenAPI(specUrlOrPayload, mockServerLogger);
        AtomicInteger expectationCounter = new AtomicInteger(0);
        return openAPI
            .getPaths()
            .values()
            .stream()
            .flatMap(pathItem ->
                pathItem
                    .readOperations()
                    .stream()
            )
            .filter(operation -> operationsAndResponses == null || operationsAndResponses.containsKey(operation.getOperationId()))
            .map(operation -> new Expectation(openAPI(specUrlOrPayload, operation.getOperationId()))
                .thenRespond(buildHttpResponse(
                    openAPI,
                    operation.getResponses(),
                    operationsAndResponses != null ? operationsAndResponses.get(operation.getOperationId()) : null
                ))
            )
            .map(expectation -> {
                int index = expectationCounter.incrementAndGet();
                return expectation.withId(new UUID((long) Objects.hash(specUrlOrPayload, operationsAndResponses) * index, (long) Objects.hash(specUrlOrPayload, operationsAndResponses) * index).toString());
            })
            .collect(Collectors.toList());
    }

    private HttpResponse buildHttpResponse(OpenAPI openAPI, ApiResponses apiResponses, String apiResponseKey) {
        HttpResponse response = response();
        Optional
            .ofNullable(apiResponses)
            .flatMap(notNullApiResponses -> notNullApiResponses.entrySet().stream().filter(entry -> isBlank(apiResponseKey) | entry.getKey().equals(apiResponseKey)).findFirst())
            .ifPresent(apiResponse -> {
                if (!apiResponse.getKey().equalsIgnoreCase("default")) {
                    response.withStatusCode(Integer.parseInt(apiResponse.getKey()));
                }
                Optional
                    .ofNullable(apiResponse.getValue().getHeaders())
                    .map(Map::entrySet)
                    .map(Set::stream)
                    .ifPresent(stream -> stream
                        .forEach(entry -> {
                            Header value = entry.getValue();
                            Example example = findExample(value);
                            if (example != null) {
                                response.withHeader(entry.getKey(), String.valueOf(example.getValue()));
                            } else if (value.getSchema() != null) {
                                software.xdev.mockserver.openapi.examples.models.Example generatedExample = ExampleBuilder.fromSchema(value.getSchema(), openAPI.getComponents() != null ? openAPI.getComponents().getSchemas() : null);
                                if (generatedExample instanceof StringExample) {
                                    response.withHeader(entry.getKey(), ((StringExample) generatedExample).getValue());
                                } else {
                                    response.withHeader(entry.getKey(), serialise(generatedExample));
                                }
                            }
                        })
                    );
                Optional
                    .ofNullable(apiResponse.getValue().getContent())
                    .flatMap(content -> content
                        .entrySet()
                        .stream()
                        .findFirst()
                    )
                    .ifPresent(contentType -> {
                        response.withHeader("content-type", contentType.getKey());
                        Optional
                            .ofNullable(contentType.getValue())
                            .ifPresent(mediaType -> {
                                Object example = findExample(mediaType);
                                if (example instanceof Example) {
                                    if (isJsonContentType(contentType.getKey())) {
                                        response.withBody(json(serialise(((Example) example).getValue())));
                                    } else {
                                        response.withBody(String.valueOf(((Example) example).getValue()));
                                    }
                                } else if (example != null) {
                                    if (isJsonContentType(contentType.getKey())) {
                                        response.withBody(json(serialise(mediaType.getExample())));
                                    } else {
                                        response.withBody(serialise(mediaType.getExample()));
                                    }
                                } else if (mediaType.getSchema() != null) {
                                    software.xdev.mockserver.openapi.examples.models.Example generatedExample = ExampleBuilder.fromSchema(mediaType.getSchema(), openAPI.getComponents() != null ? openAPI.getComponents().getSchemas() : null);
                                    if (generatedExample instanceof StringExample) {
                                        if (isJsonContentType(contentType.getKey())) {
                                            response.withBody(json(serialise(((StringExample) generatedExample).getValue())));
                                        } else {
                                            response.withBody(((StringExample) generatedExample).getValue());
                                        }
                                    } else {
                                        software.xdev.mockserver.openapi.examples.models.Example exampleFromSchema = ExampleBuilder.fromSchema(mediaType.getSchema(), openAPI.getComponents() != null ? openAPI.getComponents().getSchemas() : null);
                                        if (exampleFromSchema != null) {
                                            String serialise = serialise(exampleFromSchema);
                                            if (isJsonContentType(contentType.getKey())) {
                                                response.withBody(json(serialise));
                                            } else {
                                                response.withBody(serialise);
                                            }
                                        }
                                    }
                                }
                            });
                    });
            });
        return response;
    }

    public static boolean isJsonContentType(String contentType) {
        return software.xdev.mockserver.model.MediaType.parse(contentType).isJson();
    }

    private Example findExample(Header value) {
        Example example = null;
        if (value.getExample() instanceof Example) {
            example = (Example) value.getExample();
        } else if (value.getExamples() != null && !value.getExamples().isEmpty()) {
            example = value.getExamples().values().stream().findFirst().orElse(null);
        }
        return example;
    }

    private Object findExample(MediaType mediaType) {
        Object example = null;
        if (mediaType.getExample() != null) {
            example = mediaType.getExample();
        } else if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
            example = mediaType.getExamples().values().stream().findFirst().orElse(null);
        }
        return example;
    }

    private String serialise(Object example) {
        try {
            return OBJECT_WRITER.writeValueAsString(example);
        } catch (Throwable throwable) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(ERROR)
                    .setMessageFormat("exception while serialising " + example.getClass() + " {}")
                    .setArguments(example)
                    .setThrowable(throwable)
            );
            return "";
        }
    }
}
