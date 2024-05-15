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

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.xdev.mockserver.matchers.TimeToLive;
import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;

import java.util.concurrent.TimeUnit;

public class TimeToLiveDTO extends ObjectWithReflectiveEqualsHashCodeToString implements DTO<TimeToLive> {

    private static final String[] EXCLUDED_FIELDS = {"endDate"};
    private TimeUnit timeUnit;
    private Long timeToLive;
    private Long endDate;
    private boolean unlimited;

    public TimeToLiveDTO(TimeToLive timeToLive) {
        this.timeUnit = timeToLive.getTimeUnit();
        this.timeToLive = timeToLive.getTimeToLive();
        this.endDate = timeToLive.getEndDate();
        this.unlimited = timeToLive.isUnlimited();
    }

    public TimeToLiveDTO() {
    }


    public TimeToLive buildObject() {
        if (unlimited) {
            return TimeToLive.unlimited();
        } else {
            TimeToLive exactly = TimeToLive.exactly(timeUnit, timeToLive);
            if (this.endDate != null) {
                exactly.setEndDate(this.endDate);
            }
            return exactly;
        }
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Long getTimeToLive() {
        return timeToLive;
    }

    public Long getEndDate() {
        return endDate;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    @Override
    @JsonIgnore
    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return EXCLUDED_FIELDS;
    }
}
