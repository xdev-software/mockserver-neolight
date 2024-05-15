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
package software.xdev.mockserver.model;

import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class OpenAPIDefinition extends RequestDefinition {
    private int hashCode;
    private String specUrlOrPayload;
    private String operationId;

    public static OpenAPIDefinition openAPI() {
        return new OpenAPIDefinition();
    }

    /**
     * Specify the OpenAPI to match against by URL or payload and string as follows:
     * <p>
     * // Create from a publicly hosted HTTP location (json or yaml)
     * openAPI("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v3.0/petstore-expanded.yaml")
     * <p>
     * // Create from a file on the local filesystem (json or yaml)
     * openAPI("file://Users/myuser/git/mockserver/mockserver-core/src/test/resources/org/mockserver/openapi/openapi_petstore_example.json");
     * <p>
     * // Create from a classpath resource in the /api package (json or yaml)
     * openAPI("org/mockserver/openapi/openapi_petstore_example.json");
     * <p>
     * // Create from an OpenAPI payload (json or yaml)
     * openAPI("{\"openapi\": \"3.0.0\", \"info\": { ...")
     * <p>
     *
     * @param specUrlOrPayload the OpenAPI to match against by URL or payload
     * @return the OpenAPIMatcher
     */
    public static OpenAPIDefinition openAPI(String specUrlOrPayload) {
        return new OpenAPIDefinition()
            .withSpecUrlOrPayload(specUrlOrPayload);
    }

    /**
     * Specify the OpenAPI and operationId to match against by URL or payload and string as follows:
     * <p>
     * // Create from a publicly hosted HTTP location (json or yaml)
     * openAPI("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v3.0/petstore-expanded.yaml", "showPetById")
     * <p>
     * // Create from a file on the local filesystem (json or yaml)
     * openAPI("file://Users/myuser/git/mockserver/mockserver-core/src/test/resources/org/mockserver/openapi/openapi_petstore_example.json", "showPetById");
     * <p>
     * // Create from a classpath resource in the /api package (json or yaml)
     * openAPI("org/mockserver/openapi/openapi_petstore_example.json", "showPetById");
     * <p>
     * // Create from an OpenAPI payload (json or yaml)
     * openAPI("{\"openapi\": \"3.0.0\", \"info\": { ...", "showPetById")
     * <p>
     *
     * @param specUrlOrPayload the OpenAPI to match against by URL or payload
     * @param operationId      operationId from the OpenAPI to match against i.e. "showPetById"
     * @return the OpenAPIMatcher
     */
    public static OpenAPIDefinition openAPI(String specUrlOrPayload, String operationId) {
        return new OpenAPIDefinition()
            .withSpecUrlOrPayload(specUrlOrPayload)
            .withOperationId(operationId);
    }

    public String getSpecUrlOrPayload() {
        return specUrlOrPayload;
    }

    /**
     * Specify the OpenAPI to match against by URL or payload as follows:
     * <p>
     * // Create from a publicly hosted HTTP location (json or yaml)
     * withSpecUrlOrPayload("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v3.0/petstore-expanded.yaml")
     * <p>
     * // Create from a file on the local filesystem (json or yaml)
     * withSpecUrlOrPayload("file://Users/myuser/git/mockserver/mockserver-core/src/test/resources/org/mockserver/openapi/openapi_petstore_example.json");
     * <p>
     * // Create from a classpath resource in the /api package (json or yaml)
     * withSpecUrlOrPayload("org/mockserver/openapi/openapi_petstore_example.json");
     * <p>
     * // Create from an OpenAPI payload (json or yaml)
     * withSpecUrlOrPayload("{\"openapi\": \"3.0.0\", \"info\": { ...")
     * <p>
     *
     * @param specUrlOrPayload the OpenAPI to match against by URL or payload
     * @return the OpenAPIMatcher
     */
    public OpenAPIDefinition withSpecUrlOrPayload(String specUrlOrPayload) {
        this.specUrlOrPayload = specUrlOrPayload;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    /**
     * Specify operationId from the OpenAPI to match against i.e. "showPetById"
     *
     * @param operationId operationId from the OpenAPI to match against i.e. "showPetById"
     * @return the OpenAPIMatcher
     */
    public OpenAPIDefinition withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public OpenAPIDefinition shallowClone() {
        return not(openAPI(), not)
            .withSpecUrlOrPayload(specUrlOrPayload)
            .withOperationId(operationId);
    }

    public OpenAPIDefinition update(OpenAPIDefinition replaceRequest) {
        if (replaceRequest.getSpecUrlOrPayload() != null && isNotBlank(replaceRequest.getSpecUrlOrPayload())) {
            withSpecUrlOrPayload(replaceRequest.getSpecUrlOrPayload());
        }
        if (replaceRequest.getOperationId() != null && isNotBlank(replaceRequest.getOperationId())) {
            withOperationId(replaceRequest.getOperationId());
        }
        this.hashCode = 0;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (hashCode() != o.hashCode()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        OpenAPIDefinition that = (OpenAPIDefinition) o;
        return Objects.equals(specUrlOrPayload, that.specUrlOrPayload) &&
            Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), specUrlOrPayload, operationId);
        }
        return hashCode;
    }
}
