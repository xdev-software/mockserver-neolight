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

import org.apache.commons.lang3.tuple.Pair;
import software.xdev.mockserver.matchers.RegexStringMatcher;
import software.xdev.mockserver.model.NottableString;

import java.util.*;

import static software.xdev.mockserver.model.NottableString.string;

public class ImmutableEntry extends Pair<NottableString, NottableString> implements Map.Entry<NottableString, NottableString> {
    private final RegexStringMatcher regexStringMatcher;
    private final NottableString key;
    private final NottableString value;

    public static ImmutableEntry entry(RegexStringMatcher regexStringMatcher, String key, String value) {
        return new ImmutableEntry(regexStringMatcher, key, value);
    }

    public static ImmutableEntry entry(RegexStringMatcher regexStringMatcher, NottableString key, NottableString value) {
        return new ImmutableEntry(regexStringMatcher, key, value);
    }

    ImmutableEntry(RegexStringMatcher regexStringMatcher, String key, String value) {
        this.regexStringMatcher = regexStringMatcher;
        this.key = string(key);
        this.value = string(value);
    }

    ImmutableEntry(RegexStringMatcher regexStringMatcher, NottableString key, NottableString value) {
        this.regexStringMatcher = regexStringMatcher;
        this.key = key;
        this.value = value;
    }

    public boolean isOptional() {
        return getKey().isOptional();
    }

    public boolean isNotted() {
        return getKey().isNot() && !getValue().isNot();
    }

    public boolean isNotOptional() {
        return !isOptional();
    }

    @Override
    public NottableString getLeft() {
        return key;
    }

    @Override
    public NottableString getRight() {
        return value;
    }

    @Override
    public NottableString setValue(NottableString value) {
        throw new UnsupportedOperationException("ImmutableEntry is immutable");
    }

    @Override
    public String toString() {
        return "(" + key + ": " + value + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImmutableEntry that = (ImmutableEntry) o;
        return regexStringMatcher.matches(key, that.key) &&
            regexStringMatcher.matches(value, that.value) ||
            (
                !regexStringMatcher.matches(key, that.key) &&
                    (
                        key.isOptional() || that.key.isOptional()
                    )
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    public static <T> boolean listsEqual(List<T> matcher, List<T> matched) {
        boolean matches = false;
        if (matcher.size() == matched.size()) {
            Set<Integer> matchedIndexes = new HashSet<>();
            Set<Integer> matcherIndexes = new HashSet<>();
            for (int i = 0; i < matcher.size(); i++) {
                T matcherItem = matcher.get(i);
                for (int j = 0; j < matched.size(); j++) {
                    T matchedItem = matched.get(j);
                    if (matcherItem != null && matcherItem.equals(matchedItem)) {
                        matchedIndexes.add(j);
                        matcherIndexes.add(i);
                    }
                }
            }
            matches = matchedIndexes.size() == matched.size() && matcherIndexes.size() == matcher.size();
        }
        return matches;
    }

    public static boolean listsEqualWithOptionals(RegexStringMatcher regexStringMatcher, List<ImmutableEntry> matcher, List<ImmutableEntry> matched) {
        Set<Integer> matchingMatchedIndexes = new HashSet<>();
        Set<Integer> matchingMatcherIndexes = new HashSet<>();
        Set<NottableString> matcherKeys = new HashSet<>();
        matcher.forEach(matcherItem -> matcherKeys.add(matcherItem.getKey()));
        Set<NottableString> matchedKeys = new HashSet<>();
        matched.forEach(matchedItem -> matchedKeys.add(matchedItem.getKey()));
        for (int i = 0; i < matcher.size(); i++) {
            ImmutableEntry matcherItem = matcher.get(i);
            if (matcherItem != null) {
                for (int j = 0; j < matched.size(); j++) {
                    ImmutableEntry matchedItem = matched.get(j);
                    if (matchedItem != null) {
                        if (matcherItem.equals(matchedItem)) {
                            matchingMatchedIndexes.add(j);
                            matchingMatcherIndexes.add(i);
                        } else if (matcherItem.getKey().isOptional() && !contains(regexStringMatcher, matchedKeys, matcherItem.getKey())) {
                            matchingMatchedIndexes.add(j);
                            matchingMatcherIndexes.add(i);
                        } else if (matchedItem.getKey().isOptional() && !contains(regexStringMatcher, matcherKeys, matchedItem.getKey())) {
                            matchingMatchedIndexes.add(j);
                            matchingMatcherIndexes.add(i);
                        }
                    }
                }
            }
        }
        return matchingMatchedIndexes.size() == matched.size() && matchingMatcherIndexes.size() == matcher.size();
    }

    private static boolean contains(RegexStringMatcher regexStringMatcher, Set<NottableString> matchedKeys, NottableString matcherItem) {
        boolean result = false;
        for (NottableString matchedKey : matchedKeys) {
            if (regexStringMatcher.matches(matchedKey, matcherItem)) {
                return true;
            }
        }
        return result;
    }
}