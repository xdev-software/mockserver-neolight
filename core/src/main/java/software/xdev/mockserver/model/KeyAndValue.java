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

import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.model.NottableString.string;

public class KeyAndValue extends ObjectWithJsonToString {
    private final NottableString name;
    private final NottableString value;
    private final int hashCode;

    public KeyAndValue(String name, String value) {
        this(string(name), string(isBlank(value) ? "" : value));
    }

    public KeyAndValue(NottableString name, String value) {
        this(name, string(isBlank(value) ? "" : value));
    }

    public KeyAndValue(NottableString name, NottableString value) {
        this.name = name;
        this.value = value;
        this.hashCode = Objects.hash(name, value);
    }

    public NottableString getName() {
        return name;
    }

    public NottableString getValue() {
        return value;
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
        KeyAndValue that = (KeyAndValue) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
