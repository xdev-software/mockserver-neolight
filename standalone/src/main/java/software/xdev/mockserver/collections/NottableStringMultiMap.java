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
package software.xdev.mockserver.collections;

import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.matchers.RegexStringMatcher;
import software.xdev.mockserver.model.*;

import java.util.*;

import static software.xdev.mockserver.collections.ImmutableEntry.entry;
import static software.xdev.mockserver.collections.SubSetMatcher.containsSubset;

public class NottableStringMultiMap extends ObjectWithReflectiveEqualsHashCodeToString {

    private final Map<NottableString, List<NottableString>> backingMap = new LinkedHashMap<>();
    private final RegexStringMatcher regexStringMatcher;
    private final KeyMatchStyle keyMatchStyle;

    public NottableStringMultiMap(boolean controlPlaneMatcher, KeyMatchStyle keyMatchStyle, List<? extends KeyToMultiValue> entries) {
        this.keyMatchStyle = keyMatchStyle;
        regexStringMatcher = new RegexStringMatcher(controlPlaneMatcher);
        for (KeyToMultiValue keyToMultiValue : entries) {
            backingMap.put(keyToMultiValue.getName(), keyToMultiValue.getValues());
        }
    }

    public NottableStringMultiMap(boolean controlPlaneMatcher, KeyMatchStyle keyMatchStyle, NottableString[]... keyAndValues) {
        this.keyMatchStyle = keyMatchStyle;
        regexStringMatcher = new RegexStringMatcher(controlPlaneMatcher);
        for (NottableString[] keyAndValue : keyAndValues) {
            if (keyAndValue.length > 0) {
                backingMap.put(keyAndValue[0], keyAndValue.length > 1 ? Arrays.asList(keyAndValue).subList(1, keyAndValue.length) : Collections.emptyList());
            }
        }
    }

    public KeyMatchStyle getKeyMatchStyle() {
        return keyMatchStyle;
    }

    public boolean containsAll(MatchDifference context, NottableStringMultiMap subset) {
        switch (subset.keyMatchStyle) {
            case SUB_SET: {
                boolean isSubset = containsSubset(context, regexStringMatcher, subset.entryList(), entryList());
                if (!isSubset && context != null) {
                    context.addDifference("multimap subset match failed subset:{}was not a subset of:{}", subset.entryList(), entryList());
                }
                return isSubset;
            }
            case MATCHING_KEY: {
                for (NottableString matcherKey : subset.backingMap.keySet()) {
                    List<NottableString> matchedValuesForKey = getAll(matcherKey);
                    if (matchedValuesForKey.isEmpty() && !matcherKey.isOptional()) {
                        if (context != null) {
                            context.addDifference("multimap subset match failed subset:{}did not have expected key:{}", subset, matcherKey);
                        }
                        return false;
                    }

                    List<NottableString> matcherValuesForKey = subset.getAll(matcherKey);
                    for (NottableString matchedValue : matchedValuesForKey) {
                        boolean matchesValue = false;
                        for (NottableString matcherValue : matcherValuesForKey) {
                            // match item by item
                            if (regexStringMatcher.matches(context, matcherValue, matchedValue)) {
                                matchesValue = true;
                                break;
                            } else {
                                if (context != null) {
                                    context.addDifference("multimap matching key match failed for key:{}", matcherKey);
                                }
                            }
                        }
                        if (!matchesValue) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean allKeysNotted() {
        if (!isEmpty()) {
            for (NottableString key : backingMap.keySet()) {
                if (!key.isNot()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean allKeysOptional() {
        if (!isEmpty()) {
            for (NottableString key : backingMap.keySet()) {
                if (!key.isOptional()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    private List<NottableString> getAll(NottableString key) {
        if (!isEmpty()) {
            List<NottableString> values = new ArrayList<>();
            for (Map.Entry<NottableString, List<NottableString>> entry : backingMap.entrySet()) {
                if (regexStringMatcher.matches(key, entry.getKey())) {
                    values.addAll(entry.getValue());
                }
            }
            return values;
        } else {
            return Collections.emptyList();
        }
    }

    private List<ImmutableEntry> entryList() {
        if (!isEmpty()) {
            List<ImmutableEntry> entrySet = new ArrayList<>();
            for (Map.Entry<NottableString, List<NottableString>> entry : backingMap.entrySet()) {
                for (NottableString value : entry.getValue()) {
                    entrySet.add(entry(regexStringMatcher, entry.getKey(), value));
                }
            }
            return entrySet;
        } else {
            return Collections.emptyList();
        }
    }
}



