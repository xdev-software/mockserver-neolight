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
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.serialization.model.HttpResponseDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;

@SuppressWarnings("FieldMayBeFinal")
public class HttpResponseSerializer implements Serializer<HttpResponse> {
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();

    public String serialize(HttpResponse httpResponse) {
        try {
            return objectWriter.writeValueAsString(new HttpResponseDTO(httpResponse));
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing httpResponse to JSON with value " + httpResponse, e);
        }
    }

    public String serialize(List<HttpResponse> httpResponses) {
        return serialize(httpResponses.toArray(new HttpResponse[0]));
    }

    public String serialize(HttpResponse... httpResponses) {
        try {
            if (httpResponses != null && httpResponses.length > 0) {
                HttpResponseDTO[] httpResponseDTOs = new HttpResponseDTO[httpResponses.length];
                for (int i = 0; i < httpResponses.length; i++) {
                    httpResponseDTOs[i] = new HttpResponseDTO(httpResponses[i]);
                }
                return objectWriter.writeValueAsString(httpResponseDTOs);
            } else {
                return "[]";
            }
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing HttpResponse to JSON with value " + Arrays.asList(httpResponses), e);
        }
    }

    public HttpResponse deserialize(String jsonHttpResponse) {
        if (jsonHttpResponse.contains("\"httpResponse\"")) {
            try {
                JsonNode jsonNode = objectMapper.readTree(jsonHttpResponse);
                if (jsonNode.has("httpResponse")) {
                    jsonHttpResponse = jsonNode.get("httpResponse").toString();
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("exception while parsing [" + jsonHttpResponse + "] for HttpResponse", ex);
            }
        }
        HttpResponse httpResponse = null;
        try {
            HttpResponseDTO httpResponseDTO = objectMapper.readValue(jsonHttpResponse, HttpResponseDTO.class);
            if (httpResponseDTO != null) {
                httpResponse = httpResponseDTO.buildObject();
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("exception while parsing [" + jsonHttpResponse + "] for HttpResponse", ex);
        }
        return httpResponse;
    }

    @Override
    public Class<HttpResponse> supportsType() {
        return HttpResponse.class;
    }

    public HttpResponse[] deserializeArray(String jsonHttpResponses) {
        List<HttpResponse> httpResponses = new ArrayList<>();
        if (isBlank(jsonHttpResponses)) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a response or response array is required but value was \"" + jsonHttpResponses + "\"");
        } else {
            List<String> jsonResponseList = jsonArraySerializer.splitJSONArray(jsonHttpResponses);
            if (jsonResponseList.isEmpty()) {
                throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a response or array of response is required");
            } else {
                List<String> validationErrorsList = new ArrayList<>();
                for (String jsonExpectation : jsonResponseList) {
                    try {
                        httpResponses.add(deserialize(jsonExpectation));
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
        return httpResponses.toArray(new HttpResponse[0]);
    }

}
