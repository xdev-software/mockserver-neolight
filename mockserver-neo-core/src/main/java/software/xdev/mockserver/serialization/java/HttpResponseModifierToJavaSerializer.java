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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpResponseModifier;
import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;
import software.xdev.mockserver.serialization.Base64Converter;

import java.util.List;
import java.util.stream.Collectors;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class HttpResponseModifierToJavaSerializer implements ToJavaSerializer<HttpResponseModifier> {

    private final Base64Converter base64Converter = new Base64Converter();

    public String serialize(List<HttpResponseModifier> httpResponseModifiers) {
        StringBuilder output = new StringBuilder();
        for (HttpResponseModifier httpResponseModifier : httpResponseModifiers) {
            output.append(serialize(0, httpResponseModifier));
            output.append(";");
            output.append(NEW_LINE);
        }
        return output.toString();
    }

    @Override
    public String serialize(int numberOfSpacesToIndent, HttpResponseModifier response) {
        StringBuffer output = new StringBuffer();
        if (response != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output);
            output.append("responseModifier()");
            if (response.getHeaders() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withHeaders(");
                outputHeaders(numberOfSpacesToIndent, output, response.getHeaders().getAdd());
                outputHeaders(numberOfSpacesToIndent, output, response.getHeaders().getReplace());
                outputList(numberOfSpacesToIndent, output, response.getHeaders().getRemove());
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
            if (response.getCookies() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withCookies(");
                outputCookies(numberOfSpacesToIndent, output, response.getCookies().getAdd());
                outputCookies(numberOfSpacesToIndent, output, response.getCookies().getReplace());
                outputList(numberOfSpacesToIndent, output, response.getCookies().getRemove());
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(")");
            }
        }

        return output.toString();
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
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("ImmutableList.of(").append(Joiner.on(",").join(add.stream().map(s -> "\"" + s + "\"").collect(Collectors.toList()))).append(")");
        } else {
            appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output).append("null");
        }
    }

    private <T extends ObjectWithReflectiveEqualsHashCodeToString> void appendObject(int numberOfSpacesToIndent, StringBuffer output, MultiValueToJavaSerializer<T> toJavaSerializer, List<T> objects) {
        output.append(toJavaSerializer.serializeAsJava(numberOfSpacesToIndent + 1, objects));
    }

    private StringBuffer appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuffer output) {
        return output.append(NEW_LINE).append(Strings.padStart("", numberOfSpacesToIndent, ' '));
    }
}
