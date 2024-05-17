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

import software.xdev.mockserver.event.EventBus;
import software.xdev.mockserver.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class MockServerEventLogNotifier {

    private boolean listenerAdded = false;
    private final List<MockServerLogListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final Scheduler scheduler;

    public MockServerEventLogNotifier(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    protected void notifyListeners(final EventBus notifier, boolean synchronous) {
        if (listenerAdded && !listeners.isEmpty()) {
            scheduler.submit(() -> {
                for (MockServerLogListener listener : listeners.toArray(new MockServerLogListener[0])) {
                    listener.updated(notifier);
                }
            }, synchronous);
        }
    }

    public void registerListener(MockServerLogListener listener) {
        listeners.add(listener);
        listenerAdded = true;
    }

    public void unregisterListener(MockServerLogListener listener) {
        listeners.remove(listener);
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
		return listenerAdded == that.listenerAdded && Objects.equals(listeners, that.listeners)
            && Objects.equals(scheduler, that.scheduler);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(listenerAdded, listeners, scheduler);
    }
}
