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

import software.xdev.mockserver.matchers.MatchType;
import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.JsonBody;

public class JsonBodyDTO extends BodyWithContentTypeDTO {

    private final String json;
    private final MatchType matchType;
    private final byte[] rawBytes;

    public JsonBodyDTO(JsonBody jsonBody) {
        this(jsonBody, null);
    }

    public JsonBodyDTO(JsonBody jsonBody, Boolean not) {
        super(Body.Type.JSON, not, jsonBody);
        json = jsonBody.getValue();
        matchType = jsonBody.getMatchType();
        rawBytes = jsonBody.getRawBytes();
    }

    public String getJson() {
        return json;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public byte[] getRawBytes() {
        return rawBytes;
    }

    public JsonBody buildObject() {
        return (JsonBody) new JsonBody(getJson(), getRawBytes(), getMediaType(), getMatchType()).withOptional(getOptional());
    }
}
