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

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.xdev.mockserver.model.*;

public abstract class BodyWithContentTypeDTO extends BodyDTO {

    protected final String contentType;

    public BodyWithContentTypeDTO(Body.Type type, Boolean not, Body<?> body) {
        super(type, not);
        this.contentType = body.getContentType();
        withOptional(body.getOptional());
    }

    public static BodyWithContentTypeDTO createWithContentTypeDTO(BodyWithContentType<?> body) {
        BodyWithContentTypeDTO result = null;

        if (body instanceof BinaryBody) {
            BinaryBody binaryBody = (BinaryBody) body;
            result = new BinaryBodyDTO(binaryBody, binaryBody.getNot());
        } else if (body instanceof JsonBody) {
            JsonBody jsonBody = (JsonBody) body;
            result = new JsonBodyDTO(jsonBody, jsonBody.getNot());
        } else if (body instanceof StringBody) {
            StringBody stringBody = (StringBody) body;
            result = new StringBodyDTO(stringBody, stringBody.getNot());
        } else if (body instanceof XmlBody) {
            XmlBody xmlBody = (XmlBody) body;
            result = new XmlBodyDTO(xmlBody, xmlBody.getNot());
        } else if (body instanceof LogEntryBody) {
            LogEntryBody logEventBody = (LogEntryBody) body;
            result = new LogEntryBodyDTO(logEventBody);
        }

        if (result != null) {
            result.withOptional(body.getOptional());
        }

        return result;
    }

    public String getContentType() {
        return contentType;
    }

    @JsonIgnore
    MediaType getMediaType() {
        return contentType != null ? MediaType.parse(contentType) : null;
    }

    public abstract BodyWithContentType<?> buildObject();

}
