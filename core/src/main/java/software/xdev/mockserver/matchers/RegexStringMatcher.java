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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.model.NottableString;

import java.util.regex.PatternSyntaxException;

import static software.xdev.mockserver.model.NottableString.string;

public class RegexStringMatcher extends BodyMatcher<NottableString> {
    
    private static final Logger LOG = LoggerFactory.getLogger(RegexStringMatcher.class);
    
    private final NottableString matcher;
    private final boolean controlPlaneMatcher;

    public RegexStringMatcher(boolean controlPlaneMatcher) {
        this.controlPlaneMatcher = controlPlaneMatcher;
        this.matcher = null;
    }

    RegexStringMatcher(NottableString matcher, boolean controlPlaneMatcher) {
        this.controlPlaneMatcher = controlPlaneMatcher;
        this.matcher = matcher;
    }

    public boolean matches(String matched) {
        return matches((MatchDifference) null, string(matched));
    }

    public boolean matches(final MatchDifference context, NottableString matched) {
        boolean result = matcher == null || matches(context, matcher, matched);
        return not != result;
    }

    public boolean matches(NottableString matcher, NottableString matched) {
        return matches(null, matcher, matched);
    }

    public boolean matches(MatchDifference context, NottableString matcher, NottableString matched) {
        return matchesByNottedStrings(context, matcher, matched);
    }

    private boolean matchesByNottedStrings(MatchDifference context, NottableString matcher, NottableString matched) {
        if (matcher.isNot() && matched.isNot()) {
            // mutual notted control plane match
            return matchesByStrings(context, matcher, matched);
        } else {
            // data plane & control plan match
            return (matcher.isNot() || matched.isNot()) ^ matchesByStrings(context, matcher, matched);
        }
    }

    private boolean matchesByStrings(MatchDifference context, NottableString matcher, NottableString matched) {
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
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Error while matching regex [{}] for string [{}]", matcher, matched, pse);
                        }
                    }
                    // match as regex - matched -> matcher (control plane only)
                    try {
                        if (controlPlaneMatcher && matched.matches(matcherValue)) {
                            return true;
                        } else if (LOG.isDebugEnabled() && matched.matches(matcherValue)) {
                            LOG.debug("Matcher {} would match {} if matcher was used for control plane", matcher, matched);
                        }
                    } catch (PatternSyntaxException pse) {
                        if (controlPlaneMatcher) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Error while matching regex [{}] for string [{}]", matcher, matched, pse);
                            }
                        }
                    }
                }
            }
        }
        if (context != null) {
            context.addDifference("string or regex match failed expected:{}found:{}", matcher, matched);
        }

        return false;
    }

    public boolean isBlank() {
        return matcher == null || StringUtils.isBlank(matcher.getValue());
    }
}
