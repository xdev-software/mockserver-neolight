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
import software.xdev.mockserver.model.ExpectationId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;

@SuppressWarnings("FieldMayBeFinal")
public class ExpectationIdSerializer implements Serializer<ExpectationId> {
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();

    public String serialize(ExpectationId expectationId) {
        try {
            return objectWriter.writeValueAsString(expectationId);
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing ExpectationId to JSON with value " + expectationId, e);
        }
    }

    public String serialize(List<? extends ExpectationId> expectationIds) {
        return serialize(expectationIds.toArray(new ExpectationId[0]));
    }

    public String serialize(ExpectationId... expectationIds) {
        try {
            if (expectationIds != null && expectationIds.length > 0) {
                return objectWriter.writeValueAsString(expectationIds);
            }
            return "[]";
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing ExpectationId to JSON with value " + Arrays.asList(expectationIds), e);
        }
    }

    public ExpectationId deserialize(String jsonExpectationId) {
        try {
            if (jsonExpectationId.contains("\"httpRequest\"")) {
                JsonNode jsonNode = objectMapper.readTree(jsonExpectationId);
                if (jsonNode.has("httpRequest")) {
                    jsonExpectationId = jsonNode.get("httpRequest").toString();
                }
            } else if (jsonExpectationId.contains("\"openAPIDefinition\"")) {
                JsonNode jsonNode = objectMapper.readTree(jsonExpectationId);
                if (jsonNode.has("openAPIDefinition")) {
                    jsonExpectationId = jsonNode.get("openAPIDefinition").toString();
                }
            }
            return objectMapper.readValue(jsonExpectationId, ExpectationId.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("exception while parsing [" + jsonExpectationId + "] for ExpectationId", ex);
        }
    }

    @Override
    public Class<ExpectationId> supportsType() {
        return ExpectationId.class;
    }

    public ExpectationId[] deserializeArray(String jsonExpectationIds) {
        List<ExpectationId> expectationIds = new ArrayList<>();
        if (isBlank(jsonExpectationIds)) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request or request array is required but value was \"" + jsonExpectationIds + "\"");
        } else {
            List<String> jsonRequestList = jsonArraySerializer.splitJSONArray(jsonExpectationIds);
            if (jsonRequestList.isEmpty()) {
                throw new IllegalArgumentException("1 error:" + NEW_LINE + " - a request or array of request is required");
            } else {
                List<String> validationErrorsList = new ArrayList<>();
                for (String jsonRequest : jsonRequestList) {
                    try {
                        expectationIds.add(deserialize(jsonRequest));
                    } catch (IllegalArgumentException iae) {
                        validationErrorsList.add(iae.getMessage());
                    }

                }
                if (!validationErrorsList.isEmpty()) {
                    throw new IllegalArgumentException((validationErrorsList.size() > 1 ? "[" : "") + Joiner.on("," + NEW_LINE).join(validationErrorsList) + (validationErrorsList.size() > 1 ? "]" : ""));
                }
            }
        }
        return expectationIds.toArray(new ExpectationId[0]);
    }

}
