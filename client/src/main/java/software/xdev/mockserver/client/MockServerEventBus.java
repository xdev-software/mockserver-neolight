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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

/**
 * A publish/subscribe communication channel between {@link MockServerClient} and {@link ForwardChainExpectation} instances
 *
 * @author albans
 */
final class MockServerEventBus {
    private final Multimap<EventType, SubscriberHandler> subscribers = LinkedListMultimap.create();

    void publish(EventType event) {
        for (SubscriberHandler subscriber : subscribers.get(event)) {
            subscriber.handle();
        }
    }

    public void subscribe(SubscriberHandler subscriber, EventType... events) {
        for (EventType event : events) {
            subscribers.put(event, subscriber);
        }
    }

    enum EventType {
        STOP, RESET
    }

    interface SubscriberHandler {
        void handle();
    }
}
