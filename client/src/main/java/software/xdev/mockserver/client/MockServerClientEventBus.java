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
package software.xdev.mockserver.client;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * A publish/subscribe communication channel between {@link MockServerClient} and {@link ForwardChainExpectation}
 * instances
 */
class MockServerClientEventBus
{
	private final Map<EventType, Set<SubscriberHandler>> subscribers = new LinkedHashMap<>();
	
	void publish(final EventType event)
	{
		for(final SubscriberHandler subscriber : this.subscribers.get(event))
		{
			subscriber.handle();
		}
	}
	
	public void subscribe(final SubscriberHandler subscriber, final EventType... events)
	{
		for(final EventType event : events)
		{
			this.subscribers.computeIfAbsent(event, x -> new LinkedHashSet<>()).add(subscriber);
		}
	}
	
	enum EventType
	{
		STOP, RESET
	}
	
	
	interface SubscriberHandler
	{
		void handle();
	}
}
