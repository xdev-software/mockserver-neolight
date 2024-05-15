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

public class NottableOptionalString extends NottableString {

    public static final char OPTIONAL_CHAR = '?';

    public static NottableOptionalString optional(String value, Boolean not) {
        return new NottableOptionalString(value, not);
    }

    public static NottableOptionalString optional(String value) {
        return new NottableOptionalString(value);
    }

    /**
     * @deprecated use `optional` instead
     */
    @Deprecated
    public static NottableOptionalString optionalString(String value) {
        return optional(value);
    }

    public static NottableOptionalString notOptional(String value) {
        return new NottableOptionalString(value, Boolean.TRUE);
    }

    private NottableOptionalString(String schema, Boolean not) {
        super(schema, not);
    }

    private NottableOptionalString(String schema) {
        super(schema);
    }

    @Override
    public boolean isOptional() {
        return true;
    }

}
