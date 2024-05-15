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
package software.xdev.mockserver.serialization.serializers.collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.model.Cookie;
import software.xdev.mockserver.model.Cookies;

import java.io.IOException;

import static software.xdev.mockserver.model.NottableString.serialiseNottableString;

public class CookiesSerializer extends StdSerializer<Cookies> {

    public CookiesSerializer() {
        super(Cookies.class);
    }

    @Override
    public void serialize(Cookies collection, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        for (Cookie cookie : collection.getEntries()) {
            jgen.writeObjectField(serialiseNottableString(cookie.getName()), cookie.getValue());
        }
        jgen.writeEndObject();
    }

}
