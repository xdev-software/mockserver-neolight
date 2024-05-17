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

import org.apache.commons.text.StringEscapeUtils;
import software.xdev.mockserver.serialization.Base64Converter;
import software.xdev.mockserver.model.*;

import java.util.List;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class HttpResponseToJavaSerializer implements ToJavaSerializer<HttpResponse> {

    @Override
    public String serialize(int numberOfSpacesToIndent, HttpResponse httpResponse) {
        StringBuilder output = new StringBuilder();
        if (httpResponse != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("response()");
            if (httpResponse.getStatusCode() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
                    .append(".withStatusCode(").append(httpResponse.getStatusCode()).append(")");
            }
            if (httpResponse.getReasonPhrase() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
                    .append(".withReasonPhrase(\"").append(StringEscapeUtils.escapeJava(httpResponse.getReasonPhrase())).append("\")");
            }
            outputHeaders(numberOfSpacesToIndent + 1, output, httpResponse.getHeaderList());
            outputCookies(numberOfSpacesToIndent + 1, output, httpResponse.getCookieList());
            if (isNotBlank(httpResponse.getBodyAsString())) {
                if (httpResponse.getBody() instanceof BinaryBody body) {
                    appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                    output.append(".withBody(Base64Converter.base64StringToBytes(\"")
                        .append(Base64Converter.bytesToBase64String(body.getRawBytes()))
                        .append("\"))");
                } else {
                    appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
                        .append(".withBody(\"")
                        .append(StringEscapeUtils.escapeJava(httpResponse.getBodyAsString()))
                        .append("\")");
                }
            }
            if (httpResponse.getDelay() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
                    .append(".withDelay(")
                    .append(new DelayToJavaSerializer().serialize(0, httpResponse.getDelay())).append(")");
            }
            if (httpResponse.getConnectionOptions() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
                    .append(".withConnectionOptions(");
                output.append(new ConnectionOptionsToJavaSerializer().serialize(numberOfSpacesToIndent + 2, httpResponse.getConnectionOptions()));
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
                    .append(")");
            }
        }

        return output.toString();
    }

    private void outputCookies(int numberOfSpacesToIndent, StringBuilder output, List<Cookie> cookies) {
        if (!cookies.isEmpty()) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".withCookies(");
            appendObject(numberOfSpacesToIndent + 1, output, new CookieToJavaSerializer(), cookies);
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
        }
    }

    private void outputHeaders(int numberOfSpacesToIndent, StringBuilder output, List<Header> headers) {
        if (!headers.isEmpty()) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".withHeaders(");
            appendObject(numberOfSpacesToIndent + 1, output, new HeaderToJavaSerializer(), headers);
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
        }
    }

    private <T extends ObjectWithJsonToString> void appendObject(
        int numberOfSpacesToIndent,
        StringBuilder output,
        MultiValueToJavaSerializer<T> toJavaSerializer,
        List<T> objects) {
        output.append(toJavaSerializer.serializeAsJava(numberOfSpacesToIndent, objects));
    }

    private StringBuilder appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuilder output) {
        return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
    }
}
