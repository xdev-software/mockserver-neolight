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
package software.xdev.mockserver.validator.jsonschema;

import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.RequestDefinition;

public class JsonSchemaRequestDefinitionValidator extends JsonSchemaValidator {

    private JsonSchemaRequestDefinitionValidator(MockServerLogger mockServerLogger) {
        super(
            mockServerLogger,
            RequestDefinition.class,
			"software/xdev/mockserver/schema/",
            "requestDefinition",
            "httpRequest",
            "stringOrJsonSchema",
            "openAPIDefinition",
            "body",
            "keyToMultiValue",
            "keyToValue",
            "socketAddress",
            "protocol",
            "openAPIDefinition",
            "draft-07"
        );
    }

    private static JsonSchemaRequestDefinitionValidator jsonSchemaHttpRequestValidator;

    public static JsonSchemaRequestDefinitionValidator jsonSchemaRequestDefinitionValidator(MockServerLogger mockServerLogger) {
        if (jsonSchemaHttpRequestValidator == null) {
            jsonSchemaHttpRequestValidator = new JsonSchemaRequestDefinitionValidator(mockServerLogger);
        }
        return jsonSchemaHttpRequestValidator;
    }
}
