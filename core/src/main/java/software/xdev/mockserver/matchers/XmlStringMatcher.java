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
import software.xdev.mockserver.model.NottableString;
import org.slf4j.event.Level;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.placeholder.PlaceholderDifferenceEvaluator;

import static software.xdev.mockserver.model.NottableString.string;

public class XmlStringMatcher extends BodyMatcher<String> {
    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger", "diffBuilder"};
    private final MockServerLogger mockServerLogger;
    private DiffBuilder diffBuilder;
    private NottableString matcher = string("THIS SHOULD NEVER MATCH");

    XmlStringMatcher(MockServerLogger mockServerLogger, final String matcher) {
        this(mockServerLogger, string(matcher));
    }

    XmlStringMatcher(MockServerLogger mockServerLogger, final NottableString matcher) {
        this.mockServerLogger = mockServerLogger;
        try {
            this.matcher = matcher;
            this.diffBuilder = DiffBuilder.compare(Input.fromString(this.matcher.getValue()))
                .ignoreComments()
                .ignoreWhitespace()
                .normalizeWhitespace()
                .checkForSimilar()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .withDifferenceEvaluator(DifferenceEvaluators.chain(new PlaceholderDifferenceEvaluator(), DifferenceEvaluators.Default));
        } catch (Exception e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("error while creating xml string matcher for [" + matcher + "]" + e.getMessage())
                    .setThrowable(e)
            );
        }
    }

    public boolean matches(String matched) {
        return matches(null, matched);
    }

    public boolean matches(final MatchDifference context, String matched) {
        boolean result = false;

        if (diffBuilder != null) {
            try {
                Diff diff = diffBuilder.withTest(Input.fromString(matched)).build();
                result = !diff.hasDifferences();

                if (!result && context != null) {
                    context.addDifference(mockServerLogger, "xml match failed expected:{}found:{}failed because:{}", this.matcher, matched, diff.toString());
                }

            } catch (Throwable throwable) {
                if (context != null) {
                    context.addDifference(mockServerLogger, throwable, "xml match failed expected:{}found:{}failed because:{}", this.matcher, matched, throwable.getMessage());
                }
            }
        }

        return matcher.isNot() == (not == result);
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
