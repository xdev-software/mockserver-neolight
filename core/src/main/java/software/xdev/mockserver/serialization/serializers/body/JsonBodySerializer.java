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
package software.xdev.mockserver.serialization.serializers.body;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.JsonBody;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.io.IOException;

import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.EXCEPTION;

public class JsonBodySerializer extends StdSerializer<JsonBody> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();
    private final boolean serialiseDefaultValues;

    public JsonBodySerializer(boolean serialiseDefaultValues) {
        super(JsonBody.class);
        this.serialiseDefaultValues = serialiseDefaultValues;
    }

    @Override
    public void serialize(JsonBody jsonBody, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        boolean notNonDefault = jsonBody.getNot() != null && jsonBody.getNot();
        boolean optionalNonDefault = jsonBody.getOptional() != null && jsonBody.getOptional();
        boolean contentTypeNonDefault = jsonBody.getContentType() != null && !jsonBody.getContentType().equals(JsonBody.DEFAULT_JSON_CONTENT_TYPE.toString());
        boolean matchTypeNonDefault = jsonBody.getMatchType() != JsonBody.DEFAULT_MATCH_TYPE;
        if (serialiseDefaultValues || notNonDefault || optionalNonDefault || contentTypeNonDefault || matchTypeNonDefault) {
            jgen.writeStartObject();
            if (notNonDefault) {
                jgen.writeBooleanField("not", jsonBody.getNot());
            }
            if (optionalNonDefault) {
                jgen.writeBooleanField("optional", jsonBody.getOptional());
            }
            if (contentTypeNonDefault) {
                jgen.writeStringField("contentType", jsonBody.getContentType());
            }
            jgen.writeStringField("type", jsonBody.getType().name());
            try {
                jgen.writeObjectField("json", OBJECT_MAPPER.readTree(jsonBody.getValue()));
            } catch (Throwable throwable) {
                new MockServerLogger().logEvent(
                    new LogEntry()
                        .setType(EXCEPTION)
                        .setMessageFormat("exception:{} while deserialising jsonBody with json:{}")
                        .setArguments(throwable.getMessage(), jsonBody.getValue())
                        .setThrowable(throwable)
                );
            }
            if (matchTypeNonDefault) {
                jgen.writeStringField("matchType", jsonBody.getMatchType().name());
            }
            jgen.writeEndObject();
        } else {
            jgen.writeObject(OBJECT_MAPPER.readTree(jsonBody.getValue()));
        }
    }
}
