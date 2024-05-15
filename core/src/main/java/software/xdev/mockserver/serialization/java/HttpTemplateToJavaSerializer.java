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
package software.xdev.mockserver.serialization.java;

import com.google.common.base.Strings;
import org.apache.commons.text.StringEscapeUtils;
import software.xdev.mockserver.model.HttpTemplate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

public class HttpTemplateToJavaSerializer implements ToJavaSerializer<HttpTemplate> {

    @Override
    public String serialize(int numberOfSpacesToIndent, HttpTemplate httpTemplate) {
        StringBuffer output = new StringBuffer();
        if (httpTemplate != null) {
            appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("template(HttpTemplate.TemplateType.").append(httpTemplate.getTemplateType().name()).append(")");
            if (isNotBlank(httpTemplate.getTemplate())) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withTemplate(\"").append(StringEscapeUtils.escapeJava(httpTemplate.getTemplate())).append("\")");
            }
            if (httpTemplate.getDelay() != null) {
                appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withDelay(").append(new DelayToJavaSerializer().serialize(0, httpTemplate.getDelay())).append(")");
            }
        }

        return output.toString();
    }

    private StringBuffer appendNewLineAndIndent(int numberOfSpacesToIndent, StringBuffer output) {
        return output.append(NEW_LINE).append(Strings.padStart("", numberOfSpacesToIndent, ' '));
    }
}
