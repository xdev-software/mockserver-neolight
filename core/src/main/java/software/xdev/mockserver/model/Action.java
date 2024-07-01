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
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;


@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class Action<T extends Action> extends ObjectWithJsonToString
{
	private int hashCode;
	private Delay delay;
	private String expectationId;
	
	/**
	 * The delay before responding with this request as a Delay object, for example new Delay(TimeUnit.SECONDS, 3)
	 *
	 * @param delay a Delay object, for example new Delay(TimeUnit.SECONDS, 3)
	 */
	public T withDelay(final Delay delay)
	{
		this.delay = delay;
		this.hashCode = 0;
		return (T)this;
	}
	
	/**
	 * The delay before responding with this request as a Delay object, for example new Delay(TimeUnit.SECONDS, 3)
	 *
	 * @param timeUnit the time unit, for example TimeUnit.SECONDS
	 * @param value    the number of time units to delay the response
	 */
	public T withDelay(final TimeUnit timeUnit, final long value)
	{
		this.delay = new Delay(timeUnit, value);
		return (T)this;
	}
	
	public Delay getDelay()
	{
		return this.delay;
	}
	
	@JsonIgnore
	public String getExpectationId()
	{
		return this.expectationId;
	}
	
	@JsonIgnore
	public Action setExpectationId(final String expectationId)
	{
		this.expectationId = expectationId;
		return this;
	}
	
	@JsonIgnore
	public abstract Type getType();
	
	public enum Type
	{
		FORWARD(Direction.FORWARD),
		FORWARD_CLASS_CALLBACK(Direction.FORWARD),
		FORWARD_OBJECT_CALLBACK(Direction.FORWARD),
		FORWARD_REPLACE(Direction.FORWARD),
		RESPONSE(Direction.RESPONSE),
		RESPONSE_CLASS_CALLBACK(Direction.RESPONSE),
		RESPONSE_OBJECT_CALLBACK(Direction.RESPONSE),
		ERROR(Direction.RESPONSE);
		
		private final Direction direction;
		
		Type(final Direction direction)
		{
			this.direction = direction;
		}
		
		public Direction getDirection()
		{
			return this.direction;
		}
	}
	
	
	public enum Direction
	{
		FORWARD,
		RESPONSE
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
		final Action<?> action = (Action<?>)o;
		return Objects.equals(this.delay, action.delay);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(this.delay);
		}
		return this.hashCode;
	}
}
