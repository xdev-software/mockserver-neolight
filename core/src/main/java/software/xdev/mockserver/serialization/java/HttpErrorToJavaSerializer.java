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
import software.xdev.mockserver.serialization.Base64Converter;
import software.xdev.mockserver.model.HttpError;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class HttpErrorToJavaSerializer implements ToJavaSerializer<HttpError> {

    private final Base64Converter base64Converter = new Base64Converter();

    @Override
    public String serialize(int numberOfSpacesToIndent, HttpError httpError) {
        StringBuffer output = new StringBuffer();
        if (httpError != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("error()");
            if (httpError.getDelay() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withDelay(").append(new DelayToJavaSerializer().serialize(0, httpError.getDelay())).append(")");
            }
            if (httpError.getDropConnection() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withDropConnection(").append(httpError.getDropConnection()).append(")");
            }
            if (httpError.getResponseBytes() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withResponseBytes(new Base64Converter().base64StringToBytes(\"").append(base64Converter.bytesToBase64String(httpError.getResponseBytes())).append("\"))");
            }
        }
        return output.toString();
    }

    private StringBuffer appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuffer output) {
        return output.append(NEW_LINE).append(Strings.padStart("", numberOfSpacesToIndent, ' '));
    }
}