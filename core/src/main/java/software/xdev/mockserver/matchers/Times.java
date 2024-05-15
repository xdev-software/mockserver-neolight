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

import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;

import java.util.Objects;

public class Times extends ObjectWithReflectiveEqualsHashCodeToString {

    private static final Times TIMES_UNLIMITED = new Times(-1, true) {
        public final int getRemainingTimes() {
            return -1;
        }

        public final boolean isUnlimited() {
            return true;
        }

        public final boolean greaterThenZero() {
            return true;
        }

        public final boolean decrement() {
            return false;
        }
    };

    private int hashCode;
    private int remainingTimes;
    private final boolean unlimited;

    private Times(int remainingTimes, boolean unlimited) {
        this.remainingTimes = remainingTimes;
        this.unlimited = unlimited;
    }

    public static Times unlimited() {
        return TIMES_UNLIMITED;
    }

    public static Times once() {
        return new Times(1, false);
    }

    public static Times exactly(int count) {
        return new Times(count, false);
    }

    public int getRemainingTimes() {
        return remainingTimes;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public boolean greaterThenZero() {
        return unlimited || remainingTimes > 0;
    }

    public boolean decrement() {
        if (!unlimited) {
            remainingTimes--;
            return true;
        }
        return false;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Times clone() {
        if (unlimited) {
            return Times.unlimited();
        } else {
            return Times.exactly(remainingTimes);
        }
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
        Times times = (Times) o;
        return remainingTimes == times.remainingTimes &&
            unlimited == times.unlimited;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(remainingTimes, unlimited);
        }
        return hashCode;
    }
}
