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
import software.xdev.mockserver.model.SocketAddress;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class SocketAddressToJavaSerializer implements ToJavaSerializer<SocketAddress> {

    @Override
    public String serialize(int numberOfSpacesToIndent, SocketAddress socketAddress) {
        StringBuffer output = new StringBuffer();
        if (socketAddress != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("new SocketAddress()");
            if (socketAddress.getHost() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withHost(\"").append(socketAddress.getHost()).append("\")");
            }
            if (socketAddress.getPort() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withPort(").append(socketAddress.getPort()).append(")");
            }
            if (socketAddress.getScheme() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withScheme(SocketAddress.Scheme.").append(socketAddress.getScheme()).append(")");
            }
        }
        return output.toString();
    }

    private StringBuffer appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuffer output) {
        return output.append(NEW_LINE).append(Strings.padStart("", numberOfSpacesToIndent, ' '));
    }
}