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

import org.apache.commons.lang3.StringUtils;

public class LogMessageDescription implements Description {
    final String firstPart;
    final String secondPart;
    final int length;
    final DescriptionProcessor descriptionProcessor;

    public LogMessageDescription(String firstPart, String secondPart, DescriptionProcessor descriptionProcessor) {
        this.firstPart = firstPart;
        this.secondPart = secondPart;
        this.length = firstPart.length() + secondPart.length();
        this.descriptionProcessor = descriptionProcessor;
    }

    public int length() {
        return length + 1;
    }

    public String toObject() {
        return firstPart + " " + secondPart + StringUtils.repeat(" ", descriptionProcessor.getMaxLogEventLength() - length + 1) + " ";
    }
}