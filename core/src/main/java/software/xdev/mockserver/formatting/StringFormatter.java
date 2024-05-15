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
package software.xdev.mockserver.formatting;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.netty.buffer.ByteBufUtil;

import java.util.HashMap;
import java.util.Map;

import static software.xdev.mockserver.character.Character.NEW_LINE;

public class StringFormatter {

    private static final Map<Integer, String> INDENTS = new HashMap<>();
    private static final Splitter fixedLengthSplitter = Splitter.fixedLength(64);
    private static final Joiner newLineJoiner = Joiner.on(NEW_LINE);

    static {
        INDENTS.put(0, "");
        INDENTS.put(1, "  ");
        INDENTS.put(2, "    ");
        INDENTS.put(3, "      ");
        INDENTS.put(4, "        ");
    }

    public static StringBuilder[] indentAndToString(final Object... objects) {
        return indentAndToString(1, objects);
    }

    public static StringBuilder[] indentAndToString(final int indent, final Object... objects) {
        final StringBuilder[] indentedObjects = new StringBuilder[objects.length];
        for (int i = 0; i < objects.length; i++) {
            indentedObjects[i] =
                new StringBuilder(NEW_LINE)
                    .append(NEW_LINE)
                    .append(String.valueOf(objects[i]).replaceAll("(?m)^", INDENTS.get(indent)))
                    .append(NEW_LINE);
        }
        return indentedObjects;
    }

    public static String formatLogMessage(final String message, final Object... arguments) {
        return formatLogMessage(0, message, arguments);
    }

    public static String formatLogMessage(final int indent, final String message, final Object... arguments) {
        final StringBuilder logMessage = new StringBuilder();
        final StringBuilder[] formattedArguments = indentAndToString(indent + 1, arguments);
        final String[] messageParts = message.split("\\{}");
        for (int messagePartIndex = 0; messagePartIndex < messageParts.length; messagePartIndex++) {
            logMessage.append(INDENTS.get(indent)).append(messageParts[messagePartIndex]);
            if (formattedArguments.length > 0 &&
                formattedArguments.length > messagePartIndex) {
                logMessage.append(formattedArguments[messagePartIndex]);
            }
            if (messagePartIndex < messageParts.length - 1) {
                logMessage.append(NEW_LINE);
                if (!messageParts[messagePartIndex + 1].startsWith(" ")) {
                    logMessage.append(" ");
                }
            }
        }
        return logMessage.toString();
    }

    public static String formatLogMessage(final String[] messageParts, final Object... arguments) {
        final StringBuilder logMessage = new StringBuilder();
        final StringBuilder[] formattedArguments = indentAndToString(arguments);
        for (int messagePartIndex = 0; messagePartIndex < messageParts.length; messagePartIndex++) {
            logMessage.append(messageParts[messagePartIndex]);
            if (formattedArguments.length > 0 &&
                formattedArguments.length > messagePartIndex) {
                logMessage.append(formattedArguments[messagePartIndex]);
            }
        }
        return logMessage.toString();
    }

    public static String formatBytes(byte[] bytes) {
        return newLineJoiner.join(fixedLengthSplitter.split(ByteBufUtil.hexDump(bytes)));
    }
}
