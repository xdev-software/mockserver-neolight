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
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.serialization.model.ExpectationDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;

@SuppressWarnings("FieldMayBeFinal")
public class ExpectationSerializer implements Serializer<Expectation> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExpectationSerializer.class);
    
    private ObjectWriter objectWriter;
    private ObjectMapper objectMapper;
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();

    public ExpectationSerializer() {
        this(false);
    }

    public ExpectationSerializer(boolean serialiseDefaultValues) {
        this.objectWriter = ObjectMapperFactory.createObjectMapper(true, serialiseDefaultValues);
        this.objectMapper = ObjectMapperFactory.createObjectMapper();
    }

    public String serialize(Expectation expectation) {
        if (expectation == null) {
            return "";
        }
        try {
            return objectWriter.writeValueAsString(new ExpectationDTO(expectation));
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing expectation to JSON with value " + expectation, e);
        }
    }

    public String serialize(List<Expectation> expectations) {
        return serialize(expectations.toArray(new Expectation[0]));
    }

    public String serialize(Expectation... expectations) {
        try {
            if (expectations == null || expectations.length == 0) {
                return "[]";
            }
            ExpectationDTO[] expectationDTOs = new ExpectationDTO[expectations.length];
            for (int i = 0; i < expectations.length; i++) {
                expectationDTOs[i] = new ExpectationDTO(expectations[i]);
            }
            return objectWriter.writeValueAsString(expectationDTOs);
        } catch (Exception e) {
            throw new IllegalStateException("Exception while serializing expectation to JSON with value " + Arrays.asList(expectations), e);
        }
    }

    public Expectation deserialize(String jsonExpectation) {
        try {
            ExpectationDTO expectationDTO = objectMapper.readValue(jsonExpectation, ExpectationDTO.class);
            if (expectationDTO != null) {
                return expectationDTO.buildObject();
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("exception while parsing [" + jsonExpectation + "] for Expectation", ex);
        }
        return null;
    }

    @Override
    public Class<Expectation> supportsType() {
        return Expectation.class;
    }

    public Expectation[] deserializeArray(String jsonExpectations, boolean allowEmpty) {
        return deserializeArray(jsonExpectations, allowEmpty, (s, expectation) -> expectation);
    }

    public Expectation[] deserializeArray(String jsonExpectations, boolean allowEmpty, BiFunction<String, List<Expectation>, List<Expectation>> expectationModifier) {
        if (isBlank(jsonExpectations)) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - an expectation or expectation array is required but value was \"" + jsonExpectations + "\"");
        }
        List<Expectation> expectations = new ArrayList<>();
        List<String> validationErrorsList = new ArrayList<>();
        List<JsonNode> jsonExpectationList = jsonArraySerializer.splitJSONArrayToJSONNodes(jsonExpectations);
        if (!jsonExpectationList.isEmpty()) {
            for (int i = 0; i < jsonExpectationList.size(); i++) {
                String jsonExpectation = JacksonUtils.prettyPrint(jsonExpectationList.get(i));
                if (jsonExpectationList.size() > 100) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Processing JSON expectation {} of {}: {}", i + 1, jsonExpectationList.size(), jsonExpectation);
                    } else if (LOG.isInfoEnabled()) {
                        LOG.info("Processing JSON expectation {} of {}", i + 1, jsonExpectationList.size());
                    }
                }
                try {
                    expectations.addAll(expectationModifier.apply(jsonExpectation, Collections.singletonList(deserialize(jsonExpectation))));
                } catch (IllegalArgumentException iae) {
                    validationErrorsList.add(iae.getMessage());
                }
            }
            if (!validationErrorsList.isEmpty()) {
                if (validationErrorsList.size() > 1) {
                    throw new IllegalArgumentException(("[" + NEW_LINE
                        + String.join("," + NEW_LINE + NEW_LINE, validationErrorsList))
                        .replaceAll(NEW_LINE, NEW_LINE + "  ")
                        + NEW_LINE + "]");
                } else {
                    throw new IllegalArgumentException(validationErrorsList.get(0));
                }
            }
        } else if (!allowEmpty) {
            throw new IllegalArgumentException("1 error:" + NEW_LINE + " - an expectation or array of expectations is required");
        }
        return expectations.toArray(new Expectation[0]);
    }

}
