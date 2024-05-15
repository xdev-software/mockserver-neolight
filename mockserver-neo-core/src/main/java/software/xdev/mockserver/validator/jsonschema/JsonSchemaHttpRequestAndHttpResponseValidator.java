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
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;

public class JsonSchemaHttpRequestAndHttpResponseValidator extends JsonSchemaValidator {

    private JsonSchemaHttpRequestAndHttpResponseValidator(MockServerLogger mockServerLogger) {
        super(
            mockServerLogger,
            HttpRequestAndHttpResponse.class,
			"software/xdev/mockserver/schema/",
            "httpRequestAndHttpResponse",
            "requestDefinition",
            "openAPIDefinition",
            "httpRequest",
            "stringOrJsonSchema",
            "body",
            "keyToMultiValue",
            "keyToValue",
            "socketAddress",
            "protocol",
            "httpResponse",
            "bodyWithContentType",
            "delay",
            "connectionOptions",
            "draft-07"
        );
    }

    private static JsonSchemaHttpRequestAndHttpResponseValidator jsonSchemaHttpRequestValidator;

    public static JsonSchemaHttpRequestAndHttpResponseValidator jsonSchemaHttpRequestAndHttpResponseValidator(MockServerLogger mockServerLogger) {
        if (jsonSchemaHttpRequestValidator == null) {
            jsonSchemaHttpRequestValidator = new JsonSchemaHttpRequestAndHttpResponseValidator(mockServerLogger);
        }
        return jsonSchemaHttpRequestValidator;
    }
}
