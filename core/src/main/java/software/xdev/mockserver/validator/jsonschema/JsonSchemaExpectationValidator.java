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
import software.xdev.mockserver.mock.Expectation;

public class JsonSchemaExpectationValidator extends JsonSchemaValidator {

    private JsonSchemaExpectationValidator(MockServerLogger mockServerLogger) {
        super(
            mockServerLogger,
            Expectation.class,
			"software/xdev/mockserver/schema/",
            "expectation",
            "requestDefinition",
            "openAPIDefinition",
            "httpRequest",
            "httpResponse",
            "httpTemplate",
            "httpForward",
            "httpClassCallback",
            "httpObjectCallback",
            "httpOverrideForwardedRequest",
            "httpError",
            "times",
            "timeToLive",
            "stringOrJsonSchema",
            "body",
            "bodyWithContentType",
            "delay",
            "connectionOptions",
            "keyToMultiValue",
            "keyToValue",
            "socketAddress",
            "protocol",
            "draft-07"
        );
    }

    private static JsonSchemaExpectationValidator jsonSchemaExpectationValidator;

    public static JsonSchemaExpectationValidator jsonSchemaExpectationValidator(MockServerLogger mockServerLogger) {
        if (jsonSchemaExpectationValidator == null) {
            jsonSchemaExpectationValidator = new JsonSchemaExpectationValidator(mockServerLogger);
        }
        return jsonSchemaExpectationValidator;
    }
}