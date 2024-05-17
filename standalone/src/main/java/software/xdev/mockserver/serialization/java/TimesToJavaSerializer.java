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

import software.xdev.mockserver.matchers.Times;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class TimesToJavaSerializer implements ToJavaSerializer<Times> {

    @Override
    public String serialize(int numberOfSpacesToIndent, Times times) {
        StringBuilder output = new StringBuilder();
        if (times != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output);
            if (times.isUnlimited()) {
                output.append("Times.unlimited()");
            } else if (times.getRemainingTimes() == 1) {
                output.append("Times.once()");
            } else {
                output.append("Times.exactly(").append(times.getRemainingTimes()).append(")");
            }
        }

        return output.toString();
    }

    private StringBuilder appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuilder output) {
        return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
    }
}
