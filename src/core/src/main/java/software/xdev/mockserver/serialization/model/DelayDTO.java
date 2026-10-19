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

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import software.xdev.mockserver.model.Delay;


public class DelayDTO implements DTO<Delay>
{
	private TimeUnit timeUnit;
	private long value;
	
	public DelayDTO(final Delay delay)
	{
		if(delay != null)
		{
			this.timeUnit = delay.getTimeUnit();
			this.value = delay.getValue();
		}
	}
	
	public DelayDTO()
	{
	}
	
	@Override
	public Delay buildObject()
	{
		return new Delay(this.timeUnit, this.value);
	}
	
	public TimeUnit getTimeUnit()
	{
		return this.timeUnit;
	}
	
	public DelayDTO setTimeUnit(final TimeUnit timeUnit)
	{
		this.timeUnit = timeUnit;
		return this;
	}
	
	public long getValue()
	{
		return this.value;
	}
	
	public DelayDTO setValue(final long value)
	{
		this.value = value;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final DelayDTO delayDTO))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return this.getValue() == delayDTO.getValue() && this.getTimeUnit() == delayDTO.getTimeUnit();
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.getTimeUnit(), this.getValue());
	}
}
