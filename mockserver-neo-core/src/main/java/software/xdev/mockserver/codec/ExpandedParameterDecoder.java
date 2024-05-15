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
package software.xdev.mockserver.codec;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.configuration.ConfigurationProperties;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.Parameter;
import software.xdev.mockserver.model.ParameterStyle;
import software.xdev.mockserver.model.Parameters;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.model.NottableOptionalString.optional;
import static software.xdev.mockserver.model.NottableString.string;

public class ExpandedParameterDecoder {

    private static final Pattern QUOTED_PARAMETER_VALUE = Pattern.compile("\\s*^[\"']+(.*)[\"']+\\s*$");
    private static final Pattern JSON_VALUE = Pattern.compile("(?s)^\\s*[{\\[].*[}\\]]\\s*$");

    private final Configuration configuration;
    private final MockServerLogger mockServerLogger;

    public ExpandedParameterDecoder(Configuration configuration, MockServerLogger mockServerLogger) {
        this.configuration = configuration;
        this.mockServerLogger = mockServerLogger;
    }

    public Parameters retrieveFormParameters(String parameterString, boolean hasPath) {
        Parameters parameters = new Parameters();
        Map<String, List<String>> parameterMap = new HashMap<>();
        if (isNotBlank(parameterString)) {
            try {
                hasPath = parameterString.startsWith("/") || parameterString.contains("?") || hasPath;
                parameterMap.putAll(new QueryStringDecoder(parameterString, HttpConstants.DEFAULT_CHARSET, hasPath, Integer.MAX_VALUE, !configuration.useSemicolonAsQueryParameterSeparator()).parameters());
            } catch (IllegalArgumentException iae) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception{}while parsing query string{}")
                        .setArguments(parameterString, iae.getMessage())
                        .setThrowable(iae)
                );
            }
        }
        return parameters.withEntries(parameterMap);
    }

    public Parameters retrieveQueryParameters(String parameterString, boolean hasPath) {
        if (isNotBlank(parameterString)) {
            String rawParameterString = parameterString.contains("?") ? StringUtils.substringAfter(parameterString, "?") : parameterString;
            Map<String, List<String>> parameterMap = new HashMap<>();
            try {
                hasPath = parameterString.startsWith("/") || parameterString.contains("?") || hasPath;
                parameterMap.putAll(new QueryStringDecoder(parameterString, HttpConstants.DEFAULT_CHARSET, parameterString.contains("/") || hasPath, Integer.MAX_VALUE, true).parameters());
            } catch (IllegalArgumentException iae) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception{}while parsing query string{}")
                        .setArguments(parameterString, iae.getMessage())
                        .setThrowable(iae)
                );
            }
            return new Parameters().withEntries(parameterMap).withRawParameterString(rawParameterString);
        }
        return null;
    }

    public void splitParameters(Parameters matcher, Parameters matched) {
        if (matcher != null && matched != null) {
            for (Parameter matcherEntry : matcher.getEntries()) {
                if (matcherEntry.getName().getParameterStyle() != null && matcherEntry.getName().getParameterStyle().isExploded()) {
                    for (Parameter matchedEntry : matched.getEntries()) {
                        if (matcherEntry.getName().getValue().equals(matchedEntry.getName().getValue()) || matchedEntry.getName().getValue().matches(matcherEntry.getName().getValue())) {
                            matchedEntry.replaceValues(new ExpandedParameterDecoder(configuration, mockServerLogger).splitOnDelimiter(matcherEntry.getName().getParameterStyle(), matcherEntry.getName().getValue(), matchedEntry.getValues()));
                            matched.replaceEntry(matchedEntry);
                        }
                    }
                }
            }
        }
    }

    public List<NottableString> splitOnDelimiter(ParameterStyle style, String name, List<NottableString> values) {
        if (isNotBlank(style.getRegex())) {
            List<NottableString> splitValues = new ArrayList<>();
            for (NottableString value : values) {
                Matcher quotedValue = QUOTED_PARAMETER_VALUE.matcher(value.getValue());
                if (quotedValue.matches()) {
                    if (value.isOptional()) {
                        splitValues.add(optional(quotedValue.group(1), value.isNot()));
                    } else {
                        splitValues.add(string(quotedValue.group(1), value.isNot()));
                    }
                } else if (!JSON_VALUE.matcher(value.getValue()).matches()) {
                    for (String splitValue : value.getValue().split(style.getRegex().replaceAll("<name>", name))) {
                        if (value.isOptional()) {
                            splitValues.add(optional(splitValue, value.isNot()));
                        } else {
                            splitValues.add(string(splitValue, value.isNot()));
                        }
                    }
                }
            }
            return splitValues;
        } else {
            return values;
        }
    }

}
