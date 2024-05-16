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
package software.xdev.mockserver.serialization.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.event.Level.ERROR;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class BodyDTO extends NotDTO implements DTO<Body<?>> {
    
    private static final Logger LOG = LoggerFactory.getLogger(BodyDTO.class);
    
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();
    private final Body.Type type;
    private Boolean optional;

    protected BodyDTO(Body.Type type, Boolean not) {
        super(not);
        this.type = type;
    }

    public static BodyDTO createDTO(Body<?> body) {
        BodyDTO result = null;

        if (body instanceof BinaryBody typedDTO) {
            result = new BinaryBodyDTO(typedDTO, typedDTO.getNot());
        } else if (body instanceof ParameterBody typedDTO) {
            result = new ParameterBodyDTO(typedDTO, typedDTO.getNot());
        } else if (body instanceof RegexBody typedDTO) {
            result = new RegexBodyDTO(typedDTO, typedDTO.getNot());
        } else if (body instanceof StringBody typedDTO) {
            result = new StringBodyDTO(typedDTO, typedDTO.getNot());
        }

        if (result != null) {
            result.withOptional(body.getOptional());
        }

        return result;
    }

    public static String toString(BodyDTO body) {
        if (body instanceof BinaryBodyDTO typedDTO) {
            return Base64.getEncoder().encodeToString(typedDTO.getBase64Bytes());
        } else if (body instanceof ParameterBodyDTO typedDTO) {
            try {
                return OBJECT_MAPPER.writeValueAsString(typedDTO.getParameters().getMultimap());
            } catch (Exception ex) {
                LOG.error("Serialising parameter body into json string for javascript template {}",
                    (isNotBlank(ex.getMessage()) ? " " + ex.getMessage() : ""),
                    ex);
                return "";
            }
        } else if (body instanceof RegexBodyDTO typedDTO) {
            return typedDTO.getRegex();
        } else if (body instanceof StringBodyDTO typedDTO) {
            return typedDTO.getString();
        }

        return "";
    }

    public Body.Type getType() {
        return type;
    }

    public Boolean getOptional() {
        return optional;
    }

    public BodyDTO withOptional(Boolean optional) {
        this.optional = optional;
        return this;
    }

    public abstract Body<?> buildObject();

}
