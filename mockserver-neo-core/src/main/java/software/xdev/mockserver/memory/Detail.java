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
package software.xdev.mockserver.memory;

import software.xdev.mockserver.model.ObjectWithJsonToString;

public class Detail extends ObjectWithJsonToString {
    private long init;
    private long used;
    private long committed;
    private long max;

    public Detail plus(Detail detail) {
        return new Detail()
            .setInit(init + detail.init)
            .setUsed(used + detail.used)
            .setCommitted(committed + detail.committed)
            .setMax(max + detail.max);
    }

    public long getInit() {
        return init;
    }

    public Detail setInit(long init) {
        this.init = init;
        return this;
    }

    public long getUsed() {
        return used;
    }

    public Detail setUsed(long used) {
        this.used = used;
        return this;
    }

    public long getCommitted() {
        return committed;
    }

    public Detail setCommitted(long committed) {
        this.committed = committed;
        return this;
    }

    public long getMax() {
        return max;
    }

    public Detail setMax(long max) {
        this.max = max;
        return this;
    }
}