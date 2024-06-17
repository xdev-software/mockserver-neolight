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

import java.util.Collection;


public class Header extends KeyToMultiValue
{
	public Header(final String name, final String... value)
	{
		super(name, value);
	}
	
	public Header(final NottableString name, final NottableString... value)
	{
		super(name, value);
	}
	
	public Header(final NottableString name, final String... value)
	{
		super(name, value);
	}
	
	public Header(final String name, final Collection<String> value)
	{
		super(name, value);
	}
	
	public Header(final NottableString name, final Collection<NottableString> value)
	{
		super(name, value);
	}
	
	public static Header header(final String name, final int value)
	{
		return new Header(name, String.valueOf(value));
	}
	
	public static Header header(final String name, final String... value)
	{
		return new Header(name, value);
	}
	
	public static Header header(final NottableString name, final NottableString... value)
	{
		return new Header(name, value);
	}
	
	public static Header header(final String name, final Collection<String> value)
	{
		return new Header(name, value);
	}
	
	public static Header header(final NottableString name, final Collection<NottableString> value)
	{
		return new Header(name, value);
	}
}
