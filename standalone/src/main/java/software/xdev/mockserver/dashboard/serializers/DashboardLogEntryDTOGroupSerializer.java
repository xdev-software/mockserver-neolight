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
package software.xdev.mockserver.dashboard.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.dashboard.model.DashboardLogEntryDTO;
import software.xdev.mockserver.dashboard.model.DashboardLogEntryDTOGroup;

import java.io.IOException;

public class DashboardLogEntryDTOGroupSerializer extends StdSerializer<DashboardLogEntryDTOGroup> {

    public DashboardLogEntryDTOGroupSerializer() {
        super(DashboardLogEntryDTOGroup.class);
    }

    @Override
    public void serialize(DashboardLogEntryDTOGroup logEntryGroup, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
        if (logEntryGroup.getLogEntryDTOS().size() > 1) {
            if (logEntryGroup.getLogEntryDTOS() != null) {
                jsonGenerator.writeStartObject();
                DashboardLogEntryDTO firstLogEntry = logEntryGroup.getLogEntryDTOS().get(0);
                jsonGenerator.writeObjectField("key", firstLogEntry.getId() + "_log_group");
                jsonGenerator.writeObjectField("group", new DashboardLogEntryDTO(firstLogEntry.getId(), firstLogEntry.getCorrelationId(), firstLogEntry.getTimestamp(), firstLogEntry.getType()).setDescription(logEntryGroup.getDescriptionProcessor().description(firstLogEntry)));
                jsonGenerator.writeObjectField("value", logEntryGroup.getLogEntryDTOS());
                jsonGenerator.writeEndObject();
            }
        } else if (logEntryGroup.getLogEntryDTOS().size() == 1) {
            jsonGenerator.writeObject(logEntryGroup.getLogEntryDTOS().get(0));
        }
    }
}
