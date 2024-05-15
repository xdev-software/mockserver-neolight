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
package software.xdev.mockserver.serialization.serializers.condition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.serialization.model.VerificationTimesDTO;

import java.io.IOException;

public class VerificationTimesDTOSerializer extends StdSerializer<VerificationTimesDTO> {

    public VerificationTimesDTOSerializer() {
        super(VerificationTimesDTO.class);
    }

    @Override
    public void serialize(VerificationTimesDTO verificationTimesDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (verificationTimesDTO.getAtLeast() != -1) {
            jgen.writeNumberField("atLeast", verificationTimesDTO.getAtLeast());
        }
        if (verificationTimesDTO.getAtMost() != -1) {
            jgen.writeNumberField("atMost", verificationTimesDTO.getAtMost());
        }
        jgen.writeEndObject();
    }
}