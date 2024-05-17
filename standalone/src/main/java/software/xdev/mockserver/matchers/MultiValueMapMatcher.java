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

import software.xdev.mockserver.collections.NottableStringMultiMap;
import software.xdev.mockserver.model.KeyMatchStyle;
import software.xdev.mockserver.model.KeyToMultiValue;
import software.xdev.mockserver.model.KeysToMultiValues;

import static software.xdev.mockserver.model.NottableString.string;

import java.util.Objects;


@SuppressWarnings("rawtypes")
public class MultiValueMapMatcher extends NotMatcher<KeysToMultiValues<? extends KeyToMultiValue, ? extends KeysToMultiValues>> {
    private final NottableStringMultiMap matcher;
    private final KeysToMultiValues keysToMultiValues;
    private final boolean controlPlaneMatcher;
    private Boolean allKeysNotted;
    private Boolean allKeysOptional;

    MultiValueMapMatcher(KeysToMultiValues<? extends KeyToMultiValue, ? extends KeysToMultiValues> keysToMultiValues, boolean controlPlaneMatcher) {
        this.keysToMultiValues = keysToMultiValues;
        this.controlPlaneMatcher = controlPlaneMatcher;
        if (keysToMultiValues != null) {
            this.matcher = new NottableStringMultiMap(this.controlPlaneMatcher, keysToMultiValues.getKeyMatchStyle(), keysToMultiValues.getEntries());
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
            result = new NottableStringMultiMap(controlPlaneMatcher, matched.getKeyMatchStyle(), matched.getEntries()).containsAll(context, matcher);
        }

        if (!result && context != null) {
            context.addDifference("multimap match failed expected:{}found:{}failed because:{}", keysToMultiValues, matched != null ? matched : "none", matched != null ? (matcher.getKeyMatchStyle() == KeyMatchStyle.SUB_SET ? "multimap is not a subset" : "multimap values don't match") : "none is not a subset");
        }

        return not != result;
    }

    public boolean isBlank() {
        return matcher == null || matcher.isEmpty();
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final MultiValueMapMatcher that))
        {
            return false;
        }
        if(!super.equals(o))
        {
            return false;
        }
		return controlPlaneMatcher == that.controlPlaneMatcher
            && Objects.equals(matcher, that.matcher)
            && Objects.equals(keysToMultiValues, that.keysToMultiValues)
            && Objects.equals(allKeysNotted, that.allKeysNotted)
            && Objects.equals(allKeysOptional, that.allKeysOptional);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(
            super.hashCode(),
            matcher,
            keysToMultiValues,
            controlPlaneMatcher,
            allKeysNotted,
            allKeysOptional);
    }
}
