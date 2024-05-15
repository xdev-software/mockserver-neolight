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
package software.xdev.mockserver.templates.engine.javascript.bindings;

import org.apache.commons.lang3.NotImplementedException;

import javax.script.Bindings;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ScriptBindings implements Bindings {

    private final Map<String, Supplier<Object>> suppliers;
    private final Map<String, Object> otherState;

    public ScriptBindings(Map<String, Supplier<Object>> suppliers) {
        this.suppliers = suppliers;
        this.otherState = new ConcurrentHashMap<>();
    }

    @Override
    public Object put(String name, Object value) {
        return otherState.put(name, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> toMerge) {
        otherState.putAll(toMerge);
    }

    @Override
    public void clear() {
        otherState.clear();
        suppliers.clear();
    }

    @Override
    public Set<String> keySet() {
        throw new NotImplementedException("keySet not implemented by " + this.getClass().getSimpleName());
    }

    @Override
    public Collection<Object> values() {
        throw new NotImplementedException("values not implemented by " + this.getClass().getSimpleName());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new NotImplementedException("entrySet not implemented by " + this.getClass().getSimpleName());
    }

    @Override
    public int size() {
        return suppliers.size() + otherState.size();
    }

    @Override
    public boolean isEmpty() {
        return suppliers.isEmpty() && otherState.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return suppliers.containsKey(key) || otherState.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new NotImplementedException("containsValue not implemented by " + this.getClass().getSimpleName());
    }

    @Override
    public Object get(Object key) {
        Supplier<Object> objectSupplier = suppliers.get(key);
        if (objectSupplier != null) {
            return objectSupplier.get();
        }
        return otherState.get(key);
    }

    @Override
    public Object remove(Object key) {
        otherState.remove(key);
        return suppliers.remove(key);
    }

}
