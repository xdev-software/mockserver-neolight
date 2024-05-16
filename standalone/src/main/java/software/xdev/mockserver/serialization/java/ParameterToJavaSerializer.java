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
package software.xdev.mockserver.serialization.java;

import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.Parameter;

import java.util.Arrays;
import java.util.List;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class ParameterToJavaSerializer implements MultiValueToJavaSerializer<Parameter> {
    @Override
    public String serialize(int numberOfSpacesToIndent, Parameter parameter) {
        StringBuilder output = new StringBuilder();
        output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent * INDENT_SIZE));
        String serializedKey = NottableStringToJavaSerializer.serialize(parameter.getName(), false);
        output.append("new Parameter(").append(serializedKey);
        for (NottableString value : parameter.getValues()) {
            output.append(", ").append(NottableStringToJavaSerializer.serialize(value, serializedKey.endsWith(")")));
        }
        output.append(")");
        return output.toString();
    }

    @Override
    public String serializeAsJava(int numberOfSpacesToIndent, List<Parameter> parameters) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            output.append(serialize(numberOfSpacesToIndent, parameters.get(i)));
            if (i < (parameters.size() - 1)) {
                output.append(",");
            }
        }
        return output.toString();
    }

    @Override
    public String serializeAsJava(int numberOfSpacesToIndent, Parameter... object) {
        return serializeAsJava(numberOfSpacesToIndent, Arrays.asList(object));
    }
}
