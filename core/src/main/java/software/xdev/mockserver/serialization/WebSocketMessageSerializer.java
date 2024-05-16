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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.serialization.model.WebSocketMessageDTO;

import java.io.IOException;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked", "FieldMayBeFinal"})
public class WebSocketMessageSerializer {

    private ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private Map<Class, Serializer> serializers;

    public WebSocketMessageSerializer() {
        serializers = Map.of(
            HttpRequest.class, new HttpRequestSerializer(),
            HttpResponse.class, new HttpResponseSerializer(),
            HttpRequestAndHttpResponse.class, new HttpRequestAndHttpResponseSerializer()
        );
    }

    public String serialize(Object message) throws JsonProcessingException {
        if (serializers.containsKey(message.getClass())) {
            WebSocketMessageDTO value = new WebSocketMessageDTO().setType(message.getClass().getName()).setValue(serializers.get(message.getClass()).serialize((message)));
            return objectWriter.writeValueAsString(value);
        } else {
            return objectWriter.writeValueAsString(new WebSocketMessageDTO().setType(message.getClass().getName()).setValue(objectMapper.writeValueAsString(message)));
        }
    }

    public Object deserialize(String messageJson) throws ClassNotFoundException, IOException {
        WebSocketMessageDTO webSocketMessageDTO = objectMapper.readValue(messageJson, WebSocketMessageDTO.class);
        if (webSocketMessageDTO.getType() != null && webSocketMessageDTO.getValue() != null) {
            Class format = Class.forName(webSocketMessageDTO.getType());
            if (serializers.containsKey(format)) {
                return serializers.get(format).deserialize(webSocketMessageDTO.getValue());
            } else {
                return objectMapper.readValue(webSocketMessageDTO.getValue(), format);
            }
        } else {
            return null;
        }
    }
}
