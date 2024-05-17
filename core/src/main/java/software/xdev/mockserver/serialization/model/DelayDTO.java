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

import software.xdev.mockserver.model.Delay;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DelayDTO implements DTO<Delay> {

    private TimeUnit timeUnit;
    private long value;

    public DelayDTO(Delay delay) {
        if (delay != null) {
            timeUnit = delay.getTimeUnit();
            value = delay.getValue();
        }
    }

    public DelayDTO() {
    }

    public Delay buildObject() {
        return new Delay(timeUnit, value);
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public DelayDTO setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    public long getValue() {
        return value;
    }

    public DelayDTO setValue(long value) {
        this.value = value;
        return this;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final DelayDTO delayDTO))
        {
            return false;
        }
        if(!super.equals(o))
        {
            return false;
        }
		return getValue() == delayDTO.getValue() && getTimeUnit() == delayDTO.getTimeUnit();
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), getTimeUnit(), getValue());
    }
}
