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

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import software.xdev.mockserver.model.LogEventRequestAndResponse;
import software.xdev.mockserver.serialization.model.LogEventRequestAndResponseDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;

public class LogEventRequestAndResponseSerializer {
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
    private static final ObjectWriter objectWriter = ObjectMapperFactory
        .createObjectMapper()
        .writer(
            new DefaultPrettyPrinter()
                .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
                .withObjectIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        );

    public String serialize(LogEventRequestAndResponse httpRequestAndHttpResponse) {
        try {
            return objectWriter.writeValueAsString(new LogEventRequestAndResponseDTO(httpRequestAndHttpResponse));
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while serializing HttpRequestAndHttpResponse to JSON with value " + httpRequestAndHttpResponse, e);
        }
    }

    public String serialize(List<LogEventRequestAndResponse> httpRequestAndHttpResponses) {
        return serialize(httpRequestAndHttpResponses.toArray(new LogEventRequestAndResponse[0]));
    }

    public String serialize(LogEventRequestAndResponse... httpRequestAndHttpResponses) {
        try {
            if (httpRequestAndHttpResponses != null && httpRequestAndHttpResponses.length > 0) {
                LogEventRequestAndResponseDTO[] httpRequestAndHttpResponseDTOS = new LogEventRequestAndResponseDTO[httpRequestAndHttpResponses.length];
                for (int i = 0; i < httpRequestAndHttpResponses.length; i++) {
                    httpRequestAndHttpResponseDTOS[i] = new LogEventRequestAndResponseDTO(httpRequestAndHttpResponses[i]);
                }
                return objectWriter
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(httpRequestAndHttpResponseDTOS);
            } else {
                return "[]";
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while serializing HttpRequestAndHttpResponse to JSON with value " + Arrays.asList(httpRequestAndHttpResponses), e);
        }
    }

    public LogEventRequestAndResponse deserialize(String jsonHttpRequestAndHttpResponse) {
        if (isBlank(jsonHttpRequestAndHttpResponse)) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request is required but value was \"" + jsonHttpRequestAndHttpResponse + "\"");
        } else {
            LogEventRequestAndResponse httpRequestAndHttpResponse = null;
            try {
                LogEventRequestAndResponseDTO httpRequestAndHttpResponseDTO = objectMapper.readValue(jsonHttpRequestAndHttpResponse, LogEventRequestAndResponseDTO.class);
                if (httpRequestAndHttpResponseDTO != null) {
                    httpRequestAndHttpResponse = httpRequestAndHttpResponseDTO.buildObject();
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("exception while parsing [" + jsonHttpRequestAndHttpResponse + "] for HttpRequestAndHttpResponse", ex);
            }
            return httpRequestAndHttpResponse;
        }
    }

    public LogEventRequestAndResponse[] deserializeArray(String jsonHttpRequestAndHttpResponse) {
        List<LogEventRequestAndResponse> httpRequestAndHttpResponses = new ArrayList<>();
        if (isBlank(jsonHttpRequestAndHttpResponse)) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request or request array is required but value was \"" + jsonHttpRequestAndHttpResponse + "\"");
        } else {
            List<String> jsonRequestList = jsonArraySerializer.splitJSONArray(jsonHttpRequestAndHttpResponse);
            if (jsonRequestList.isEmpty()) {
                throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request or array of request is required");
            } else {
                List<String> validationErrorsList = new ArrayList<String>();
                for (String jsonRequest : jsonRequestList) {
                    try {
                        httpRequestAndHttpResponses.add(deserialize(jsonRequest));
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
        return httpRequestAndHttpResponses.toArray(new LogEventRequestAndResponse[0]);
    }

}
