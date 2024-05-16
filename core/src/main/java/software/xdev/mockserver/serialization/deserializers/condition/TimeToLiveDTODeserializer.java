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
package software.xdev.mockserver.serialization.deserializers.condition;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import software.xdev.mockserver.matchers.TimeToLive;
import software.xdev.mockserver.serialization.model.TimeToLiveDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TimeToLiveDTODeserializer extends StdDeserializer<TimeToLiveDTO> {
    
    private static final Logger LOG = LoggerFactory.getLogger(TimeToLiveDTODeserializer.class);
    
    public TimeToLiveDTODeserializer() {
        super(TimeToLiveDTO.class);
    }

    @Override
    public TimeToLiveDTO deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        TimeToLiveDTO timeToLiveDTO = null;
        TimeToLive timeToLive = null;
        TimeUnit timeUnit;
        long ttl = 0L;
        long endDate;
        boolean unlimited = false;

        JsonNode timeToLiveDTONode = jsonParser.getCodec().readTree(jsonParser);
        JsonNode unlimitedNode = timeToLiveDTONode.get("unlimited");
        if (unlimitedNode != null) {
            unlimited = unlimitedNode.asBoolean();
        }
        if (!unlimited) {
            JsonNode timeToLiveNode = timeToLiveDTONode.get("timeToLive");
            if (timeToLiveNode != null) {
                ttl = timeToLiveNode.asLong();
            }
            JsonNode timeUnitNode = timeToLiveDTONode.get("timeUnit");
            if (timeUnitNode != null) {
                try {
                    timeUnit = Enum.valueOf(TimeUnit.class, timeUnitNode.asText());
                    timeToLive = TimeToLive.exactly(timeUnit, ttl);
                } catch (IllegalArgumentException iae) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Exception parsing TimeToLiveDTO timeUnit", iae);
                    }
                }
            }
            if (timeToLive != null) {
                JsonNode endDateNode = timeToLiveDTONode.get("endDate");
                if (endDateNode != null) {
                    endDate = endDateNode.asLong();
                    timeToLive.setEndDate(endDate);
                }
                timeToLiveDTO = new TimeToLiveDTO(timeToLive);
            }
        } else {
            timeToLiveDTO = new TimeToLiveDTO(TimeToLive.unlimited());
        }

        return timeToLiveDTO;
    }
}
