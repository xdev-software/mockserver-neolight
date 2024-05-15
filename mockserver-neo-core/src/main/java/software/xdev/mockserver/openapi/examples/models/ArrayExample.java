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

import java.util.ArrayList;
import java.util.List;

/**
 * See: https://github.com/swagger-api/swagger-inflector
 */
@JsonDeserialize(using = JsonExampleDeserializer.class)
public class ArrayExample extends AbstractExample {
    List<Example> values = null;

    public ArrayExample() {
        super.setTypeName("array");
    }

    public void add(Example value) {
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(value);
    }

    public String asString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                Example example = values.get(i);
                builder.append(example.asString());
                if (i > 0) {
                    builder.append(",");
                }
            }
        }
        builder.append("]");

        return builder.toString();
    }

    public List<Example> getItems() {
        if (values == null) {
            return new ArrayList<>();
        }
        return values;
    }
}
