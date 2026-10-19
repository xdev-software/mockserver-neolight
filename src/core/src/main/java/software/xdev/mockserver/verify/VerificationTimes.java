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
package software.xdev.mockserver.verify;

import java.util.Objects;


public final class VerificationTimes
{
	private final int atLeast;
	private final int atMost;
	
	private VerificationTimes(final int atLeast, final int atMost)
	{
		this.atMost = atMost;
		this.atLeast = atLeast;
	}
	
	public static VerificationTimes never()
	{
		return new VerificationTimes(0, 0);
	}
	
	public static VerificationTimes once()
	{
		return new VerificationTimes(1, 1);
	}
	
	public static VerificationTimes exactly(final int count)
	{
		return new VerificationTimes(count, count);
	}
	
	public static VerificationTimes atLeast(final int count)
	{
		return new VerificationTimes(count, -1);
	}
	
	public static VerificationTimes atMost(final int count)
	{
		return new VerificationTimes(-1, count);
	}
	
	public static VerificationTimes between(final int atLeast, final int atMost)
	{
		return new VerificationTimes(atLeast, atMost);
	}
	
	public int getAtLeast()
	{
		return this.atLeast;
	}
	
	public int getAtMost()
	{
		return this.atMost;
	}
	
	public boolean matches(final int times)
	{
		if(this.atLeast != -1 && times < this.atLeast)
		{
			return false;
		}
		else
		{
			return this.atMost == -1 || times <= this.atMost;
		}
	}
	
	@Override
	public String toString()
	{
		String string = "";
		if(this.atLeast == this.atMost)
		{
			string += "exactly ";
			if(this.atMost == 1)
			{
				string += "once";
			}
			else
			{
				string += this.atMost + " times";
			}
		}
		else if(this.atMost == -1)
		{
			string += "at least ";
			if(this.atLeast == 1)
			{
				string += "once";
			}
			else
			{
				string += this.atLeast + " times";
			}
		}
		else if(this.atLeast == -1)
		{
			string += "at most ";
			if(this.atMost == 1)
			{
				string += "once";
			}
			else
			{
				string += this.atMost + " times";
			}
		}
		else
		{
			string += "between " + this.atLeast + " and " + this.atMost + " times";
		}
		return string;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final VerificationTimes that))
		{
			return false;
		}
		return this.getAtLeast() == that.getAtLeast() && this.getAtMost() == that.getAtMost();
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getAtLeast(), this.getAtMost());
	}
}
