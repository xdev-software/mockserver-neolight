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
package software.xdev.mockserver.matchers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.xdev.mockserver.util.StringUtils;
import software.xdev.mockserver.model.NottableString;

import static software.xdev.mockserver.model.NottableString.string;

public class ExactStringMatcher extends BodyMatcher<NottableString> {
    private final NottableString matcher;

    ExactStringMatcher(NottableString matcher) {
        this.matcher = matcher;
    }

    public static boolean matches(String matcher, String matched, boolean ignoreCase) {

        if (StringUtils.isBlank(matcher)) {
            return true;
        } else if (matched != null) {
            if (matched.equals(matcher)) {
                return true;
            }
            // case-insensitive comparison is mainly to improve matching in web containers like Tomcat that convert header names to lower case
            if (ignoreCase) {
                return matched.equalsIgnoreCase(matcher);
            }
        }

        return false;
    }

    public boolean matches(final MatchDifference context, String matched) {
        return matches(context, string(matched));
    }

    public boolean matches(final MatchDifference context, NottableString matched) {
        boolean result = false;

        if (matcher == null) {
            return true;
        }

        if (matched != null && matches(matcher.getValue(), matched.getValue(), false)) {
            result = true;
        }

        if (!result && context != null) {
            context.addDifference("exact string match failed expected:{}found:{}", this.matcher, matched);
        }

        if (matched == null) {
            return false;
        }

        return matched.isNot() == (matcher.isNot() == (not != result));
    }

    public boolean isBlank() {
        return matcher == null || StringUtils.isBlank(matcher.getValue());
    }
}
