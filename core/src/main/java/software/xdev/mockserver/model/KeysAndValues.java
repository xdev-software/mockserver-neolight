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
package software.xdev.mockserver.model;

import java.util.*;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.model.NottableString.string;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class KeysAndValues<T extends KeyAndValue, K extends KeysAndValues> extends ObjectWithJsonToString {

    private final Map<NottableString, NottableString> map;

    protected KeysAndValues() {
        map = new LinkedHashMap<>();
    }

    protected KeysAndValues(Map<NottableString, NottableString> map) {
        this.map = new LinkedHashMap<>(map);
    }

    public abstract T build(NottableString name, NottableString value);

    public K withEntries(List<T> entries) {
        map.clear();
        if (entries != null) {
            for (T cookie : entries) {
                withEntry(cookie);
            }
        }
        return (K) this;
    }

    public K withEntries(T... entries) {
        if (entries != null) {
            withEntries(Arrays.asList(entries));
        }
        return (K) this;
    }

    public K withEntry(T entry) {
        if (entry != null) {
            map.put(entry.getName(), entry.getValue());
        }
        return (K) this;
    }

    public K withEntry(String name, String value) {
        map.put(string(name), string(value));
        return (K) this;
    }

    public K withEntry(NottableString name, NottableString value) {
        map.put(name, value);
        return (K) this;
    }

    public K replaceEntryIfExists(final T entry) {
        if (entry != null) {
            if (remove(entry.getName())) {
                map.put(entry.getName(), entry.getValue());
            }
        }
        return (K) this;
    }

    public List<T> getEntries() {
        if (!map.isEmpty()) {
            ArrayList<T> cookies = new ArrayList<>();
            for (NottableString nottableString : map.keySet()) {
                cookies.add(build(nottableString, map.get(nottableString)));
            }
            return cookies;
        } else {
            return Collections.emptyList();
        }
    }

    public Map<NottableString, NottableString> getMap() {
        return map;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean remove(NottableString name) {
        return remove(name.getValue());
    }

    public boolean remove(String name) {
        if (isNotBlank(name)) {
            return map.remove(string(name)) != null;
        }
        return false;
    }

    public abstract K clone();
}
