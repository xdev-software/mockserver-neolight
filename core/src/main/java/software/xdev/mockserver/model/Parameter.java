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

public class Parameter extends KeyToMultiValue {

    public Parameter(String name, String... value) {
        super(name, value);
    }

    public Parameter(NottableString name, NottableString... value) {
        super(name, value);
    }

    public Parameter(NottableString name, String... value) {
        super(name, value);
    }

    public Parameter(String name, Collection<String> value) {
        super(name, value);
    }

    public Parameter(NottableString name, Collection<NottableString> value) {
        super(name, value);
    }

    public static Parameter param(String name, String... value) {
        return new Parameter(name, value);
    }

    public static Parameter param(NottableString name, NottableString... value) {
        return new Parameter(name, value);
    }

    public static Parameter param(NottableString name, String... value) {
        return new Parameter(name, value);
    }

    public static Parameter param(String name, Collection<String> value) {
        return new Parameter(name, value);
    }

    public static Parameter param(NottableString name, Collection<NottableString> value) {
        return new Parameter(name, value);
    }

    public static Parameter optionalParam(String name, String... values) {
        if (values.length == 0) {
            values = new String[]{".*"};
        }
        return new Parameter(optional(name), Arrays.stream(values).map(NottableString::string).toArray(NottableString[]::new));
    }

    public Parameter withStyle(ParameterStyle style) {
        getName().withStyle(style);
        return this;
    }

}
