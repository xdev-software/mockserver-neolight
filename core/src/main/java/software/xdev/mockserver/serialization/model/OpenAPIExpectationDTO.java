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
package software.xdev.mockserver.serialization.model;

import software.xdev.mockserver.mock.OpenAPIExpectation;
import software.xdev.mockserver.model.ObjectWithJsonToString;

import java.util.Map;

public class OpenAPIExpectationDTO extends ObjectWithJsonToString {
    private String specUrlOrPayload;
    private Map<String, String> operationsAndResponses;

    public OpenAPIExpectationDTO(OpenAPIExpectation openAPIExpectation) {
        if (openAPIExpectation != null) {
            specUrlOrPayload = openAPIExpectation.getSpecUrlOrPayload();
            operationsAndResponses = openAPIExpectation.getOperationsAndResponses();
        }
    }

    public OpenAPIExpectationDTO() {
    }

    public OpenAPIExpectation buildObject() {
        return new OpenAPIExpectation()
            .withSpecUrlOrPayload(specUrlOrPayload)
            .withOperationsAndResponses(operationsAndResponses);
    }

    public String getSpecUrlOrPayload() {
        return specUrlOrPayload;
    }

    public OpenAPIExpectationDTO setSpecUrlOrPayload(String specUrlOrPayload) {
        this.specUrlOrPayload = specUrlOrPayload;
        return this;
    }

    public Map<String, String> getOperationsAndResponses() {
        return operationsAndResponses;
    }

    public OpenAPIExpectationDTO setOperationsAndResponses(Map<String, String> operationsAndResponses) {
        this.operationsAndResponses = operationsAndResponses;
        return this;
    }

}
