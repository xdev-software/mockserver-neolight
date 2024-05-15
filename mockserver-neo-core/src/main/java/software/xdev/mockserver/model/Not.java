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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class Not extends ObjectWithJsonToString {
    private int hashCode;
    Boolean not;

    public static <T extends Not> T not(T t) {
        t.not = true;
        return t;
    }

    public static <T extends Not> T not(T t, Boolean not) {
        if (not != null && not) {
            t.not = true;
        }
        return t;
    }

    @JsonIgnore
    public boolean isNot() {
        return not != null && not;
    }

    public Boolean getNot() {
        return not;
    }

    public void setNot(Boolean not) {
        this.not = not;
        this.hashCode = 0;
    }

    public Not withNot(Boolean not) {
        this.not = not;
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
        Not not1 = (Not) o;
        return Objects.equals(not, not1.not);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(not);
        }
        return hashCode;
    }
}
