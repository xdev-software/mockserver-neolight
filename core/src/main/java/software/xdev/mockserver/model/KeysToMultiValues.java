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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

import static software.xdev.mockserver.model.NottableString.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class KeysToMultiValues<T extends KeyToMultiValue, K extends KeysToMultiValues> extends ObjectWithJsonToString {

    private KeyMatchStyle keyMatchStyle = KeyMatchStyle.SUB_SET;

    private final Multimap<NottableString, NottableString> multimap;
    private final K k = (K) this;

    protected KeysToMultiValues() {
        multimap = LinkedHashMultimap.create();
    }

    protected KeysToMultiValues(Multimap<NottableString, NottableString> multimap) {
        this.multimap = LinkedHashMultimap.create(multimap);
    }

    public abstract T build(final NottableString name, final Collection<NottableString> values);

    protected abstract void isModified();

    public KeyMatchStyle getKeyMatchStyle() {
        return keyMatchStyle;
    }

    @SuppressWarnings("UnusedReturnValue")
    public KeysToMultiValues<T, K> withKeyMatchStyle(KeyMatchStyle keyMatchStyle) {
        this.keyMatchStyle = keyMatchStyle;
        return this;
    }

    public K withEntries(final Map<String, List<String>> entries) {
        isModified();
        multimap.clear();
        for (String name : entries.keySet()) {
            for (String value : entries.get(name)) {
                withEntry(name, value);
            }
        }
        return k;
    }

    public K withEntries(final List<T> entries) {
        isModified();
        multimap.clear();
        if (entries != null) {
            for (T entry : entries) {
                withEntry(entry);
            }
        }
        return k;
    }

    @SafeVarargs
    public final K withEntries(final T... entries) {
        if (ArrayUtils.isNotEmpty(entries)) {
            withEntries(Arrays.asList(entries));
        }
        return k;
    }

    public K withEntry(final T entry) {
        if (entry != null) {
            isModified();
            if (entry.getValues().isEmpty()) {
                multimap.put(entry.getName(), null);
            } else {
                multimap.putAll(entry.getName(), entry.getValues());
            }
        }
        return k;
    }

    public K withEntry(final String name, final String... values) {
        isModified();
        if (values == null || values.length == 0) {
            multimap.put(string(name), string(""));
        } else {
            multimap.putAll(string(name), deserializeNottableStrings(values));
        }
        return k;
    }

    public K withEntry(final String name, final List<String> values) {
        isModified();
        if (values == null || values.size() == 0) {
            multimap.put(string(name), null);
        } else {
            multimap.putAll(string(name), deserializeNottableStrings(values));
        }
        return k;
    }

    public K withEntry(final NottableString name, final List<NottableString> values) {
        if (values != null) {
            isModified();
            multimap.putAll(name, values);
        }
        return k;
    }

    public K withEntry(final NottableString name, final NottableString... values) {
        if (ArrayUtils.isNotEmpty(values)) {
            withEntry(name, Arrays.asList(values));
        }
        return k;
    }

    public boolean remove(final String name) {
        boolean exists = false;
        if (name != null) {
            isModified();
            for (NottableString key : multimap.keySet().toArray(new NottableString[0])) {
                if (key.equalsIgnoreCase(name)) {
                    multimap.removeAll(key);
                    exists = true;
                }
            }
        }
        return exists;
    }

    public boolean remove(final NottableString name) {
        boolean exists = false;
        if (name != null) {
            isModified();
            for (NottableString key : multimap.keySet().toArray(new NottableString[0])) {
                if (key.equalsIgnoreCase(name)) {
                    multimap.removeAll(key);
                    exists = true;
                }
            }
        }
        return exists;
    }

    @SuppressWarnings("UnusedReturnValue")
    public K replaceEntry(final T entry) {
        if (entry != null) {
            isModified();
            remove(entry.getName());
            multimap.putAll(entry.getName(), entry.getValues());
        }
        return k;
    }

    @SuppressWarnings("UnusedReturnValue")
    public K replaceEntryIfExists(final T entry) {
        if (entry != null) {
            isModified();
            if (remove(entry.getName())) {
                multimap.putAll(entry.getName(), entry.getValues());
            }
        }
        return k;
    }

    @SuppressWarnings("UnusedReturnValue")
    public K replaceEntry(final String name, final String... values) {
        if (ArrayUtils.isNotEmpty(values)) {
            isModified();
            remove(name);
            multimap.putAll(string(name), deserializeNottableStrings(values));
        }
        return k;
    }

    public List<T> getEntries() {
        if (!isEmpty()) {
            ArrayList<T> headers = new ArrayList<>();
            for (NottableString nottableString : multimap.keySet().toArray(new NottableString[0])) {
                headers.add(build(nottableString, multimap.get(nottableString)));
            }
            return headers;
        } else {
            return Collections.emptyList();
        }
    }

    public Set<NottableString> keySet() {
        return multimap.keySet();
    }

    public Collection<NottableString> getValues(NottableString key) {
        return multimap.get(key);
    }

    public Multimap<NottableString, NottableString> getMultimap() {
        return multimap;
    }

    public List<String> getValues(final String name) {
        if (!isEmpty() && name != null) {
            List<String> values = new ArrayList<>();
            for (NottableString key : multimap.keySet().toArray(new NottableString[0])) {
                if (key != null && key.equalsIgnoreCase(name)) {
                    values.addAll(serialiseNottableStrings(multimap.get(key)));
                }
            }
            return values;
        } else {
            return Collections.emptyList();
        }
    }

    String getFirstValue(final String name) {
        if (!isEmpty()) {
            for (NottableString key : multimap.keySet().toArray(new NottableString[0])) {
                if (key != null && key.equalsIgnoreCase(name)) {
                    Collection<NottableString> nottableStrings = multimap.get(key);
                    if (!nottableStrings.isEmpty()) {
                        NottableString next = nottableStrings.iterator().next();
                        if (next != null) {
                            return next.getValue();
                        }
                    }
                }
            }
        }
        return "";
    }

    public boolean containsEntry(final String name) {
        if (!isEmpty()) {
            for (NottableString key : multimap.keySet().toArray(new NottableString[0])) {
                if (key != null && key.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsEntry(final String name, final String value) {
        return containsEntry(string(name), string(value));
    }

    boolean containsEntry(final NottableString name, final NottableString value) {
        if (!isEmpty() && name != null && value != null) {
            for (NottableString entryKey : multimap.keySet().toArray(new NottableString[0])) {
                if (entryKey != null && entryKey.equalsIgnoreCase(name)) {
                    Collection<NottableString> nottableStrings = multimap.get(entryKey);
                    if (nottableStrings != null) {
                        for (NottableString entryValue : nottableStrings.toArray(new NottableString[0])) {
                            if (value.equalsIgnoreCase(entryValue)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return multimap.isEmpty();
    }

    public abstract K clone();

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeysToMultiValues)) {
            return false;
        }
        KeysToMultiValues<?, ?> that = (KeysToMultiValues<?, ?>) o;
        return Objects.equals(multimap, that.multimap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multimap);
    }
}
