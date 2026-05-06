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
package software.xdev.mockserver.matchers;

import java.util.Arrays;
import java.util.Objects;

import software.xdev.mockserver.logging.BinaryArrayFormatter;


public class BinaryMatcher extends BodyMatcher<byte[]>
{
	private final byte[] matcher;
	
	BinaryMatcher(final byte[] matcher)
	{
		this.matcher = matcher;
	}
	
	@Override
	public boolean matches(final MatchDifference context, final byte[] matched)
	{
		boolean result = false;
		
		if(this.matcher == null || this.matcher.length == 0 || Arrays.equals(this.matcher, matched))
		{
			result = true;
		}
		
		if(!result && context != null)
		{
			context.addDifference(
				"binary match failed expected:{}found:{}",
				BinaryArrayFormatter.byteArrayToString(this.matcher),
				BinaryArrayFormatter.byteArrayToString(matched));
		}
		
		return this.not != result;
	}
	
	@Override
	public boolean isBlank()
	{
		return this.matcher == null || this.matcher.length == 0;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final BinaryMatcher that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.deepEquals(this.matcher, that.matcher);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), Arrays.hashCode(this.matcher));
	}
}
