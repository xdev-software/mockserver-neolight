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
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.OpenAPIDefinition;
import software.xdev.mockserver.model.RequestDefinition;

import java.util.List;

import static software.xdev.mockserver.character.Character.NEW_LINE;

public class ExpectationToJavaSerializer implements ToJavaSerializer<Expectation> {

    public static final int INDENT_SIZE = 8;

    public String serialize(List<Expectation> expectations) {
        StringBuilder output = new StringBuilder();
        for (Expectation expectation : expectations) {
            output.append(serialize(0, expectation));
            output.append(NEW_LINE);
            output.append(NEW_LINE);
        }
        return output.toString();
    }

    @Override
    public String serialize(int numberOfSpacesToIndent, Expectation expectation) {
        StringBuffer output = new StringBuffer();
        if (expectation != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("new MockServerClient(\"localhost\", 1080)");
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".when(");
            RequestDefinition requestDefinition = expectation.getHttpRequest();
            if (requestDefinition instanceof HttpRequest) {
                output.append(new HttpRequestToJavaSerializer().serialize(numberOfSpacesToIndent + 1, (HttpRequest) requestDefinition));
            } else if (requestDefinition instanceof OpenAPIDefinition) {
                output.append(new OpenAPIMatcherToJavaSerializer().serialize(numberOfSpacesToIndent + 1, (OpenAPIDefinition) requestDefinition));
            }
            output.append(",");
            if (expectation.getTimes() != null) {
                output.append(new TimesToJavaSerializer().serialize(numberOfSpacesToIndent + 1, expectation.getTimes()));
            } else {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append("null");
            }
            output.append(",");
            if (expectation.getTimeToLive() != null) {
                output.append(new TimeToLiveToJavaSerializer().serialize(numberOfSpacesToIndent + 1, expectation.getTimeToLive()));
            } else {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append("null");
            }
            output.append(",");
            appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(expectation.getPriority());
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
            if (expectation.getHttpResponse() != null) {
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".respond(");
                output.append(new HttpResponseToJavaSerializer().serialize(numberOfSpacesToIndent + 1, expectation.getHttpResponse()));
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
            }
            if (expectation.getHttpResponseClassCallback() != null) {
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".respond(");
                output.append(new HttpClassCallbackToJavaSerializer().serialize(numberOfSpacesToIndent + 1, expectation.getHttpResponseClassCallback()));
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
            }
            if (expectation.getHttpResponseObjectCallback() != null) {
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("/*NOT POSSIBLE TO GENERATE CODE FOR OBJECT CALLBACK*/");
            }
            if (expectation.getHttpForward() != null) {
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".forward(");
                output.append(new HttpForwardToJavaSerializer().serialize(numberOfSpacesToIndent + 1, expectation.getHttpForward()));
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
            }
            if (expectation.getHttpOverrideForwardedRequest() != null) {
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".forward(");
                output.append(new HttpOverrideForwardedRequestToJavaSerializer().serialize(numberOfSpacesToIndent + 1, expectation.getHttpOverrideForwardedRequest()));
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
            }
            if (expectation.getHttpForwardClassCallback() != null) {
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".forward(");
                output.append(new HttpClassCallbackToJavaSerializer().serialize(numberOfSpacesToIndent + 1, expectation.getHttpForwardClassCallback()));
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
            }
            if (expectation.getHttpForwardObjectCallback() != null) {
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("/*NOT POSSIBLE TO GENERATE CODE FOR OBJECT CALLBACK*/");
            }
            if (expectation.getHttpError() != null) {
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(".error(");
                output.append(new HttpErrorToJavaSerializer().serialize(numberOfSpacesToIndent + 1, expectation.getHttpError()));
                appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append(")");
            }
            output.append(";");
        }
        return output.toString();
    }

    private StringBuffer appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuffer output) {
        return output.append(NEW_LINE).append(Strings.padStart("", numberOfSpacesToIndent, ' '));
    }
}
