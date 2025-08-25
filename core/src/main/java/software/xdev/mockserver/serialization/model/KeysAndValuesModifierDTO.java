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

import java.util.List;
import java.util.Objects;

import software.xdev.mockserver.model.KeyAndValue;
import software.xdev.mockserver.model.KeysAndValues;
import software.xdev.mockserver.model.KeysAndValuesModifier;


@SuppressWarnings("unchecked")
public abstract class KeysAndValuesModifierDTO<
	T extends KeysAndValues<I, T>,
	K extends KeysAndValuesModifier<T, K, I>,
	I extends KeyAndValue,
	D extends DTO<K>>
	implements DTO<K>
{
	private T add;
	private T replace;
	private List<String> remove;
	
	protected KeysAndValuesModifierDTO()
	{
	}
	
	protected KeysAndValuesModifierDTO(final K keysAndValuesModifier)
	{
		if(keysAndValuesModifier != null)
		{
			this.add = keysAndValuesModifier.getAdd();
			this.replace = keysAndValuesModifier.getReplace();
			this.remove = keysAndValuesModifier.getRemove();
		}
	}
	
	@Override
	public K buildObject()
	{
		return this.newKeysAndValuesModifier()
			.withAdd(this.add)
			.withReplace(this.replace)
			.withRemove(this.remove);
	}
	
	abstract K newKeysAndValuesModifier();
	
	public T getAdd()
	{
		return this.add;
	}
	
	public D setAdd(final T add)
	{
		this.add = add;
		return (D)this;
	}
	
	public T getReplace()
	{
		return this.replace;
	}
	
	public D setReplace(final T replace)
	{
		this.replace = replace;
		return (D)this;
	}
	
	public List<String> getRemove()
	{
		return this.remove;
	}
	
	public D setRemove(final List<String> remove)
	{
		this.remove = remove;
		return (D)this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final KeysAndValuesModifierDTO<?, ?, ?, ?> that))
		{
			return false;
		}
		return Objects.equals(this.getAdd(), that.getAdd())
			&& Objects.equals(this.getReplace(), that.getReplace())
			&& Objects.equals(this.getRemove(), that.getRemove());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getAdd(), this.getReplace(), this.getRemove());
	}
}
