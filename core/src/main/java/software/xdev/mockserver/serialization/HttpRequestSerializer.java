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
import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import software.xdev.mockserver.serialization.model.HttpRequestPrettyPrintedDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;

@SuppressWarnings("FieldMayBeFinal")
public class HttpRequestSerializer implements Serializer<HttpRequest> {
    
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();

    public String serialize(HttpRequest httpRequest) {
        return serialize(false, httpRequest);
    }

    public String serialize(boolean prettyPrint, HttpRequest httpRequest) {
        try {
            if (prettyPrint) {
                return objectWriter.writeValueAsString(new HttpRequestPrettyPrintedDTO(httpRequest));
            } else {
                return objectWriter.writeValueAsString(new HttpRequestDTO(httpRequest));
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while serializing HttpRequest to JSON with value " + httpRequest, e);
        }
    }

    public String serialize(List<HttpRequest> httpRequests) {
        return serialize(false, httpRequests);
    }

    public String serialize(boolean prettyPrint, List<HttpRequest> httpRequests) {
        return serialize(prettyPrint, httpRequests.toArray(new HttpRequest[0]));
    }

    public String serialize(HttpRequest... httpRequests) {
        return serialize(false, httpRequests);
    }

    public String serialize(boolean prettyPrint, HttpRequest... httpRequests) {
        try {
            if (httpRequests != null && httpRequests.length > 0) {
                if (prettyPrint) {
                    HttpRequestPrettyPrintedDTO[] httpRequestTemplateObjects = new HttpRequestPrettyPrintedDTO[httpRequests.length];
                    for (int i = 0; i < httpRequests.length; i++) {
                        httpRequestTemplateObjects[i] = new HttpRequestPrettyPrintedDTO(httpRequests[i]);
                    }
                    return objectWriter.writeValueAsString(httpRequestTemplateObjects);
                } else {
                    HttpRequestDTO[] httpRequestDTOs = new HttpRequestDTO[httpRequests.length];
                    for (int i = 0; i < httpRequests.length; i++) {
                        httpRequestDTOs[i] = new HttpRequestDTO(httpRequests[i]);
                    }
                    return objectWriter.writeValueAsString(httpRequestDTOs);
                }
            } else {
                return "[]";
            }
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing HttpRequest to JSON with value " + Arrays.asList(httpRequests), e);
        }
    }

    public HttpRequest deserialize(String jsonHttpRequest) {
        if (jsonHttpRequest.contains("\"httpRequest\"")) {
            try {
                JsonNode jsonNode = objectMapper.readTree(jsonHttpRequest);
                if (jsonNode.has("httpRequest")) {
                    jsonHttpRequest = jsonNode.get("httpRequest").toString();
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("exception while parsing [" + jsonHttpRequest + "] for HttpRequest", ex);
            }
        }
        HttpRequest httpRequest = null;
        try {
            HttpRequestDTO httpRequestDTO = objectMapper.readValue(jsonHttpRequest, HttpRequestDTO.class);
            if (httpRequestDTO != null) {
                httpRequest = httpRequestDTO.buildObject();
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("exception while parsing [" + jsonHttpRequest + "] for HttpRequest", ex);
        }
        return httpRequest;
    }

    @Override
    public Class<HttpRequest> supportsType() {
        return HttpRequest.class;
    }

    public HttpRequest[] deserializeArray(String jsonHttpRequests) {
        List<HttpRequest> httpRequests = new ArrayList<>();
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
                    throw new IllegalArgumentException((validationErrorsList.size() > 1 ? "[" : "")
                        + String.join("," + NEW_LINE, validationErrorsList)
                        + (validationErrorsList.size() > 1 ? "]" : ""));
                }
            }
        }
        return httpRequests.toArray(new HttpRequest[0]);
    }

}
