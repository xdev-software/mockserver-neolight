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

import static software.xdev.mockserver.model.NottableOptionalString.optional;
import static software.xdev.mockserver.model.NottableSchemaString.schemaString;
import static software.xdev.mockserver.model.NottableString.string;

public class Cookie extends KeyAndValue {

    public Cookie(String name, String value) {
        super(name, value);
    }

    public Cookie(NottableString name, NottableString value) {
        super(name, value);
    }


    public Cookie(NottableString name, String value) {
        super(name, value);
    }

    public static Cookie cookie(String name, String value) {
        return new Cookie(name, value);
    }

    public static Cookie cookie(NottableString name, NottableString value) {
        return new Cookie(name, value);
    }

    public static Cookie cookie(NottableString name, String value) {
        return new Cookie(name, value);
    }

    public static Cookie schemaCookie(String name, String value) {
        return new Cookie(string(name), schemaString(value));
    }

    public static Cookie optionalCookie(String name, String value) {
        return new Cookie(optional(name), string(value));
    }

    public static Cookie optionalCookie(String name, NottableString value) {
        return new Cookie(optional(name), value);
    }
}
