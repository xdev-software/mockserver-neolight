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

import static software.xdev.mockserver.model.NottableString.string;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

import software.xdev.mockserver.matchers.RegexStringMatcher;
import software.xdev.mockserver.model.NottableString;


public class ImmutableEntry extends Pair<NottableString, NottableString>
	implements Map.Entry<NottableString, NottableString>
{
	private final RegexStringMatcher regexStringMatcher;
	private final NottableString key;
	private final NottableString value;
	
	public static ImmutableEntry entry(
		final RegexStringMatcher regexStringMatcher,
		final String key,
		final String value)
	{
		return new ImmutableEntry(regexStringMatcher, key, value);
	}
	
	public static ImmutableEntry entry(
		final RegexStringMatcher regexStringMatcher,
		final NottableString key,
		final NottableString value)
	{
		return new ImmutableEntry(regexStringMatcher, key, value);
	}
	
	ImmutableEntry(final RegexStringMatcher regexStringMatcher, final String key, final String value)
	{
		this.regexStringMatcher = regexStringMatcher;
		this.key = string(key);
		this.value = string(value);
	}
	
	ImmutableEntry(final RegexStringMatcher regexStringMatcher, final NottableString key, final NottableString value)
	{
		this.regexStringMatcher = regexStringMatcher;
		this.key = key;
		this.value = value;
	}
	
	public boolean isOptional()
	{
		return this.getKey().isOptional();
	}
	
	public boolean isNotted()
	{
		return this.getKey().isNot() && !this.getValue().isNot();
	}
	
	public boolean isNotOptional()
	{
		return !this.isOptional();
	}
	
	@Override
	public NottableString getLeft()
	{
		return this.key;
	}
	
	@Override
	public NottableString getRight()
	{
		return this.value;
	}
	
	@Override
	public NottableString setValue(final NottableString value)
	{
		throw new UnsupportedOperationException("ImmutableEntry is immutable");
	}
	
	@Override
	public String toString()
	{
		return "(" + this.key + ": " + this.value + ")";
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
		final ImmutableEntry that = (ImmutableEntry)o;
		return this.regexStringMatcher.matches(this.key, that.key)
			&& this.regexStringMatcher.matches(this.value, that.value)
			|| !this.regexStringMatcher.matches(this.key, that.key) && (this.key.isOptional() || that.key.isOptional());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.key, this.value);
	}
}
