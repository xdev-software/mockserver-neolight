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

import software.xdev.mockserver.file.FileReader;

import java.util.Map;
import java.util.Objects;

public class JsonSchemaBody extends Body<String> {
    private int hashCode;
    private final String jsonSchema;
    private Map<String, ParameterStyle> parameterStyles;

    public JsonSchemaBody(String jsonSchema) {
        super(Type.JSON_SCHEMA);
        this.jsonSchema = jsonSchema;
    }

    public static JsonSchemaBody jsonSchema(String jsonSchema) {
        return new JsonSchemaBody(jsonSchema);
    }

    public static JsonSchemaBody jsonSchemaFromResource(String jsonSchemaPath) {
        return new JsonSchemaBody(FileReader.readFileFromClassPathOrPath(jsonSchemaPath));
    }

    public Map<String, ParameterStyle> getParameterStyles() {
        return parameterStyles;
    }

    public JsonSchemaBody withParameterStyles(Map<String, ParameterStyle> parameterStyles) {
        this.parameterStyles = parameterStyles;
        return this;
    }

    public String getValue() {
        return jsonSchema;
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
        JsonSchemaBody that = (JsonSchemaBody) o;
        return Objects.equals(jsonSchema, that.jsonSchema);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), jsonSchema);
        }
        return hashCode;
    }
}
