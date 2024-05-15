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

public class JsonPathBody extends Body<String> {
    private int hashCode;
    private final String jsonPath;

    public JsonPathBody(String jsonPath) {
        super(Type.JSON_PATH);
        this.jsonPath = jsonPath;
    }

    public static JsonPathBody jsonPath(String jsonPath) {
        return new JsonPathBody(jsonPath);
    }

    public String getValue() {
        return jsonPath;
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
        JsonPathBody that = (JsonPathBody) o;
        return Objects.equals(jsonPath, that.jsonPath);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), jsonPath);
        }
        return hashCode;
    }
}
