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

import com.google.common.base.Strings;
import software.xdev.mockserver.model.OpenAPIDefinition;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class OpenAPIMatcherToJavaSerializer implements ToJavaSerializer<OpenAPIDefinition> {

    @Override
    public String serialize(int numberOfSpacesToIndent, OpenAPIDefinition OpenAPIMatcher) {
        StringBuffer output = new StringBuffer();
        if (OpenAPIMatcher != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("openAPIMatcher()");
            if (OpenAPIMatcher.getSpecUrlOrPayload() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withSpecUrlOrPayload(\"").append(OpenAPIMatcher.getSpecUrlOrPayload()).append("\")");
            }
            if (OpenAPIMatcher.getOperationId() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withOperationId(").append(OpenAPIMatcher.getOperationId()).append(")");
            }
        }
        return output.toString();
    }

    private StringBuffer appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuffer output) {
        return output.append(NEW_LINE).append(Strings.padStart("", numberOfSpacesToIndent, ' '));
    }
}
