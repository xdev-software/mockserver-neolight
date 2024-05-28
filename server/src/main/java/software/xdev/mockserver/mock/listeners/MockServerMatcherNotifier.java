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
package software.xdev.mockserver.mock.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import software.xdev.mockserver.mock.RequestMatchers;
import software.xdev.mockserver.scheduler.Scheduler;


public class MockServerMatcherNotifier
{
	private boolean listenerAdded;
	private final List<MockServerMatcherListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private final Scheduler scheduler;
	
	public MockServerMatcherNotifier(final Scheduler scheduler)
	{
		this.scheduler = scheduler;
	}
	
	protected void notifyListeners(final RequestMatchers notifier, final Cause cause)
	{
		if(this.listenerAdded && !this.listeners.isEmpty())
		{
			for(final MockServerMatcherListener listener : this.listeners.toArray(new MockServerMatcherListener[0]))
			{
				this.scheduler.submit(() -> listener.updated(notifier, cause));
			}
		}
	}
	
	public void registerListener(final MockServerMatcherListener listener)
	{
		this.listeners.add(listener);
		this.listenerAdded = true;
	}
	
	public void unregisterListener(final MockServerMatcherListener listener)
	{
		this.listeners.remove(listener);
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final MockServerMatcherNotifier that))
		{
			return false;
		}
		return this.listenerAdded == that.listenerAdded
			&& Objects.equals(this.listeners, that.listeners)
			&& Objects.equals(this.scheduler, that.scheduler);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.listenerAdded, this.listeners, this.scheduler);
	}
	
	public static class Cause
	{
		public Cause(final String source, final Type type)
		{
			this.source = source;
			this.type = type;
		}
		
		public static final Cause API = new Cause("", Type.API);
		
		
		public enum Type
		{
			FILE_INITIALISER,
			CLASS_INITIALISER,
			API
		}
		
		
		private final String source;
		private final Type type;
		
		public String getSource()
		{
			return this.source;
		}
		
		public Type getType()
		{
			return this.type;
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
			final Cause cause = (Cause)o;
			return Objects.equals(this.source, cause.source) && this.type == cause.type;
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(this.source, this.type);
		}
	}
}
