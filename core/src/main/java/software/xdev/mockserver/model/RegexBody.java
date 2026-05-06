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

import java.util.Objects;


public class RegexBody extends Body<String>
{
	private int hashCode;
	private final String regex;
	
	public RegexBody(final String regex)
	{
		super(Type.REGEX);
		this.regex = regex;
	}
	
	@Override
	public String getValue()
	{
		return this.regex;
	}
	
	public static RegexBody regex(final String regex)
	{
		return new RegexBody(regex);
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
		if(!super.equals(o))
		{
			return false;
		}
		final RegexBody regexBody = (RegexBody)o;
		return Objects.equals(this.regex, regexBody.regex);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(super.hashCode(), this.regex);
		}
		return this.hashCode;
	}
}
