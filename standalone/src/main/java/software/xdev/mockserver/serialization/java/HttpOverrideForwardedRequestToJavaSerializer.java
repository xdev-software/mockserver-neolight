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

import software.xdev.mockserver.model.HttpOverrideForwardedRequest;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class HttpOverrideForwardedRequestToJavaSerializer implements ToJavaSerializer<HttpOverrideForwardedRequest> {

    @Override
    public String serialize(int numberOfSpacesToIndent, HttpOverrideForwardedRequest httpForward) {
        StringBuilder output = new StringBuilder();
        if (httpForward != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("forwardOverriddenRequest()");
            if (httpForward.getRequestOverride() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withRequestOverride(");
                output.append(new HttpRequestToJavaSerializer().serialize(numberOfSpacesToIndent + 2, httpForward.getRequestOverride()));
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
            if (httpForward.getRequestModifier() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withRequestModifier(");
                output.append(new HttpRequestModifierToJavaSerializer().serialize(numberOfSpacesToIndent + 2, httpForward.getRequestModifier()));
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
            if (httpForward.getResponseOverride() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withResponseOverride(");
                output.append(new HttpResponseToJavaSerializer().serialize(numberOfSpacesToIndent + 2, httpForward.getResponseOverride()));
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
            if (httpForward.getResponseModifier() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withResponseModifier(");
                output.append(new HttpResponseModifierToJavaSerializer().serialize(numberOfSpacesToIndent + 2, httpForward.getResponseModifier()));
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
            if (httpForward.getDelay() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withDelay(");
                output.append(new DelayToJavaSerializer().serialize(0, httpForward.getDelay()));
                output.append(")");
            }
        }
        return output.toString();
    }

    private StringBuilder appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuilder output) {
        return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
    }
}
