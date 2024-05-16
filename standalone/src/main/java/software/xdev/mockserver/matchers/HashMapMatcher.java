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

import software.xdev.mockserver.collections.NottableStringHashMap;
import software.xdev.mockserver.model.KeyAndValue;
import software.xdev.mockserver.model.KeysAndValues;

@SuppressWarnings("rawtypes")
public class HashMapMatcher extends NotMatcher<KeysAndValues<? extends KeyAndValue, ? extends KeysAndValues>> {
    private final NottableStringHashMap matcher;
    private final KeysAndValues keysAndValues;
    private final boolean controlPlaneMatcher;
    private Boolean allKeysNotted;
    private Boolean allKeysOptional;

    HashMapMatcher(KeysAndValues<? extends KeyAndValue, ? extends KeysAndValues> keysAndValues, boolean controlPlaneMatcher) {
        this.keysAndValues = keysAndValues;
        this.controlPlaneMatcher = controlPlaneMatcher;
        if (keysAndValues != null) {
            this.matcher = new NottableStringHashMap(this.controlPlaneMatcher, keysAndValues.getEntries());
        } else {
            this.matcher = null;
        }
    }

    public boolean matches(final MatchDifference context, KeysAndValues<? extends KeyAndValue, ? extends KeysAndValues> matched) {
        boolean result;

        if (matcher == null || matcher.isEmpty()) {
            result = true;
        } else if (matched == null || matched.isEmpty()) {
            if (allKeysNotted == null) {
                allKeysNotted = matcher.allKeysNotted();
            }
            if (allKeysOptional == null) {
                allKeysOptional = matcher.allKeysOptional();
            }
            result = allKeysNotted || allKeysOptional;
        } else {
            result = new NottableStringHashMap(controlPlaneMatcher, matched.getEntries()).containsAll(context, matcher);
        }

        if (!result && context != null) {
            context.addDifference("map subset match failed expected:{}found:{}failed because:{}", keysAndValues, matched != null ? matched : "none", matched != null ? "map is not a subset" : "none is not a subset");
        }

        return not != result;
    }

    public boolean isBlank() {
        return matcher == null || matcher.isEmpty();
    }
}
