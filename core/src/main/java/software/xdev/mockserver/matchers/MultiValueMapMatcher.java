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
import software.xdev.mockserver.collections.NottableStringMultiMap;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.KeyMatchStyle;
import software.xdev.mockserver.model.KeyToMultiValue;
import software.xdev.mockserver.model.KeysToMultiValues;

import static software.xdev.mockserver.model.NottableString.string;

@SuppressWarnings("rawtypes")
public class MultiValueMapMatcher extends NotMatcher<KeysToMultiValues<? extends KeyToMultiValue, ? extends KeysToMultiValues>> {
    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger"};
    private final MockServerLogger mockServerLogger;
    private final NottableStringMultiMap matcher;
    private final KeysToMultiValues keysToMultiValues;
    private final boolean controlPlaneMatcher;
    private Boolean allKeysNotted;
    private Boolean allKeysOptional;

    MultiValueMapMatcher(MockServerLogger mockServerLogger, KeysToMultiValues<? extends KeyToMultiValue, ? extends KeysToMultiValues> keysToMultiValues, boolean controlPlaneMatcher) {
        this.mockServerLogger = mockServerLogger;
        this.keysToMultiValues = keysToMultiValues;
        this.controlPlaneMatcher = controlPlaneMatcher;
        if (keysToMultiValues != null) {
            this.matcher = new NottableStringMultiMap(this.mockServerLogger, this.controlPlaneMatcher, keysToMultiValues.getKeyMatchStyle(), keysToMultiValues.getEntries());
        } else {
            this.matcher = null;
        }
    }

    public boolean matches(final MatchDifference context, KeysToMultiValues<? extends KeyToMultiValue, ? extends KeysToMultiValues> matched) {
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
            result = new NottableStringMultiMap(mockServerLogger, controlPlaneMatcher, matched.getKeyMatchStyle(), matched.getEntries()).containsAll(mockServerLogger, context, matcher);
        }

        if (!result && context != null) {
            context.addDifference(mockServerLogger, "multimap match failed expected:{}found:{}failed because:{}", keysToMultiValues, matched != null ? matched : "none", matched != null ? (matcher.getKeyMatchStyle() == KeyMatchStyle.SUB_SET ? "multimap is not a subset" : "multimap values don't match") : "none is not a subset");
        }

        return not != result;
    }

    public boolean isBlank() {
        return matcher == null || matcher.isEmpty();
    }

    @Override
    @JsonIgnore
    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return EXCLUDED_FIELDS;
    }
}