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

import software.xdev.mockserver.model.*;

import java.util.List;
import java.util.stream.Collectors;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class HttpRequestModifierToJavaSerializer implements ToJavaSerializer<HttpRequestModifier> {

    public String serialize(List<HttpRequestModifier> httpRequestModifiers) {
        StringBuilder output = new StringBuilder();
        for (HttpRequestModifier httpRequestModifier : httpRequestModifiers) {
            output.append(serialize(0, httpRequestModifier));
            output.append(";");
            output.append(NEW_LINE);
        }
        return output.toString();
    }

    @Override
    public String serialize(int numberOfSpacesToIndent, HttpRequestModifier request) {
        StringBuffer output = new StringBuffer();
        if (request != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output);
            output.append("requestModifier()");
            if (request.getPath() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                output.append(".withPath(\"").append(request.getPath().getRegex()).append("\",\"").append(request.getPath().getSubstitution()).append("\")");
            }
            if (request.getQueryStringParameters() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withQueryStringParameters(");
                outputQueryStringParameters(numberOfSpacesToIndent, output, request.getQueryStringParameters().getAdd());
                outputQueryStringParameters(numberOfSpacesToIndent, output, request.getQueryStringParameters().getReplace());
                outputList(numberOfSpacesToIndent, output, request.getQueryStringParameters().getRemove());
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
            if (request.getHeaders() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withHeaders(");
                outputHeaders(numberOfSpacesToIndent, output, request.getHeaders().getAdd());
                outputHeaders(numberOfSpacesToIndent, output, request.getHeaders().getReplace());
                outputList(numberOfSpacesToIndent, output, request.getHeaders().getRemove());
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
            if (request.getCookies() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withCookies(");
                outputCookies(numberOfSpacesToIndent, output, request.getCookies().getAdd());
                outputCookies(numberOfSpacesToIndent, output, request.getCookies().getReplace());
                outputList(numberOfSpacesToIndent, output, request.getCookies().getRemove());
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
        }

        return output.toString();
    }

    private void outputQueryStringParameters(int numberOfSpacesToIndent, StringBuffer output, Parameters parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("parameters(");
            appendObject((numberOfSpacesToIndent + 2), output, new ParameterToJavaSerializer(), parameters.getEntries());
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("),");
        } else {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null,");
        }
    }

    private void outputHeaders(int numberOfSpacesToIndent, StringBuffer output, Headers headers) {
        if (headers != null && !headers.isEmpty()) {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("headers(");
            appendObject((numberOfSpacesToIndent + 2), output, new HeaderToJavaSerializer(), headers.getEntries());
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("),");
        } else {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null,");
        }
    }

    private void outputCookies(int numberOfSpacesToIndent, StringBuffer output, Cookies cookies) {
        if (cookies != null && !cookies.isEmpty()) {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("cookies(");
            appendObject((numberOfSpacesToIndent + 2), output, new CookieToJavaSerializer(), cookies.getEntries());
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("),");
        } else {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null,");
        }
    }

    private void outputList(int numberOfSpacesToIndent, StringBuffer output, List<String> add) {
        if (add != null && !add.isEmpty()) {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output)
                .append("List.of(")
                .append(add.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")))
                .append(")");
        } else {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null");
        }
    }

    private <T extends ObjectWithReflectiveEqualsHashCodeToString> void appendObject(int numberOfSpacesToIndent, StringBuffer output, MultiValueToJavaSerializer<T> toJavaSerializer, List<T> objects) {
        output.append(toJavaSerializer.serializeAsJava(numberOfSpacesToIndent + 1, objects));
    }

    private StringBuffer appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuffer output) {
        return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
    }
}
