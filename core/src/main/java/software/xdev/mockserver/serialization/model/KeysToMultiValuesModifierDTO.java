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

import software.xdev.mockserver.model.KeyToMultiValue;
import software.xdev.mockserver.model.KeysToMultiValues;
import software.xdev.mockserver.model.KeysToMultiValuesModifier;

import java.util.List;
import java.util.Objects;


@SuppressWarnings("unchecked")
public abstract class KeysToMultiValuesModifierDTO<
    T extends KeysToMultiValues<I, T>,
    K extends KeysToMultiValuesModifier<T, K, I>,
    I extends KeyToMultiValue,
    D extends DTO<K>>
    implements DTO<K> {

    private T add;
    private T replace;
    private List<String> remove;

    protected KeysToMultiValuesModifierDTO() {
    }
    
    protected KeysToMultiValuesModifierDTO(K keysToMultiValuesModifier) {
        if (keysToMultiValuesModifier != null) {
            add = keysToMultiValuesModifier.getAdd();
            replace = keysToMultiValuesModifier.getReplace();
            remove = keysToMultiValuesModifier.getRemove();
        }
    }

    public K buildObject() {
        return newKeysToMultiValuesModifier()
            .withAdd(add)
            .withReplace(replace)
            .withRemove(remove);
    }

    abstract K newKeysToMultiValuesModifier();

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
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final KeysToMultiValuesModifierDTO<?, ?, ?, ?> that))
        {
            return false;
        }
		return Objects.equals(getAdd(), that.getAdd())
            && Objects.equals(getReplace(), that.getReplace())
            && Objects.equals(getRemove(), that.getRemove());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(getAdd(), getReplace(), getRemove());
    }
}
