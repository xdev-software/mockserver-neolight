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

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class RequestDefinition extends Not {
    
    private static final Logger LOG = LoggerFactory.getLogger(RequestDefinition.class);
    private String logCorrelationId;

    @JsonIgnore
    public String getLogCorrelationId() {
        return logCorrelationId;
    }

    public RequestDefinition withLogCorrelationId(String logCorrelationId) {
        this.logCorrelationId = logCorrelationId;
        return this;
    }

    public abstract RequestDefinition shallowClone();

    public RequestDefinition cloneWithLogCorrelationId() {
        return LOG.isTraceEnabled() && isNotBlank(getLogCorrelationId())
            ? shallowClone().withLogCorrelationId(getLogCorrelationId())
            : this;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
