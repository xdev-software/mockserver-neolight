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

public class Delay {

    private final TimeUnit timeUnit;
    private final long value;

    public static Delay milliseconds(long value) {
        return new Delay(TimeUnit.MILLISECONDS, value);
    }

    public static Delay seconds(long value) {
        return new Delay(TimeUnit.SECONDS, value);
    }

    public static Delay minutes(long value) {
        return new Delay(TimeUnit.MINUTES, value);
    }

    public static Delay delay(TimeUnit timeUnit, long value) {
        return new Delay(timeUnit, value);
    }

    public Delay(TimeUnit timeUnit, long value) {
        this.timeUnit = timeUnit;
        this.value = value;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getValue() {
        return value;
    }

    public void applyDelay() {
        if (timeUnit != null) {
            try {
                timeUnit.sleep(value);
            } catch (InterruptedException ie) {
                throw new RuntimeException("InterruptedException while apply delay to response", ie);
            }
        }
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final Delay delay))
        {
            return false;
        }
		return getValue() == delay.getValue() && getTimeUnit() == delay.getTimeUnit();
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(getTimeUnit(), getValue());
    }
}
