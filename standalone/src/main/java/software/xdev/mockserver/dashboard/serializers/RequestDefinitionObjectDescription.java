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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

public class RequestDefinitionObjectDescription implements Description {
    private final String first;
    private final Object object;
    private final String second;
    private final DescriptionProcessor descriptionProcessor;

    public RequestDefinitionObjectDescription(String first, Object object, String second, DescriptionProcessor descriptionProcessor) {
        this.first = first;
        this.object = object;
        this.second = second;
        this.descriptionProcessor = descriptionProcessor;
    }

    public int length() {
        return first.length() + 8 + second.length() + 1;
    }

    public Object toObject() {
        return ImmutableMap.of(
            "json", true,
            "object", object,
            "first", first,
            "second", StringUtils.repeat(" ", descriptionProcessor.getMaxOpenAPIObjectLength() - length() + 1) + second
        );
    }
}