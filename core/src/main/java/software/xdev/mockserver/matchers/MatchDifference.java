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

import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.RequestDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.formatting.StringFormatter.formatLogMessage;
import static org.slf4j.event.Level.TRACE;

public class MatchDifference {

    public enum Field {
        METHOD("method"),
        PATH("path"),
        PATH_PARAMETERS("pathParameters"),
        QUERY_PARAMETERS("queryParameters"),
        COOKIES("cookies"),
        HEADERS("headers"),
        BODY("body"),
        SECURE("secure"),
        PROTOCOL("protocol"),
        KEEP_ALIVE("keep-alive"),
        OPERATION("operation"),
        OPENAPI("openapi");

        private final String name;

        Field(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final boolean detailedMatchFailures;
    private final RequestDefinition httpRequest;
    private final Map<Field, List<String>> differences = new ConcurrentHashMap<>();
    private Field fieldName;

    public MatchDifference(boolean detailedMatchFailures, RequestDefinition httpRequest) {
        this.detailedMatchFailures = detailedMatchFailures;
        this.httpRequest = httpRequest;
    }

    @SuppressWarnings("UnusedReturnValue")
    public MatchDifference addDifference(MockServerLogger mockServerLogger, Throwable throwable, String messageFormat, Object... arguments) {
        if (mockServerLogger != null && MockServerLogger.isEnabled(TRACE)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(TRACE)
                    .setHttpRequest(httpRequest)
                    .setCorrelationId(httpRequest.getLogCorrelationId())
                    .setMessageFormat(messageFormat)
                    .setArguments(arguments)
                    .setThrowable(throwable)
            );
        }
        return addDifference(messageFormat, arguments);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MatchDifference addDifference(MockServerLogger mockServerLogger, String messageFormat, Object... arguments) {
        return addDifference(mockServerLogger, null, messageFormat, arguments);
    }

    public MatchDifference addDifference(Field fieldName, String messageFormat, Object... arguments) {
        if (detailedMatchFailures) {
            if (isNotBlank(messageFormat) && arguments != null && fieldName != null) {
                this.differences
                    .computeIfAbsent(fieldName, key -> new ArrayList<>())
                    .add(formatLogMessage(1, messageFormat, arguments));
            }
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public MatchDifference addDifference(String messageFormat, Object... arguments) {
        return addDifference(fieldName, messageFormat, arguments);
    }

    public RequestDefinition getHttpRequest() {
        return httpRequest;
    }

    public String getLogCorrelationId() {
        return httpRequest.getLogCorrelationId();
    }

    @SuppressWarnings("UnusedReturnValue")
    protected MatchDifference currentField(Field fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public List<String> getDifferences(Field fieldName) {
        return this.differences.get(fieldName);
    }

    public Map<Field, List<String>> getAllDifferences() {
        return this.differences;
    }

    public void addDifferences(Map<Field, List<String>> differences) {
        for (Field field : differences.keySet()) {
            this.differences
                .computeIfAbsent(field, key -> new ArrayList<>())
                .addAll(differences.get(field));
        }
    }
}
