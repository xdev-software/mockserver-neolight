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
package software.xdev.mockserver.mock;

import software.xdev.mockserver.collections.Keyed;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class SortableExpectationId implements Keyed<String> {

    public static final SortableExpectationId NULL = new SortableExpectationId("", 0, 0);

    public static final Comparator<SortableExpectationId> EXPECTATION_SORTABLE_PRIORITY_COMPARATOR = (Comparator<SortableExpectationId> & Serializable) (expectationOne, expectationTwo) -> {
        if (expectationOne == null) {
            return expectationTwo == null ? 0 : 1;
        } else if (expectationTwo == null) {
            return -1;
        } else {
            int priorityComparison = Integer.compare(expectationTwo.priority, expectationOne.priority);
            if (priorityComparison != 0) {
                return priorityComparison;
            } else {
                int createdComparison = Long.compare(expectationOne.created, expectationTwo.created);
                if (createdComparison != 0) {
                    return createdComparison;
                } else {
                    return expectationOne.id.compareTo(expectationTwo.id);
                }
            }
        }
    };

    public final int hashCode;
    public final String id;
    public final int priority;
    public final long created;

    public SortableExpectationId(String id, int priority, long created) {
        this.id = id;
        this.priority = priority;
        this.created = created;
        this.hashCode = Objects.hash(id, priority, created);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SortableExpectationId that = (SortableExpectationId) o;
        return hashCode == that.hashCode &&
            priority == that.priority &&
            created == that.created &&
            Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String getKey() {
        return id;
    }
}
