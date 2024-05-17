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

import software.xdev.mockserver.event.EventBus;
import software.xdev.mockserver.scheduler.Scheduler;


public class MockServerEventLogNotifier
{
	private boolean listenerAdded;
	private final List<MockServerLogListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private final Scheduler scheduler;
	
	public MockServerEventLogNotifier(final Scheduler scheduler)
	{
		this.scheduler = scheduler;
	}
	
	protected void notifyListeners(final EventBus notifier, final boolean synchronous)
	{
		if(this.listenerAdded && !this.listeners.isEmpty())
		{
			this.scheduler.submit(() -> {
				for(final MockServerLogListener listener : this.listeners.toArray(new MockServerLogListener[0]))
				{
					listener.updated(notifier);
				}
			}, synchronous);
		}
	}
	
	public void registerListener(final MockServerLogListener listener)
	{
		this.listeners.add(listener);
		this.listenerAdded = true;
	}
	
	public void unregisterListener(final MockServerLogListener listener)
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
		if(!(o instanceof final MockServerEventLogNotifier that))
		{
			return false;
		}
		return this.listenerAdded == that.listenerAdded && Objects.equals(this.listeners, that.listeners)
			&& Objects.equals(this.scheduler, that.scheduler);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.listenerAdded, this.listeners, this.scheduler);
	}
}
