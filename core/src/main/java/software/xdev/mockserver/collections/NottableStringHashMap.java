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
import software.xdev.mockserver.model.KeyAndValue;
import software.xdev.mockserver.model.NottableString;

import java.util.*;

import static software.xdev.mockserver.collections.ImmutableEntry.entry;
import static software.xdev.mockserver.collections.SubSetMatcher.containsSubset;
import static software.xdev.mockserver.model.NottableString.string;

public class NottableStringHashMap {

    private final Map<NottableString, NottableString> backingMap = new LinkedHashMap<>();
    private final RegexStringMatcher regexStringMatcher;

    public NottableStringHashMap(boolean controlPlaneMatcher, List<? extends KeyAndValue> entries) {
        regexStringMatcher = new RegexStringMatcher(controlPlaneMatcher);
        for (KeyAndValue keyToMultiValue : entries) {
            put(keyToMultiValue.getName(), keyToMultiValue.getValue());
        }
    }

    public NottableStringHashMap(boolean controlPlaneMatcher, NottableString[]... keyAndValues) {
        regexStringMatcher = new RegexStringMatcher(controlPlaneMatcher);
        for (NottableString[] keyAndValue : keyAndValues) {
            if (keyAndValue.length >= 2) {
                put(keyAndValue[0], keyAndValue[1]);
            }
        }
    }

    public boolean containsAll(MatchDifference context, NottableStringHashMap subset) {
        return containsSubset(context, regexStringMatcher, subset.entryList(), entryList());
    }

    public boolean allKeysNotted() {
        for (NottableString key : backingMap.keySet()) {
            if (!key.isNot()) {
                return false;
            }
        }
        return true;
    }

    public boolean allKeysOptional() {
        for (NottableString key : backingMap.keySet()) {
            if (!key.isOptional()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    private void put(NottableString key, NottableString value) {
        backingMap.put(key, value != null ? value : string(""));
    }

    private List<ImmutableEntry> entryList() {
        if (!backingMap.isEmpty()) {
            List<ImmutableEntry> entrySet = new ArrayList<>();
            for (Map.Entry<NottableString, NottableString> entry : backingMap.entrySet()) {
                entrySet.add(entry(regexStringMatcher, entry.getKey(), entry.getValue()));
            }
            return entrySet;
        } else {
            return Collections.emptyList();
        }
    }
}
