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

import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.JsonSchemaBody;
import software.xdev.mockserver.model.ParameterStyle;

import java.util.Map;

public class JsonSchemaBodyDTO extends BodyDTO {

    private final String jsonSchema;
    private final Map<String, ParameterStyle> parameterStyles;

    public JsonSchemaBodyDTO(JsonSchemaBody jsonSchemaBody) {
        this(jsonSchemaBody, null);
    }

    public JsonSchemaBodyDTO(JsonSchemaBody jsonSchemaBody, Boolean not) {
        super(Body.Type.JSON_SCHEMA, not);
        this.jsonSchema = jsonSchemaBody.getValue();
        this.parameterStyles = jsonSchemaBody.getParameterStyles();
        withOptional(jsonSchemaBody.getOptional());
    }

    public String getJson() {
        return jsonSchema;
    }

    public Map<String, ParameterStyle> getParameterStyles() {
        return parameterStyles;
    }

    public JsonSchemaBody buildObject() {
        return (JsonSchemaBody) new JsonSchemaBody(getJson()).withParameterStyles(parameterStyles).withOptional(getOptional());
    }
}
