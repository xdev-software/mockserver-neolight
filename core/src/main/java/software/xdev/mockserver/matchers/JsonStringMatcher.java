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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Joiner;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.internal.Diff;
import net.javacrumbs.jsonunit.core.internal.Options;
import net.javacrumbs.jsonunit.core.listener.DifferenceContext;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.util.ArrayList;
import java.util.List;

import static net.javacrumbs.jsonunit.core.Option.*;
import static software.xdev.mockserver.character.Character.NEW_LINE;

public class JsonStringMatcher extends BodyMatcher<String> {
    private static final String[] EXCLUDED_FIELDS = {"mockServerLogger"};
    private static final ObjectWriter PRETTY_PRINTER = ObjectMapperFactory.createObjectMapper(true, false);
    private final MockServerLogger mockServerLogger;
    private final String matcher;
    private JsonNode matcherJsonNode;
    private final MatchType matchType;

    JsonStringMatcher(MockServerLogger mockServerLogger, String matcher, MatchType matchType) {
        this.mockServerLogger = mockServerLogger;
        this.matcher = matcher;

        this.matchType = matchType;
    }

    public boolean matches(final MatchDifference context, String matched) {
        boolean result = false;

        try {
            if (StringUtils.isBlank(matcher)) {
                result = true;
            } else {
                Options options = Options.empty();
                switch (matchType) {
                    case STRICT:
                        break;
                    case ONLY_MATCHING_FIELDS:
                        options = options.with(
                            IGNORING_ARRAY_ORDER,
                            IGNORING_EXTRA_ARRAY_ITEMS,
                            IGNORING_EXTRA_FIELDS
                        );
                        break;
                }
                final Difference diffListener = new Difference();
                Configuration diffConfig = Configuration.empty().withDifferenceListener(diffListener).withOptions(options);

                try {
                    if (matcherJsonNode == null) {
                        matcherJsonNode = ObjectMapperFactory.createObjectMapper().readTree(matcher);
                    }
                    result = Diff
                        .create(
                            matcherJsonNode,
                            ObjectMapperFactory.createObjectMapper().readTree(matched),
                            "",
                            "",
                            diffConfig
                        )
                        .similar();
                } catch (Throwable throwable) {
                    if (context != null) {
                        context.addDifference(mockServerLogger, throwable, "exception while perform json match failed expected:{}found:{}", this.matcher, matched);
                    }
                }

                if (!result) {
                    if (context != null) {
                        if (diffListener.differences.isEmpty()) {
                            context.addDifference(mockServerLogger, "json match failed expected:{}found:{}", this.matcher, matched);
                        } else {
                            context.addDifference(mockServerLogger, "json match failed expected:{}found:{}failed because:{}", this.matcher, matched, Joiner.on("," + NEW_LINE).join(diffListener.differences));
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            if (context != null) {
                context.addDifference(mockServerLogger, throwable, "json match failed expected:{}found:{}failed because:{}", this.matcher, matched, throwable.getMessage());
            }
        }

        return not != result;
    }

    private static class Difference implements DifferenceListener {

        public List<String> differences = new ArrayList<>();

        @Override
        public void diff(net.javacrumbs.jsonunit.core.listener.Difference difference, DifferenceContext context) {
            switch (difference.getType()) {
                case EXTRA:
                    differences.add("additional element at \"" + difference.getActualPath() + "\" with value: " + prettyPrint(difference.getActual()));
                    break;
                case MISSING:
                    differences.add("missing element at \"" + difference.getActualPath() + "\"");
                    break;
                case DIFFERENT:
                    differences.add("wrong value at \"" + difference.getActualPath() + "\", expected: " + prettyPrint(difference.getExpected()) + " but was: " + prettyPrint(difference.getActual()));
                    break;
            }
        }

        private String prettyPrint(Object value) {
            try {
                return PRETTY_PRINTER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                return String.valueOf(value);
            }
        }
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
