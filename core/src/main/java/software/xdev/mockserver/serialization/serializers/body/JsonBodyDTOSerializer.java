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
import software.xdev.mockserver.serialization.model.JsonBodyDTO;

import java.io.IOException;

import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.EXCEPTION;

public class JsonBodyDTOSerializer extends StdSerializer<JsonBodyDTO> {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();
    private final boolean serialiseDefaultValues;

    public JsonBodyDTOSerializer(boolean serialiseDefaultValues) {
        super(JsonBodyDTO.class);
        this.serialiseDefaultValues = serialiseDefaultValues;
    }

    @Override
    public void serialize(JsonBodyDTO jsonBodyDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        boolean notNonDefault = jsonBodyDTO.getNot() != null && jsonBodyDTO.getNot();
        boolean optionalNonDefault = jsonBodyDTO.getOptional() != null && jsonBodyDTO.getOptional();
        boolean contentTypeNonDefault = jsonBodyDTO.getContentType() != null && !jsonBodyDTO.getContentType().equals(JsonBody.DEFAULT_JSON_CONTENT_TYPE.toString());
        boolean matchTypeNonDefault = jsonBodyDTO.getMatchType() != JsonBody.DEFAULT_MATCH_TYPE;
        if (serialiseDefaultValues || notNonDefault || optionalNonDefault || contentTypeNonDefault || matchTypeNonDefault) {
            jgen.writeStartObject();
            if (notNonDefault) {
                jgen.writeBooleanField("not", jsonBodyDTO.getNot());
            }
            if (optionalNonDefault) {
                jgen.writeBooleanField("optional", jsonBodyDTO.getOptional());
            }
            if (contentTypeNonDefault) {
                jgen.writeStringField("contentType", jsonBodyDTO.getContentType());
            }
            jgen.writeStringField("type", jsonBodyDTO.getType().name());
            try {
                jgen.writeObjectField("json", OBJECT_MAPPER.readTree(jsonBodyDTO.getJson()));
            } catch (Throwable throwable) {
                new MockServerLogger().logEvent(
                    new LogEntry()
                        .setType(EXCEPTION)
                        .setMessageFormat("exception:{} while deserialising JsonBodyDTO with json:{}")
                        .setArguments(throwable.getMessage(), jsonBodyDTO.getJson())
                        .setThrowable(throwable)
                );
            }
            if (jsonBodyDTO.getRawBytes() != null) {
                jgen.writeObjectField("rawBytes", jsonBodyDTO.getRawBytes());
            }
            if (matchTypeNonDefault) {
                jgen.writeStringField("matchType", jsonBodyDTO.getMatchType().name());
            }
            jgen.writeEndObject();
        } else {
            jgen.writeObject(OBJECT_MAPPER.readTree(jsonBodyDTO.getJson()));
        }
    }
}
