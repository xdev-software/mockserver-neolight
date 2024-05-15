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
package software.xdev.mockserver.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.PortBinding;
import org.slf4j.event.Level;

@SuppressWarnings("FieldMayBeFinal")
public class PortBindingSerializer implements Serializer<PortBinding> {
    private final MockServerLogger mockServerLogger;
    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

    public PortBindingSerializer(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
    }

    public String serialize(PortBinding portBinding) {
        try {
            return objectWriter.writeValueAsString(portBinding);
        } catch (Throwable throwable) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception while serializing portBinding to JSON with value " + portBinding)
                    .setThrowable(throwable)
            );
            throw new RuntimeException("Exception while serializing portBinding to JSON with value " + portBinding, throwable);
        }
    }

    public PortBinding deserialize(String jsonPortBinding) {
        PortBinding portBinding = null;
        if (jsonPortBinding != null && !jsonPortBinding.isEmpty()) {
            try {
                portBinding = objectMapper.readValue(jsonPortBinding, PortBinding.class);
            } catch (Throwable throwable) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception while parsing{}for PortBinding " + throwable.getMessage())
                        .setArguments(jsonPortBinding)
                        .setThrowable(throwable)
                );
                throw new  IllegalArgumentException("exception while parsing PortBinding for [" + jsonPortBinding + "]", throwable);
            }
        }
        return portBinding;
    }

    @Override
    public Class<PortBinding> supportsType() {
        return PortBinding.class;
    }
}
