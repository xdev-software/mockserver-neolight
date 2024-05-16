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
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.serialization.Base64Converter;

import java.util.List;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class HttpRequestToJavaSerializer implements ToJavaSerializer<HttpRequest> {

    private final Base64Converter base64Converter = new Base64Converter();

    public String serialize(List<HttpRequest> httpRequests) {
        StringBuilder output = new StringBuilder();
        for (HttpRequest httpRequest : httpRequests) {
            output.append(serialize(0, httpRequest));
            output.append(";");
            output.append(NEW_LINE);
        }
        return output.toString();
    }

    @Override
    public String serialize(int numberOfSpacesToIndent, HttpRequest request) {
        StringBuffer output = new StringBuffer();
        if (request != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output);
            output.append("request()");
            if (isNotBlank(request.getMethod().getValue())) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                output.append(".withMethod(\"").append(request.getMethod().getValue()).append("\")");
            }
            if (isNotBlank(request.getPath().getValue())) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                output.append(".withPath(\"").append(request.getPath().getValue()).append("\")");
            }
            outputHeaders(numberOfSpacesToIndent + 1, output, request.getHeaderList());
            outputCookies(numberOfSpacesToIndent + 1, output, request.getCookieList());
            outputQueryStringParameter(numberOfSpacesToIndent + 1, output, request.getQueryStringParameterList());
            if (request.isKeepAlive() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                output.append(".withKeepAlive(").append(request.isKeepAlive().toString()).append(")");
            }
            if (request.getProtocol() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                output.append(".withProtocol(Protocol.").append(request.getProtocol().toString()).append(")");
            }
            if (request.getSocketAddress() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                output.append(".withSocketAddress(");
                output.append(new SocketAddressToJavaSerializer().serialize(numberOfSpacesToIndent + 2, request.getSocketAddress()));
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                output.append(")");
            }
            if (request.getBody() != null) {
                if (request.getBody() instanceof RegexBody) {
                    appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                    output.append(".withBody(");
                    RegexBody regexBody = (RegexBody) request.getBody();
                    output.append("new RegexBody(\"").append(StringEscapeUtils.escapeJava(regexBody.getValue())).append("\")");
                    output.append(")");
                } else if (request.getBody() instanceof StringBody) {
                    appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                    output.append(".withBody(");
                    StringBody stringBody = (StringBody) request.getBody();
                    output.append("new StringBody(\"").append(StringEscapeUtils.escapeJava(stringBody.getValue())).append("\")");
                    output.append(")");
                } else if (request.getBody() instanceof ParameterBody) {
                    appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                    output.append(".withBody(");
                    appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output);
                    output.append("new ParameterBody(");
                    List<Parameter> bodyParameters = ((ParameterBody) request.getBody()).getValue().getEntries();
                    output.append(new ParameterToJavaSerializer().serializeAsJava(numberOfSpacesToIndent + 3, bodyParameters));
                    appendNewLineAndIndent((numberOfSpacesToIndent + 2) * INDENT_SIZE, output);
                    output.append(")");
                    appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                    output.append(")");
                } else if (request.getBody() instanceof BinaryBody) {
                    appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output);
                    BinaryBody body = (BinaryBody) request.getBody();
                    output.append(".withBody(new Base64Converter().base64StringToBytes(\"").append(base64Converter.bytesToBase64String(body.getRawBytes())).append("\"))");
                }
            }
        }

        return output.toString();
    }

    private void outputQueryStringParameter(int numberOfSpacesToIndent, StringBuffer output, List<Parameter> parameters) {
        if (!parameters.isEmpty()) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".withQueryStringParameters(");
            appendObject(numberOfSpacesToIndent, output, new ParameterToJavaSerializer(), parameters);
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
        }
    }

    private void outputCookies(int numberOfSpacesToIndent, StringBuffer output, List<Cookie> cookies) {
        if (!cookies.isEmpty()) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".withCookies(");
            appendObject(numberOfSpacesToIndent, output, new CookieToJavaSerializer(), cookies);
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
        }
    }

    private void outputHeaders(int numberOfSpacesToIndent, StringBuffer output, List<Header> headers) {
        if (!headers.isEmpty()) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".withHeaders(");
            appendObject(numberOfSpacesToIndent, output, new HeaderToJavaSerializer(), headers);
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
        }
    }

    private <T extends ObjectWithReflectiveEqualsHashCodeToString> void appendObject(int numberOfSpacesToIndent, StringBuffer output, MultiValueToJavaSerializer<T> toJavaSerializer, List<T> objects) {
        output.append(toJavaSerializer.serializeAsJava(numberOfSpacesToIndent + 1, objects));
    }

    private StringBuffer appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuffer output) {
        return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
    }
}
