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
import software.xdev.mockserver.model.Cookie;

import java.util.Arrays;
import java.util.List;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class CookieToJavaSerializer implements MultiValueToJavaSerializer<Cookie> {
    @Override
    public String serialize(int numberOfSpacesToIndent, Cookie cookie) {
        return NEW_LINE + Strings.padStart("", numberOfSpacesToIndent * INDENT_SIZE, ' ') + "new Cookie(" +
            NottableStringToJavaSerializer.serialize(cookie.getName(), false) + ", " +
            NottableStringToJavaSerializer.serialize(cookie.getValue(), false) + ")";
    }

    @Override
    public String serializeAsJava(int numberOfSpacesToIndent, List<Cookie> cookies) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < cookies.size(); i++) {
            output.append(serialize(numberOfSpacesToIndent, cookies.get(i)));
            if (i < (cookies.size() - 1)) {
                output.append(",");
            }
        }
        return output.toString();
    }

    @Override
    public String serializeAsJava(int numberOfSpacesToIndent, Cookie... object) {
        return serializeAsJava(numberOfSpacesToIndent, Arrays.asList(object));
    }
}
