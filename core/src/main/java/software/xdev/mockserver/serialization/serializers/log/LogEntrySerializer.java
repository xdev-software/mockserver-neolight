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
package software.xdev.mockserver.serialization.serializers.log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.log.model.LogEntry;

import java.io.IOException;

import static software.xdev.mockserver.character.Character.NEW_LINE;

public class LogEntrySerializer extends StdSerializer<LogEntry> {

    public LogEntrySerializer() {
        super(LogEntry.class);
    }

    @Override
    public void serialize(LogEntry logEntry, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (logEntry.getLogLevel() != null) {
            jgen.writeObjectField("logLevel", logEntry.getLogLevel());
        }
        if (logEntry.getTimestamp() != null) {
            jgen.writeObjectField("timestamp", logEntry.getTimestamp());
        }
        if (logEntry.getType() != null) {
            jgen.writeObjectField("type", logEntry.getType());
        }
        if (logEntry.getHttpRequests() != null) {
            if (logEntry.getHttpRequests().length > 1) {
                jgen.writeObjectField("httpRequests", logEntry.getHttpUpdatedRequests());
            } else if (logEntry.getHttpRequests().length == 1) {
                jgen.writeObjectField("httpRequest", logEntry.getHttpUpdatedRequests()[0]);
            }
        }
        if (logEntry.getHttpResponse() != null) {
            jgen.writeObjectField("httpResponse", logEntry.getHttpUpdatedResponse());
        }
        if (logEntry.getHttpError() != null) {
            jgen.writeObjectField("httpError", logEntry.getHttpError());
        }
        if (logEntry.getExpectation() != null) {
            jgen.writeObjectField("expectation", logEntry.getExpectation());
        }
        if (logEntry.getMessage() != null) {
            jgen.writeObjectField("message", logEntry.getMessage().replaceAll(" {2}", "   ").split(NEW_LINE));
        }
        if (logEntry.getThrowable() != null) {
            jgen.writeObjectField("throwable", logEntry.getThrowable());
        }
        jgen.writeEndObject();
    }
}
