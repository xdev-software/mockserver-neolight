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
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class TimeToLive
{
	private static final TimeToLive TIME_TO_LIVE_UNLIMITED = new TimeToLive(null, null, true)
	{
		@Override
		public boolean stillAlive()
		{
			return true;
		}
	};
	
	private int hashCode;
	private final TimeUnit timeUnit;
	private final Long timeToLive;
	private final boolean unlimited;
	private long endDate;
	
	private TimeToLive(final TimeUnit timeUnit, final Long timeToLive, final boolean unlimited)
	{
		this.timeUnit = timeUnit;
		this.timeToLive = timeToLive;
		this.unlimited = unlimited;
		if(!unlimited)
		{
			this.endDate = System.currentTimeMillis() + timeUnit.toMillis(timeToLive);
		}
	}
	
	public static TimeToLive unlimited()
	{
		return TIME_TO_LIVE_UNLIMITED;
	}
	
	public static TimeToLive exactly(final TimeUnit timeUnit, final Long timeToLive)
	{
		return new TimeToLive(timeUnit, timeToLive, false);
	}
	
	public TimeUnit getTimeUnit()
	{
		return this.timeUnit;
	}
	
	public Long getTimeToLive()
	{
		return this.timeToLive;
	}
	
	@JsonIgnore
	public long getEndDate()
	{
		return this.endDate;
	}
	
	public TimeToLive setEndDate(final long endDate)
	{
		this.endDate = endDate;
		return this;
	}
	
	public boolean isUnlimited()
	{
		return this.unlimited;
	}
	
	public boolean stillAlive()
	{
		return this.unlimited || this.isAfterNow(this.endDate);
	}
	
	private boolean isAfterNow(final long date)
	{
		return date > System.currentTimeMillis();
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
		final TimeToLive that = (TimeToLive)o;
		return this.unlimited == that.unlimited
			&& this.timeUnit == that.timeUnit
			&& Objects.equals(this.timeToLive, that.timeToLive);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(this.timeUnit, this.timeToLive, this.unlimited);
		}
		return this.hashCode;
	}
}
