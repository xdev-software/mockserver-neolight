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
package software.xdev.mockserver.openapi.examples.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import software.xdev.mockserver.openapi.examples.JsonExampleDeserializer;

/**
 * See: https://github.com/swagger-api/swagger-inflector
 */
@JsonDeserialize(using = JsonExampleDeserializer.class)
public class StringExample extends AbstractExample {
    private String value;

    public StringExample() {
        super.setTypeName("string");
    }

    public StringExample(String value) {
        this();
        this.value = value;
    }

    public String textValue() {
        return value;
    }

    public String asString() {
        return value != null ? value : "null";
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
