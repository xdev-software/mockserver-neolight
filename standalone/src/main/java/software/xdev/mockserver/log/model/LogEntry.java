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
package software.xdev.mockserver.log.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lmax.disruptor.EventTranslator;
import software.xdev.mockserver.matchers.HttpRequestMatcher;
import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.matchers.TimeToLive;
import software.xdev.mockserver.matchers.Times;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.time.EpochService;
import software.xdev.mockserver.uuid.UUIDService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.model.HttpRequest.request;

public class LogEntry implements EventTranslator<LogEntry> {

    private static final RequestDefinition[] EMPTY_REQUEST_DEFINITIONS = new RequestDefinition[0];
    private static final RequestDefinition[] DEFAULT_REQUESTS_DEFINITIONS = {request()};
    public static final DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private int hashCode;
    private String id;
    private String correlationId;
    private long epochTime = EpochService.currentTimeMillis();
    private String timestamp;
    private LogMessageType type;
    private RequestDefinition[] httpRequests;
    private RequestDefinition[] httpUpdatedRequests;
    private HttpResponse httpResponse;
    private HttpResponse httpUpdatedResponse;
    private HttpError httpError;
    private Expectation expectation;
    private String expectationId;
    private Exception exception;
    private Runnable consumer;
    private boolean deleted = false;

    public LogEntry() {

    }

    private LogEntry setId(String id) {
        this.id = id;
        return this;
    }

    @JsonIgnore
    public String id() {
        if (id == null) {
            id = UUIDService.getUUID();
        }
        return id;
    }

    public void clear() {
        id = null;
        correlationId = null;
        epochTime = -1;
        timestamp = null;
        type = null;
        httpRequests = null;
        httpResponse = null;
        httpError = null;
        expectation = null;
        expectationId = null;
        exception = null;
        consumer = null;
        deleted = false;
    }

    public long getEpochTime() {
        return epochTime;
    }

    public LogEntry setEpochTime(long epochTime) {
        this.epochTime = epochTime;
        this.timestamp = null;
        return this;
    }

    public String getTimestamp() {
        if (timestamp == null) {
            timestamp = LOG_DATE_FORMAT.format(new Date(epochTime));
        }
        return timestamp;
    }

    public LogMessageType getType() {
        return type;
    }

