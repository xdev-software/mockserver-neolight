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

import java.util.Arrays;
import java.util.Collection;

import static software.xdev.mockserver.model.NottableOptionalString.optional;
import static software.xdev.mockserver.model.NottableString.string;

public class Header extends KeyToMultiValue {

    public Header(String name, String... value) {
        super(name, value);
    }

    public Header(NottableString name, NottableString... value) {
        super(name, value);
    }

    public Header(NottableString name, String... value) {
        super(name, value);
    }

    public Header(String name, Collection<String> value) {
        super(name, value);
    }

    public Header(NottableString name, Collection<NottableString> value) {
        super(name, value);
    }

    public static Header header(String name, int value) {
        return new Header(name, String.valueOf(value));
    }

    public static Header header(String name, String... value) {
        return new Header(name, value);
    }

    public static Header header(NottableString name, NottableString... value) {
        return new Header(name, value);
    }

    public static Header header(String name, Collection<String> value) {
        return new Header(name, value);
    }

    public static Header header(NottableString name, Collection<NottableString> value) {
        return new Header(name, value);
    }

    public static Header schemaHeader(String name, String... values) {
        if (values.length == 0) {
            values = new String[]{".*"};
        }
        return new Header(string(name), Arrays.stream(values).map(NottableSchemaString::schemaString).toArray(NottableString[]::new));
    }

    public static Header optionalHeader(String name, String... values) {
        if (values.length == 0) {
            values = new String[]{".*"};
        }
        return new Header(optional(name), Arrays.stream(values).map(NottableString::string).toArray(NottableString[]::new));
    }
}
