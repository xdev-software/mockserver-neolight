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
package software.xdev.mockserver.matchers;

import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.validator.xmlschema.XmlSchemaValidator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * See http://xml-schema.org/
 */
public class XmlSchemaMatcher extends BodyMatcher<String> {
    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger", "xmlSchemaValidator"};
    private final MockServerLogger mockServerLogger;
    private String matcher;
    private XmlSchemaValidator xmlSchemaValidator;

    XmlSchemaMatcher(MockServerLogger mockServerLogger, String matcher) {
        this.mockServerLogger = mockServerLogger;
        this.matcher = matcher;
        xmlSchemaValidator = new XmlSchemaValidator(mockServerLogger, matcher);
    }

    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return EXCLUDED_FIELDS;
    }

    public boolean matches(final MatchDifference context, String matched) {
        boolean result = false;

        if (isNotBlank(matched)) {
            try {
                String validation = xmlSchemaValidator.isValid(matched);

                result = validation.isEmpty();

                if (!result && context != null) {
                    context.addDifference(mockServerLogger, "xml schema match failed expected:{}found:{}failed because:{}", this.matcher, matched, validation);
                }
            } catch (Throwable throwable) {
                if (context != null) {
                    context.addDifference(mockServerLogger, throwable, "xml schema match failed expected:{}found:{}failed because:{}", this.matcher, matched, throwable.getMessage());
                }
            }
        } else {
            if (context != null) {
                context.addDifference(mockServerLogger, "xml schema match failed expected:{}found:{}failed because xml is null or empty", this.matcher, matched);
            }
        }

        return not != result;
    }

    public boolean isBlank() {
        return StringUtils.isBlank(matcher);
    }

}
