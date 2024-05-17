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

import java.util.Objects;

import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.verify.VerificationTimes;

public class VerificationTimesDTO extends ObjectWithJsonToString implements DTO<VerificationTimes> {

    private int atLeast;
    private int atMost;

    public VerificationTimesDTO(VerificationTimes times) {
        atLeast = times.getAtLeast();
        atMost = times.getAtMost();
    }

    public VerificationTimesDTO() {
    }

    public VerificationTimes buildObject() {
        return VerificationTimes.between(atLeast, atMost);
    }

    public int getAtLeast() {
        return atLeast;
    }

    public int getAtMost() {
        return atMost;
    }
    
    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final VerificationTimesDTO that))
        {
            return false;
        }
		return getAtLeast() == that.getAtLeast() && getAtMost() == that.getAtMost();
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(getAtLeast(), getAtMost());
    }
}
