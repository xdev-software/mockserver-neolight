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

import software.xdev.mockserver.serialization.ObjectMapperFactory;

public abstract class ObjectWithJsonToString {

    private static final String ESCAPED_QUOTE = "\"";

    @Override
    public String toString() {
        try {
            String valueAsString = ObjectMapperFactory
                .createObjectMapper(true, false)
                .writeValueAsString(this);
            if (valueAsString.startsWith(ESCAPED_QUOTE) && valueAsString.endsWith(ESCAPED_QUOTE)) {
                valueAsString = valueAsString.substring(1, valueAsString.length() - 1);
            }
            return valueAsString;
        } catch (Exception e) {
            return super.toString();
        }
    }
}
