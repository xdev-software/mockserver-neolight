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

import java.util.Collection;
import java.util.List;
import java.util.Map;


public class Parameters extends KeysToMultiValues<Parameter, Parameters> {

    private String rawParameterString;

    public Parameters(List<Parameter> parameters) {
        withEntries(parameters);
    }

    public Parameters(Parameter... parameters) {
        withEntries(parameters);
    }

    public Parameters(Map<NottableString, List<NottableString>> headers) {
        super(headers);
    }

    public static Parameters parameters(Parameter... parameters) {
        return new Parameters(parameters);
    }

    @Override
    public Parameter build(NottableString name, Collection<NottableString> values) {
        return new Parameter(name, values);
    }

    protected void isModified() {
        rawParameterString = null;
    }

    public Parameters withKeyMatchStyle(KeyMatchStyle keyMatchStyle) {
        super.withKeyMatchStyle(keyMatchStyle);
        return this;
    }

    public String getRawParameterString() {
        return rawParameterString;
    }

    public Parameters withRawParameterString(String rawParameterString) {
        this.rawParameterString = rawParameterString;
        return this;
    }

    public Parameters clone() {
        return new Parameters(getMultimap())
            .withRawParameterString(rawParameterString);
    }

}
