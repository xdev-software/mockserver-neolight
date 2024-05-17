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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TimeToLive {

    private static final TimeToLive TIME_TO_LIVE_UNLIMITED = new TimeToLive(null, null, true) {
        public boolean stillAlive() {
            return true;
        }
    };

    private int hashCode;
    private final TimeUnit timeUnit;
    private final Long timeToLive;
    private final boolean unlimited;
    private long endDate;

    private TimeToLive(TimeUnit timeUnit, Long timeToLive, boolean unlimited) {
        this.timeUnit = timeUnit;
        this.timeToLive = timeToLive;
        this.unlimited = unlimited;
        if (!unlimited) {
            endDate = System.currentTimeMillis() + timeUnit.toMillis(timeToLive);
        }
    }

    public static TimeToLive unlimited() {
        return TIME_TO_LIVE_UNLIMITED;
    }

    public static TimeToLive exactly(TimeUnit timeUnit, Long timeToLive) {
        return new TimeToLive(timeUnit, timeToLive, false);
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Long getTimeToLive() {
        return timeToLive;
    }

    @JsonIgnore
    public long getEndDate() {
        return endDate;
    }

    public TimeToLive setEndDate(long endDate) {
        this.endDate = endDate;
        return this;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public boolean stillAlive() {
        return unlimited || isAfterNow(endDate);
    }

    private boolean isAfterNow(long date) {
        return date > System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (hashCode() != o.hashCode()) {
            return false;
        }
        TimeToLive that = (TimeToLive) o;
        return unlimited == that.unlimited &&
            timeUnit == that.timeUnit &&
            Objects.equals(timeToLive, that.timeToLive);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(timeUnit, timeToLive, unlimited);
        }
        return hashCode;
    }
}
