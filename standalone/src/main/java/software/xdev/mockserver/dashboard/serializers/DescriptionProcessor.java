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

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.dashboard.model.DashboardLogEntryDTO;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DescriptionProcessor {

    private static final MockServerLogger MOCK_SERVER_LOGGER = new MockServerLogger();
    private int maxHttpRequestLength;
    private int maxOpenAPILength;
    private int maxOpenAPIObjectLength;
    private int maxLogEventLength;

    public int getMaxHttpRequestLength() {
        return maxHttpRequestLength;
    }

    public int getMaxOpenAPILength() {
        return maxOpenAPILength;
    }

    public int getMaxOpenAPIObjectLength() {
        return maxOpenAPIObjectLength;
    }

    public int getMaxLogEventLength() {
        return maxLogEventLength;
    }

    public Description description(Object object) {
        return description(object, null);
    }

    public Description description(Object object, String id) {
        Description description = null;
        String idMessage = isNotBlank(id) ? id + ": " : "";
        if (object instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) object;
            description = new RequestDefinitionDescription(idMessage + httpRequest.getMethod().getValue(), httpRequest.getPath().getValue(), this, false);
            if (description.length() >= maxHttpRequestLength) {
                maxHttpRequestLength = description.length();
            }
        } else if (object instanceof DashboardLogEntryDTO) {
            DashboardLogEntryDTO logEntryDTO = (DashboardLogEntryDTO) object;
            description = new LogMessageDescription(idMessage + StringUtils.substringAfter(logEntryDTO.getTimestamp(), "-"), logEntryDTO.getType() != null ? logEntryDTO.getType().name() : "", this);
            if (description.length() >= maxLogEventLength) {
                maxLogEventLength = description.length();
            }
        }

        return description;
    }
}
