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
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.NottableSchemaString;
import software.xdev.mockserver.model.NottableString;

import java.util.regex.PatternSyntaxException;

import static software.xdev.mockserver.model.NottableString.string;
import static org.slf4j.event.Level.DEBUG;

public class RegexStringMatcher extends BodyMatcher<NottableString> {

    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger"};
    private final MockServerLogger mockServerLogger;
    private final NottableString matcher;
    private final boolean controlPlaneMatcher;

    public RegexStringMatcher(MockServerLogger mockServerLogger, boolean controlPlaneMatcher) {
        this.mockServerLogger = mockServerLogger;
        this.controlPlaneMatcher = controlPlaneMatcher;
        this.matcher = null;
    }

    RegexStringMatcher(MockServerLogger mockServerLogger, NottableString matcher, boolean controlPlaneMatcher) {
        this.mockServerLogger = mockServerLogger;
        this.controlPlaneMatcher = controlPlaneMatcher;
        this.matcher = matcher;
    }

    public boolean matches(String matched) {
        return matches((MatchDifference) null, string(matched));
    }

    public boolean matches(final MatchDifference context, NottableString matched) {
        boolean result = matcher == null || matches(mockServerLogger, context, matcher, matched);
        return not != result;
    }

    public boolean matches(NottableString matcher, NottableString matched) {
        return matches(mockServerLogger, null, matcher, matched);
    }

    public boolean matches(MockServerLogger mockServerLogger, MatchDifference context, NottableString matcher, NottableString matched) {
        if (matcher instanceof NottableSchemaString && matched instanceof NottableSchemaString) {
            return controlPlaneMatcher && matchesByNottedStrings(mockServerLogger, context, matcher, matched);
        } else if (matcher instanceof NottableSchemaString) {
            return matchesBySchemas(mockServerLogger, context, (NottableSchemaString) matcher, matched);
        } else if (matched instanceof NottableSchemaString) {
            return controlPlaneMatcher && matchesBySchemas(mockServerLogger, context, (NottableSchemaString) matched, matcher);
        } else {
            return matchesByNottedStrings(mockServerLogger, context, matcher, matched);
        }
    }

    private boolean matchesByNottedStrings(MockServerLogger mockServerLogger, MatchDifference context, NottableString matcher, NottableString matched) {
        if (matcher.isNot() && matched.isNot()) {
            // mutual notted control plane match
            return matchesByStrings(mockServerLogger, context, matcher, matched);
        } else {
            // data plane & control plan match
            return (matcher.isNot() || matched.isNot()) ^ matchesByStrings(mockServerLogger, context, matcher, matched);
        }
    }

    private boolean matchesBySchemas(MockServerLogger mockServerLogger, MatchDifference context, NottableSchemaString schema, NottableString string) {
        return string.isNot() != schema.matches(mockServerLogger, context, string.getValue());
    }

    private boolean matchesByStrings(MockServerLogger mockServerLogger, MatchDifference context, NottableString matcher, NottableString matched) {
        if (matcher == null) {
            return true;
        }
        final String matcherValue = matcher.getValue();
        if (StringUtils.isBlank(matcherValue)) {
            return true;
        } else {
            if (matched != null) {
                final String matchedValue = matched.getValue();
                if (matchedValue != null) {
                    // match as exact string
                    if (matchedValue.equals(matcherValue) || matchedValue.equalsIgnoreCase(matcherValue)) {
                        return true;
                    }

                    // match as regex - matcher -> matched (data plane or control plane)
                    try {
                        if (matcher.matches(matchedValue)) {
                            return true;
                        }
                    } catch (PatternSyntaxException pse) {
                        if (MockServerLogger.isEnabled(DEBUG) && mockServerLogger != null) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(DEBUG)
                                    .setMessageFormat("error while matching regex [" + matcher + "] for string [" + matched + "] " + pse.getMessage())
                                    .setThrowable(pse)
                            );
                        }
                    }
                    // match as regex - matched -> matcher (control plane only)
                    try {
                        if (controlPlaneMatcher && matched.matches(matcherValue)) {
                            return true;
                        } else if (MockServerLogger.isEnabled(DEBUG) && matched.matches(matcherValue) && mockServerLogger != null) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(DEBUG)
                                    .setMessageFormat("matcher{}would match{}if matcher was used for control plane")
                                    .setArguments(matcher, matched)
                            );
                        }
                    } catch (PatternSyntaxException pse) {
                        if (controlPlaneMatcher) {
                            if (MockServerLogger.isEnabled(DEBUG) && mockServerLogger != null) {
                                mockServerLogger.logEvent(
                                    new LogEntry()
                                        .setLogLevel(DEBUG)
                                        .setMessageFormat("error while matching regex [" + matched + "] for string [" + matcher + "] " + pse.getMessage())
                                        .setThrowable(pse)
                                );
                            }
                        }
                    }
                }
            }
        }
        if (context != null) {
            context.addDifference(mockServerLogger, "string or regex match failed expected:{}found:{}", matcher, matched);
        }

        return false;
    }

    public boolean isBlank() {
        return matcher == null || StringUtils.isBlank(matcher.getValue());
    }

    @Override
    @JsonIgnore
    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return EXCLUDED_FIELDS;
    }
}
