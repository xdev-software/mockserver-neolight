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
import software.xdev.mockserver.logging.BinaryArrayFormatter;

import java.util.Arrays;

public class BinaryMatcher extends BodyMatcher<byte[]> {
    private final byte[] matcher;

    BinaryMatcher(byte[] matcher) {
        this.matcher = matcher;
    }

    public boolean matches(final MatchDifference context, byte[] matched) {
        boolean result = false;

        if (matcher == null || matcher.length == 0 || Arrays.equals(matcher, matched)) {
            result = true;
        }

        if (!result && context != null) {
            context.addDifference("binary match failed expected:{}found:{}", BinaryArrayFormatter.byteArrayToString(this.matcher), BinaryArrayFormatter.byteArrayToString(matched));
        }

        return not != result;
    }

    public boolean isBlank() {
        return matcher == null || matcher.length == 0;
    }
}
