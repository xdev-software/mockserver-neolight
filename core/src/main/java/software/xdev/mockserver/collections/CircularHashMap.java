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
package software.xdev.mockserver.collections;

import java.util.LinkedHashMap;
import java.util.Map;

public class CircularHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public CircularHashMap(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }

    public K findKey(V value) {
        for (Map.Entry<K, V> entry : entrySet()) {
            V entryValue = entry.getValue();
            if (entryValue == value || (value != null && value.equals(entryValue))) {
                return entry.getKey();
            }
        }
        return null;
    }
}
