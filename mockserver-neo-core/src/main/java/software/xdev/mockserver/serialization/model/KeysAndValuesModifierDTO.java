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
package software.xdev.mockserver.serialization.model;

import software.xdev.mockserver.model.KeyAndValue;
import software.xdev.mockserver.model.KeysAndValues;
import software.xdev.mockserver.model.KeysAndValuesModifier;
import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;

import java.util.List;

@SuppressWarnings("unchecked")
public abstract class KeysAndValuesModifierDTO<T extends KeysAndValues<I, T>, K extends KeysAndValuesModifier<T, K, I>, I extends KeyAndValue, D extends DTO<K>> extends ObjectWithReflectiveEqualsHashCodeToString implements DTO<K> {

    private T add;
    private T replace;
    private List<String> remove;

    public KeysAndValuesModifierDTO() {
    }

    public KeysAndValuesModifierDTO(K keysAndValuesModifier) {
        if (keysAndValuesModifier != null) {
            add = keysAndValuesModifier.getAdd();
            replace = keysAndValuesModifier.getReplace();
            remove = keysAndValuesModifier.getRemove();
        }
    }

    public K buildObject() {
        return newKeysAndValuesModifier()
            .withAdd(add)
            .withReplace(replace)
            .withRemove(remove);
    }

    abstract K newKeysAndValuesModifier();

    public T getAdd() {
        return add;
    }

    public D setAdd(T add) {
        this.add = add;
        return (D) this;
    }

    public T getReplace() {
        return replace;
    }

    public D setReplace(T replace) {
        this.replace = replace;
        return (D) this;
    }

    public List<String> getRemove() {
        return remove;
    }

    public D setRemove(List<String> remove) {
        this.remove = remove;
        return (D) this;
    }

}
