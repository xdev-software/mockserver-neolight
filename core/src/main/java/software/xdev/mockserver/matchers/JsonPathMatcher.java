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
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.event.Level.DEBUG;

/**
 * See https://github.com/json-path/JsonPath
 */
public class JsonPathMatcher extends BodyMatcher<String> {
    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger", "jsonPath"};
    private final MockServerLogger mockServerLogger;
    private final String matcher;
    private JsonPath jsonPath;

    JsonPathMatcher(MockServerLogger mockServerLogger, String matcher) {
        this.mockServerLogger = mockServerLogger;
        this.matcher = matcher;
        if (isNotBlank(matcher)) {
            try {
                jsonPath = JsonPath.compile(matcher);
            } catch (Throwable throwable) {
                if (MockServerLogger.isEnabled(DEBUG) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(DEBUG)
                            .setMessageFormat("error while creating xpath expression for [" + matcher + "] assuming matcher not xpath - " + throwable.getMessage())
                            .setArguments(throwable)
                    );
                }
            }
        }
    }

    public boolean matches(final MatchDifference context, final String matched) {
        boolean result = false;
        boolean alreadyLoggedMatchFailure = false;

        if (jsonPath == null) {
            if (context != null) {
                context.addDifference(mockServerLogger, "json path match failed expected:{}found:{}failed because:{}", "null", matched, "json path matcher was null");
                alreadyLoggedMatchFailure = true;
            }
        } else if (matcher.equals(matched)) {
            result = true;
        } else if (matched != null) {
            try {
                result = !jsonPath.<JSONArray>read(matched).isEmpty();
            } catch (Throwable throwable) {
                if (context != null) {
                    context.addDifference(mockServerLogger, throwable, "json path match failed expected:{}found:{}failed because:{}", matcher, matched, throwable.getMessage());
                    alreadyLoggedMatchFailure = true;
                }
            }
        }

        if (!result && !alreadyLoggedMatchFailure && context != null) {
            context.addDifference(mockServerLogger, "json path match failed expected:{}found:{}failed because:{}", matcher, matched, "json path did not evaluate to truthy");
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
