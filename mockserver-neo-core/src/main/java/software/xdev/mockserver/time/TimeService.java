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
package software.xdev.mockserver.time;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

public class TimeService {

    public static final Instant FIXED_INSTANT_FOR_TESTS = Instant.now();
    public static boolean fixedTime = false;

    public static Instant now() {
        if (!fixedTime) {
            return Instant.now();
        } else {
            return FIXED_INSTANT_FOR_TESTS;
        }
    }

    public static OffsetDateTime offsetNow() {
        Instant now = TimeService.now();
        return OffsetDateTime.ofInstant(now, Clock.systemDefaultZone().getZone().getRules().getOffset(now));
    }

}
