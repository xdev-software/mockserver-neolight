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

import static software.xdev.mockserver.model.NottableOptionalString.optional;

import java.util.Arrays;
import java.util.Collection;


public class Parameter extends KeyToMultiValue
{
	public Parameter(final String name, final String... value)
	{
		super(name, value);
	}
	
	public Parameter(final NottableString name, final NottableString... value)
	{
		super(name, value);
	}
	
	public Parameter(final NottableString name, final String... value)
	{
		super(name, value);
	}
	
	public Parameter(final String name, final Collection<String> value)
	{
		super(name, value);
	}
	
	public Parameter(final NottableString name, final Collection<NottableString> value)
	{
		super(name, value);
	}
	
	public static Parameter param(final String name, final String... value)
	{
		return new Parameter(name, value);
	}
	
	public static Parameter param(final NottableString name, final NottableString... value)
	{
		return new Parameter(name, value);
	}
	
	public static Parameter param(final NottableString name, final String... value)
	{
		return new Parameter(name, value);
	}
	
	public static Parameter param(final String name, final Collection<String> value)
	{
		return new Parameter(name, value);
	}
	
	public static Parameter param(final NottableString name, final Collection<NottableString> value)
	{
		return new Parameter(name, value);
	}
	
	public static Parameter optionalParam(final String name, final String... values)
	{
		return new Parameter(
			optional(name),
			Arrays.stream(values.length == 0 ? new String[]{".*"} : values)
				.map(NottableString::string)
				.toArray(NottableString[]::new));
	}
	
	public Parameter withStyle(final ParameterStyle style)
	{
		this.getName().withStyle(style);
		return this;
	}
}
