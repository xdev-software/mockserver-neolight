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

import java.util.Objects;


public class Times
{
	private static final Times TIMES_UNLIMITED = new Times(-1, true)
	{
		@Override
		public int getRemainingTimes()
		{
			return -1;
		}
		
		@Override
		public boolean isUnlimited()
		{
			return true;
		}
		
		@Override
		public boolean greaterThenZero()
		{
			return true;
		}
		
		@Override
		public boolean decrement()
		{
			return false;
		}
	};
	
	private int hashCode;
	private int remainingTimes;
	private final boolean unlimited;
	
	private Times(final int remainingTimes, final boolean unlimited)
	{
		this.remainingTimes = remainingTimes;
		this.unlimited = unlimited;
	}
	
	public static Times unlimited()
	{
		return TIMES_UNLIMITED;
	}
	
	public static Times once()
	{
		return new Times(1, false);
	}
	
	public static Times exactly(final int count)
	{
		return new Times(count, false);
	}
	
	public int getRemainingTimes()
	{
		return this.remainingTimes;
	}
	
	public boolean isUnlimited()
	{
		return this.unlimited;
	}
	
	public boolean greaterThenZero()
	{
		return this.unlimited || this.remainingTimes > 0;
	}
	
	public boolean decrement()
	{
		if(!this.unlimited)
		{
			this.remainingTimes--;
			return true;
		}
		return false;
	}
	
	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public Times clone()
	{
		if(this.unlimited)
		{
			return Times.unlimited();
		}
		else
		{
			return Times.exactly(this.remainingTimes);
		}
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
		final Times times = (Times)o;
		return this.remainingTimes == times.remainingTimes
			&& this.unlimited == times.unlimited;
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(this.remainingTimes, this.unlimited);
		}
		return this.hashCode;
	}
}
