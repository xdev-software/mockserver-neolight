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

import software.xdev.mockserver.model.KeyAndValue;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;

public class KeyAndValueDTO extends ObjectWithReflectiveEqualsHashCodeToString {
    private NottableString name;
    private NottableString value;

    protected KeyAndValueDTO(KeyAndValue keyAndValue) {
        name = keyAndValue.getName();
        value = keyAndValue.getValue();
    }

    protected KeyAndValueDTO() {
    }

    public NottableString getName() {
        return name;
    }

    public NottableString getValue() {
        return value;
    }
}
