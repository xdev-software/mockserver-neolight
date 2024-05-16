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
package software.xdev.mockserver.serialization.deserializers.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.serialization.model.BodyDTO;
import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import software.xdev.mockserver.serialization.model.RequestDefinitionDTO;

import java.io.IOException;

import static software.xdev.mockserver.model.NottableString.string;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RequestDefinitionDTODeserializer extends StdDeserializer<RequestDefinitionDTO> {
    
    private static final Logger LOG = LoggerFactory.getLogger(RequestDefinitionDTODeserializer.class);
    
    public RequestDefinitionDTODeserializer() {
        super(RequestDefinitionDTO.class);
    }

    @Override
    public RequestDefinitionDTO deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
            return null;
        }
        
        Boolean not = null;
        NottableString method = string("");
        NottableString path = string("");
        Parameters pathParameters = null;
        Parameters queryStringParameters = null;
        BodyDTO body = null;
        Cookies cookies = null;
        Headers headers = null;
        Boolean keepAlive = null;
        Protocol protocol = null;
        SocketAddress socketAddress = null;
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonParser.currentName();
            if (fieldName != null) {
                switch (fieldName) {
                    case "not": {
                        jsonParser.nextToken();
                        not = jsonParser.getBooleanValue();
                        break;
                    }
                    case "method": {
                        jsonParser.nextToken();
                        method = ctxt.readValue(jsonParser, NottableString.class);
                        break;
                    }
                    case "path": {
                        jsonParser.nextToken();
                        path = ctxt.readValue(jsonParser, NottableString.class);
                        break;
                    }
                    case "pathParameters": {
                        jsonParser.nextToken();
                        pathParameters = ctxt.readValue(jsonParser, Parameters.class);
                        break;
                    }
                    case "queryStringParameters": {
                        jsonParser.nextToken();
                        queryStringParameters = ctxt.readValue(jsonParser, Parameters.class);
                        break;
                    }
                    case "body": {
                        jsonParser.nextToken();
                        body = ctxt.readValue(jsonParser, BodyDTO.class);
                        break;
                    }
                    case "cookies": {
                        jsonParser.nextToken();
                        cookies = ctxt.readValue(jsonParser, Cookies.class);
                        break;
                    }
                    case "headers": {
                        jsonParser.nextToken();
                        headers = ctxt.readValue(jsonParser, Headers.class);
                        break;
                    }
                    case "keepAlive": {
                        jsonParser.nextToken();
                        keepAlive = ctxt.readValue(jsonParser, Boolean.class);
                        break;
                    }
                    case "socketAddress": {
                        jsonParser.nextToken();
                        socketAddress = ctxt.readValue(jsonParser, SocketAddress.class);
                        break;
                    }
                    case "protocol": {
                        jsonParser.nextToken();
                        try {
                            protocol = Protocol.valueOf(ctxt.readValue(jsonParser, String.class));
                        } catch (Exception ex) {
                            LOG.error("Exception while parsing protocol value for RequestDefinitionDTO", ex);
                        }
                        break;
                    }
                }
            }
        }
        return (RequestDefinitionDTO) new HttpRequestDTO()
            .setMethod(method)
            .setPath(path)
            .setPathParameters(pathParameters)
            .setQueryStringParameters(queryStringParameters)
            .setBody(body)
            .setCookies(cookies)
            .setHeaders(headers)
            .setKeepAlive(keepAlive)
            .setProtocol(protocol)
            .setSocketAddress(socketAddress)
            .setNot(not);
    }
}
