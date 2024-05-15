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

import java.io.IOException;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static software.xdev.mockserver.character.Character.NEW_LINE;

public class ThrowableSerializer extends StdSerializer<Throwable> {
    public ThrowableSerializer() {
        super(Throwable.class);
    }

    @Override
    public void serialize(final Throwable value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        final String stackTrace = getStackTrace(value);
        final String[] lines = stackTrace.split(NEW_LINE);
        if (lines.length > 1) {
            gen.writeObject(lines);
        } else {
            gen.writeString(stackTrace);
        }
    }
}