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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.ParameterStyle;
import software.xdev.mockserver.validator.jsonschema.JsonSchemaValidator;

import java.util.Map;

/**
 * See http://json-schema.org/
 */
@SuppressWarnings("FieldMayBeFinal")
public class JsonSchemaMatcher extends BodyMatcher<String> {
    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger", "jsonSchemaValidator"};
    private final MockServerLogger mockServerLogger;
    private String matcher;
    private JsonSchemaValidator jsonSchemaValidator;
    private Map<String, ParameterStyle> parameterStyle;

    JsonSchemaMatcher(MockServerLogger mockServerLogger, String matcher) {
        this.mockServerLogger = mockServerLogger;
        this.matcher = matcher;
        jsonSchemaValidator = new JsonSchemaValidator(mockServerLogger, matcher);
    }

    public Map<String, ParameterStyle> getParameterStyle() {
        return parameterStyle;
    }

    public JsonSchemaMatcher withParameterStyle(Map<String, ParameterStyle> parameterStyle) {
        this.parameterStyle = parameterStyle;
        return this;
    }

    public boolean matches(final MatchDifference context, String matched) {
        boolean result = false;

        if (matcher.equalsIgnoreCase(matched)) {
            result = true;
        } else if (!StringUtils.isBlank(matched)) {
            try {
                String validation = jsonSchemaValidator.isValid(matched, false);

                result = validation.isEmpty();

                if (!result && context != null) {
                    context.addDifference(mockServerLogger, "json schema match failed expected:{}found:{}failed because:{}", this.matcher, matched, validation);
                }
            } catch (Throwable throwable) {
                if (context != null) {
                    context.addDifference(mockServerLogger, throwable, "json schema match failed expected:{}found:{}failed because:{}", this.matcher, matched, throwable.getMessage());
                }
            }
        }

        return not != result;
    }

    public boolean isBlank() {
        return StringUtils.isBlank(matcher);
    }

    @Override
    @JsonIgnore
    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return EXCLUDED_FIELDS;
    }
}
