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

import software.xdev.mockserver.model.HttpClassCallback;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class HttpClassCallbackToJavaSerializer implements ToJavaSerializer<HttpClassCallback> {

    @Override
    public String serialize(int numberOfSpacesToIndent, HttpClassCallback httpClassCallback) {
        StringBuilder output = new StringBuilder();
        if (httpClassCallback != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("callback()");
            if (httpClassCallback.getCallbackClass() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withCallbackClass(\"").append(httpClassCallback.getCallbackClass()).append("\")");
            }
            if (httpClassCallback.getDelay() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withDelay(").append(new DelayToJavaSerializer().serialize(0, httpClassCallback.getDelay())).append(")");
            }
        }

        return output.toString();
    }

    private StringBuilder appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuilder output) {
        return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
    }
}
