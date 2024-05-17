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
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import software.xdev.mockserver.serialization.model.HttpRequestPrettyPrintedDTO;
import software.xdev.mockserver.serialization.model.RequestDefinitionDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;

public class RequestDefinitionSerializer implements Serializer<RequestDefinition> {
    
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();

    public String serialize(RequestDefinition requestDefinition) {
        return serialize(false, requestDefinition);
    }

    public String serialize(boolean prettyPrint, RequestDefinition requestDefinition) {
        try {
            if (requestDefinition instanceof HttpRequest request) {
                return objectWriter.writeValueAsString(prettyPrint
                    ? new HttpRequestPrettyPrintedDTO(request)
                    : new HttpRequestDTO(request));
            }
            return "";
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing RequestDefinition to JSON with value " + requestDefinition, e);
        }
    }

    public String serialize(List<? extends RequestDefinition> requestDefinitions) {
        return serialize(false, requestDefinitions);
    }

    public String serialize(boolean prettyPrint, List<? extends RequestDefinition> requestDefinitions) {
        return serialize(prettyPrint, requestDefinitions.toArray(new RequestDefinition[0]));
    }

    public String serialize(RequestDefinition... requestDefinitions) {
        return serialize(false, requestDefinitions);
    }

    public String serialize(boolean prettyPrint, RequestDefinition... requestDefinitions) {
        try {
            if (requestDefinitions != null && requestDefinitions.length > 0) {
                Object[] requestDefinitionDTOs = new Object[requestDefinitions.length];
                for (int i = 0; i < requestDefinitions.length; i++) {
                    if (requestDefinitions[i] instanceof HttpRequest request) {
                        requestDefinitionDTOs[i] = prettyPrint
                            ? new HttpRequestPrettyPrintedDTO(request)
                            : new HttpRequestDTO(request);
                    }
                }
                return objectWriter.writeValueAsString(requestDefinitionDTOs);
            } else {
                return "[]";
            }
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing RequestDefinition to JSON with value " + Arrays.asList(requestDefinitions), e);
        }
    }

    public RequestDefinition deserialize(String jsonRequestDefinition) {
        try {
            if (jsonRequestDefinition.contains("\"httpRequest\"")) {
                JsonNode jsonNode = objectMapper.readTree(jsonRequestDefinition);
                if (jsonNode.has("httpRequest")) {
                    jsonRequestDefinition = jsonNode.get("httpRequest").toString();
                }
            } else if (jsonRequestDefinition.contains("\"openAPIDefinition\"")) {
                JsonNode jsonNode = objectMapper.readTree(jsonRequestDefinition);
                if (jsonNode.has("openAPIDefinition")) {
                    jsonRequestDefinition = jsonNode.get("openAPIDefinition").toString();
                }
            }
            RequestDefinitionDTO requestDefinitionDTO = objectMapper.readValue(jsonRequestDefinition, RequestDefinitionDTO.class);
            if (requestDefinitionDTO != null) {
                return requestDefinitionDTO.buildObject();
            }
            return null;
        } catch (Exception ex) {
            throw new IllegalArgumentException("exception while parsing [" + jsonRequestDefinition + "] for RequestDefinition", ex);
        }
    }

    @Override
    public Class<RequestDefinition> supportsType() {
        return RequestDefinition.class;
    }

    public RequestDefinition[] deserializeArray(String jsonRequestDefinitions) {
        List<RequestDefinition> requestDefinitions = new ArrayList<>();
        if (isBlank(jsonRequestDefinitions)) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request or request array is required but value was \"" + jsonRequestDefinitions + "\"");
        } else {
            List<String> jsonRequestList = jsonArraySerializer.splitJSONArray(jsonRequestDefinitions);
            if (jsonRequestList.isEmpty()) {
                throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request or array of request is required");
            } else {
                List<String> validationErrorsList = new ArrayList<>();
                for (String jsonRequest : jsonRequestList) {
                    try {
                        requestDefinitions.add(deserialize(jsonRequest));
                    } catch (IllegalArgumentException iae) {
                        validationErrorsList.add(iae.getMessage());
                    }

                }
                if (!validationErrorsList.isEmpty()) {
                    throw new IllegalArgumentException((validationErrorsList.size() > 1 ? "[" : "")
                        + String.join("," + NEW_LINE, validationErrorsList)
                        + (validationErrorsList.size() > 1 ? "]" : ""));
                }
            }
        }
        return requestDefinitions.toArray(new RequestDefinition[0]);
    }

}