    public LogEntry setType(LogMessageType type) {
        this.type = type;
        return this;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public LogEntry setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    @JsonIgnore
    public RequestDefinition[] getHttpRequests() {
        if (httpRequests == null) {
            return EMPTY_REQUEST_DEFINITIONS;
        } else {
            return httpRequests;
        }
    }

    @JsonIgnore
    public RequestDefinition[] getHttpUpdatedRequests() {
        if (httpRequests == null) {
            return EMPTY_REQUEST_DEFINITIONS;
        } else if (httpUpdatedRequests == null) {
            httpUpdatedRequests = Arrays
                .stream(httpRequests)
                .map(this::updateBody)
                .toArray(RequestDefinition[]::new);
            return httpUpdatedRequests;
        } else {
            return httpUpdatedRequests;
        }
    }

    @JsonIgnore
    public boolean matches(HttpRequestMatcher matcher) {
        if (matcher == null) {
            return true;
        }
        if (httpRequests == null || httpRequests.length == 0) {
            return true;
        }
        for (RequestDefinition httpRequest : httpRequests) {
            RequestDefinition request = httpRequest.cloneWithLogCorrelationId();
            if (matcher.matches(type == LogMessageType.RECEIVED_REQUEST ? new MatchDifference(false, request) : null, request)) {
                return true;
            }
        }
        return false;
    }

    public LogEntry setHttpRequests(RequestDefinition[] httpRequests) {
        this.httpRequests = httpRequests;
        return this;
    }

    public RequestDefinition getHttpRequest() {
        if (httpRequests != null && httpRequests.length > 0) {
            return httpRequests[0];
        } else {
            return null;
        }
    }

    public LogEntry setHttpRequest(RequestDefinition httpRequest) {
        if (httpRequest != null) {
            if (isNotBlank(httpRequest.getLogCorrelationId())) {
                setCorrelationId(httpRequest.getLogCorrelationId());
            }
            this.httpRequests = new RequestDefinition[]{httpRequest};
        } else {
            this.httpRequests = DEFAULT_REQUESTS_DEFINITIONS;
        }
        return this;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public HttpResponse getHttpUpdatedResponse() {
        if (httpResponse == null) {
            return null;
        } else if (httpUpdatedResponse == null) {
            httpUpdatedResponse = updateBody(httpResponse);
            return httpUpdatedResponse;
        } else {
            return httpUpdatedResponse;
        }
    }

    public LogEntry setHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
        return this;
    }

    public HttpError getHttpError() {
        return httpError;
    }

    public LogEntry setHttpError(HttpError httpError) {
        this.httpError = httpError;
        return this;
    }

    public Expectation getExpectation() {
        return expectation;
    }

    public LogEntry setExpectation(Expectation expectation) {
        this.expectation = expectation;
        return this;
    }

    public LogEntry setExpectation(RequestDefinition httpRequest, HttpResponse httpResponse) {
        this.expectation = new Expectation(httpRequest, Times.once(), TimeToLive.unlimited(), 0).thenRespond(httpResponse);
        return this;
    }

    public String getExpectationId() {
        return expectationId;
    }

    public LogEntry setExpectationId(String expectationId) {
        this.expectationId = expectationId;
        return this;
    }

    public boolean matchesAnyExpectationId(List<String> expectationIds) {
        if (expectationIds != null && isNotBlank(this.expectationId)) {
            return expectationIds.contains(this.expectationId);
        }
        return false;
    }

    public Exception getException() {
        return exception;
    }

    public LogEntry setException(Exception ex) {
        this.exception = ex;
        return this;
    }

    public Runnable getConsumer() {
        return consumer;
    }

    public LogEntry setConsumer(Runnable consumer) {
        this.consumer = consumer;
        return this;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LogEntry setDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    private RequestDefinition updateBody(RequestDefinition requestDefinition) {
        if (requestDefinition instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) requestDefinition;
            return httpRequest;
        } else {
            return null;
        }
    }

    private HttpResponse updateBody(HttpResponse httpResponse) {
        if (httpResponse != null) {
            return httpResponse;
        } else {
            return null;
        }
    }

    public LogEntry cloneAndClear() {
        LogEntry clone = this.clone();
        clear();
        return clone;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public LogEntry clone() {
        return new LogEntry()
            .setId(id())
            .setType(getType())
            .setEpochTime(getEpochTime())
            .setCorrelationId(getCorrelationId())
            .setHttpRequests(getHttpRequests())
            .setHttpResponse(getHttpResponse())
            .setHttpError(getHttpError())
            .setExpectation(getExpectation())
            .setExpectationId(getExpectationId())
            .setException(getException())
            .setConsumer(getConsumer())
            .setDeleted(isDeleted());
    }

    @Override
    public void translateTo(LogEntry event, long sequence) {
        event
            .setId(id())
            .setType(getType())
            .setEpochTime(getEpochTime())
            .setCorrelationId(getCorrelationId())
            .setHttpRequests(getHttpRequests())
            .setHttpResponse(getHttpResponse())
            .setHttpError(getHttpError())
            .setExpectation(getExpectation())
            .setExpectationId(getExpectationId())
            .setException(getException())
            .setConsumer(getConsumer())
            .setDeleted(isDeleted());
        clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (hashCode() != o.hashCode()) {
            return false;
        }
        LogEntry logEntry = (LogEntry) o;
        return epochTime == logEntry.epochTime &&
            deleted == logEntry.deleted &&
            type == logEntry.type &&
            Objects.equals(httpResponse, logEntry.httpResponse) &&
            Objects.equals(httpError, logEntry.httpError) &&
            Objects.equals(expectation, logEntry.expectation) &&
            Objects.equals(expectationId, logEntry.expectationId) &&
            Objects.equals(consumer, logEntry.consumer) &&
            Arrays.equals(httpRequests, logEntry.httpRequests);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = Objects.hash(epochTime, deleted, type, httpResponse, httpError, expectation, expectationId, consumer);
            result = 31 * result + Arrays.hashCode(httpRequests);
            hashCode = result;
        }
        return hashCode;
    }

    @Override
    public String toString() {
        try {
            return ObjectMapperFactory
                .createObjectMapper(true, false)
                .writeValueAsString(this);
        } catch (Exception e) {
            return super.toString();
        }
    }

    public enum LogMessageType {
        RUNNABLE,
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        EXCEPTION,
        CLEARED,
        RETRIEVED,
        UPDATED_EXPECTATION,
        CREATED_EXPECTATION,
        REMOVED_EXPECTATION,
        RECEIVED_REQUEST,
        EXPECTATION_RESPONSE,
        EXPECTATION_MATCHED,
        EXPECTATION_NOT_MATCHED,
        NO_MATCH_RESPONSE,
        VERIFICATION,
        VERIFICATION_FAILED,
        VERIFICATION_PASSED,
        FORWARDED_REQUEST,
        TEMPLATE_GENERATED,
        SERVER_CONFIGURATION,
        AUTHENTICATION_FAILED,
    }

}
