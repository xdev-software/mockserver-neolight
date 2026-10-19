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

import static software.xdev.mockserver.model.NottableString.string;
import static software.xdev.mockserver.util.StringUtils.isBlank;

import java.util.Objects;


public class KeyAndValue extends ObjectWithJsonToString
{
	private final NottableString name;
	private final NottableString value;
	private final int hashCode;
	
	public KeyAndValue(final String name, final String value)
	{
		this(string(name), string(isBlank(value) ? "" : value));
	}
	
	public KeyAndValue(final NottableString name, final String value)
	{
		this(name, string(isBlank(value) ? "" : value));
	}
	
	public KeyAndValue(final NottableString name, final NottableString value)
	{
		this.name = name;
		this.value = value;
		this.hashCode = Objects.hash(name, value);
	}
	
	public NottableString getName()
	{
		return this.name;
	}
	
	public NottableString getValue()
	{
		return this.value;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		if(this.hashCode() != o.hashCode())
		{
			return false;
		}
		final KeyAndValue that = (KeyAndValue)o;
		return Objects.equals(this.name, that.name) && Objects.equals(this.value, that.value);
	}
	
	@Override
	public int hashCode()
	{
		return this.hashCode;
	}
}
