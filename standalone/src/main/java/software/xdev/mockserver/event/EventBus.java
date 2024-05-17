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
package software.xdev.mockserver.event;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;

import software.xdev.mockserver.collections.CircularConcurrentLinkedDeque;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.event.model.EventEntry;
import software.xdev.mockserver.event.model.RequestAndExpectationId;
import software.xdev.mockserver.matchers.HttpRequestMatcher;
import software.xdev.mockserver.matchers.MatcherBuilder;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.mock.listeners.MockServerEventLogNotifier;
import software.xdev.mockserver.model.ExpectationId;
import software.xdev.mockserver.model.LogEventRequestAndResponse;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.scheduler.SchedulerThreadFactory;
import software.xdev.mockserver.serialization.RequestDefinitionSerializer;
import software.xdev.mockserver.uuid.UUIDService;
import software.xdev.mockserver.verify.Verification;
import software.xdev.mockserver.verify.VerificationSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.event.model.EventEntry.EventType.*;
import static software.xdev.mockserver.logging.LoggingMessages.VERIFICATION_REQUESTS_MESSAGE_FORMAT;
import static software.xdev.mockserver.logging.LoggingMessages.VERIFICATION_REQUEST_SEQUENCES_MESSAGE_FORMAT;
import static software.xdev.mockserver.model.HttpRequest.request;

public class EventBus extends MockServerEventLogNotifier {
    
    private static final Logger LOG = LoggerFactory.getLogger(EventBus.class);
    
    private static final Predicate<EventEntry> REQUEST_LOG_PREDICATE =
        input -> !input.isDeleted() && input.getType() == RECEIVED_REQUEST;
    private static final Predicate<EventEntry> EXPECTATION_LOG_PREDICATE =
        input -> !input.isDeleted()
            && (input.getType() == EXPECTATION_RESPONSE || input.getType() == FORWARDED_REQUEST);
    private static final Predicate<EventEntry> REQUEST_RESPONSE_LOG_PREDICATE =
        input -> !input.isDeleted()
            && (input.getType() == EXPECTATION_RESPONSE
            || input.getType() == NO_MATCH_RESPONSE
            || input.getType() == FORWARDED_REQUEST);
    private static final Predicate<EventEntry> RECORDED_EXPECTATION_LOG_PREDICATE =
        input -> !input.isDeleted() && input.getType() == FORWARDED_REQUEST;
    private static final Function<EventEntry, RequestDefinition[]> LOG_ENTRY_TO_REQUEST =
        EventEntry::getHttpRequests;
    private static final Function<EventEntry, Expectation> LOG_ENTRY_TO_EXPECTATION =
        EventEntry::getExpectation;
    private static final Function<EventEntry, LogEventRequestAndResponse> LOG_ENTRY_TO_HTTP_REQUEST_AND_HTTP_RESPONSE =
        eventEntry -> new LogEventRequestAndResponse()
            .withHttpRequest(eventEntry.getHttpRequest())
            .withHttpResponse(eventEntry.getHttpResponse())
            .withTimestamp(eventEntry.getTimestamp());
    
    private final ServerConfiguration configuration;
    private CircularConcurrentLinkedDeque<EventEntry> eventLog;
    private MatcherBuilder matcherBuilder;
    private RequestDefinitionSerializer requestDefinitionSerializer;
    private final boolean asynchronousEventProcessing;
    private Disruptor<EventEntry> disruptor;

    public EventBus(ServerConfiguration configuration, Scheduler scheduler, boolean asynchronousEventProcessing) {
        super(scheduler);
        this.configuration = configuration;
        this.matcherBuilder = new MatcherBuilder(configuration);
        this.requestDefinitionSerializer = new RequestDefinitionSerializer();
        this.asynchronousEventProcessing = asynchronousEventProcessing;
        this.eventLog = new CircularConcurrentLinkedDeque<>(configuration.maxLogEntries(), EventEntry::clear);
        startRingBuffer();
    }

    public void add(EventEntry eventEntry) {
        if (asynchronousEventProcessing) {
            if (!disruptor.getRingBuffer().tryPublishEvent(eventEntry)) {
                LOG.warn("Too many log events failed to add log event to ring buffer: {}", eventEntry);
            }
        } else {
            processLogEntry(eventEntry);
        }
    }

    public int size() {
        return eventLog.size();
    }

    private void startRingBuffer() {
        disruptor = new Disruptor<>(EventEntry::new, configuration.ringBufferSize(), new SchedulerThreadFactory("EventLog"));

        final ExceptionHandler<EventEntry> errorHandler = new ExceptionHandler<>() {
            @Override
            public void handleEventException(Throwable ex, long sequence, EventEntry logEntry) {
                LOG.error("exception handling log entry in log ring buffer, for log entry: {}", logEntry, ex);
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                LOG.error("exception starting log ring buffer", ex);
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                LOG.error("exception during shutdown of log ring buffer", ex);
            }
        };
        disruptor.setDefaultExceptionHandler(errorHandler);

        disruptor.handleEventsWith((eventEntry, sequence, endOfBatch) -> {
            if (eventEntry.getType() != RUNNABLE) {
                processLogEntry(eventEntry);
            } else {
                eventEntry.getConsumer().run();
                eventEntry.clear();
            }
        });

        disruptor.start();
    }

    private void processLogEntry(EventEntry eventEntry) {
        eventEntry = eventEntry.cloneAndClear();
        eventLog.add(eventEntry);
        notifyListeners(this, false);
    }

    public void stop() {
        try {
            notifyListeners(this, true);
            eventLog.clear();
            disruptor.shutdown(2, SECONDS);
        } catch (Exception ex) {
            if (!(ex instanceof com.lmax.disruptor.TimeoutException) && LOG.isWarnEnabled()) {
                LOG.warn("Exception while shutting down log ring buffer", ex);
            }
        }
    }

    public void reset() {
        CompletableFuture<String> future = new CompletableFuture<>();
        disruptor.publishEvent(new EventEntry()
            .setType(RUNNABLE)
            .setConsumer(() -> {
                eventLog.clear();
                future.complete("done");
                notifyListeners(this, false);
            })
        );
        try {
            future.get(2, SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException ignore) {
        }
    }

    public void clear(RequestDefinition requestDefinition) {
        CompletableFuture<String> future = new CompletableFuture<>();
        final boolean markAsDeletedOnly = LOG.isInfoEnabled();
        disruptor.publishEvent(new EventEntry()
            .setType(RUNNABLE)
            .setConsumer(() -> {
                String logCorrelationId = UUIDService.getUUID();
                RequestDefinition matcher = requestDefinition != null ? requestDefinition : request().withLogCorrelationId(logCorrelationId);
                HttpRequestMatcher requestMatcher = matcherBuilder.transformsToMatcher(matcher);
                for (EventEntry eventEntry : new LinkedList<>(eventLog)) {
                    RequestDefinition[] requests = eventEntry.getHttpRequests();
                    boolean matches = false;
                    if (requests != null) {
                        for (RequestDefinition request : requests) {
                            if (requestMatcher.matches(request.cloneWithLogCorrelationId())) {
                                matches = true;
                            }
                        }
                    } else {
                        matches = true;
                    }
                    if (matches) {
                        if (markAsDeletedOnly) {
                            eventEntry.setDeleted(true);
                        } else {
                            eventLog.removeItem(eventEntry);
                        }
                    }
                }
                if (LOG.isInfoEnabled()) {
                    LOG.info("Cleared logs that match: {}", requestDefinition);
                }
                future.complete("done");
                notifyListeners(this, false);
            })
        );
        try {
            future.get(2, SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException ignore) {
        }
    }

    public void retrieveRequests(Verification verification, String logCorrelationId, Consumer<List<RequestDefinition>> listConsumer) {
        if (verification.getExpectationId() != null) {
            retrieveLogEntries(
                Collections.singletonList(verification.getExpectationId().getId()),
                EXPECTATION_LOG_PREDICATE,
                LOG_ENTRY_TO_REQUEST,
                logEventStream -> listConsumer.accept(
                    logEventStream
                        .filter(Objects::nonNull)
                        .flatMap(Arrays::stream)
                        .collect(Collectors.toList())
                )
            );
        } else {
            retrieveLogEntries(
                verification.getHttpRequest().withLogCorrelationId(logCorrelationId),
                REQUEST_LOG_PREDICATE,
                LOG_ENTRY_TO_REQUEST,
                logEventStream -> listConsumer.accept(
                    logEventStream
                        .filter(Objects::nonNull)
                        .flatMap(Arrays::stream)
                        .collect(Collectors.toList())
                )
            );
        }
    }

    public void retrieveAllRequests(boolean matchingExpectationsOnly, Consumer<List<RequestDefinition>> listConsumer) {
        if (matchingExpectationsOnly) {
            retrieveLogEntries(
                (List<String>) null,
                EXPECTATION_LOG_PREDICATE,
                LOG_ENTRY_TO_REQUEST,
                logEventStream -> listConsumer.accept(
                    logEventStream
                        .filter(Objects::nonNull)
                        .flatMap(Arrays::stream)
                        .collect(Collectors.toList())
                )
            );
        } else {
            retrieveLogEntries(
                (RequestDefinition) null,
                REQUEST_LOG_PREDICATE,
                LOG_ENTRY_TO_REQUEST,
                logEventStream -> listConsumer.accept(
                    logEventStream
                        .filter(Objects::nonNull)
                        .flatMap(Arrays::stream)
                        .collect(Collectors.toList())
                )
            );
        }
    }

    public void retrieveAllRequests(List<String> expectationIds, Consumer<List<RequestAndExpectationId>> listConsumer) {
        retrieveLogEntries(
            expectationIds,
            EXPECTATION_LOG_PREDICATE,
            eventEntry -> new RequestAndExpectationId(eventEntry.getHttpRequest(), eventEntry.getExpectationId()),
            logEventStream -> listConsumer.accept(
                logEventStream
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
            )
        );
    }

    public void retrieveRequests(RequestDefinition requestDefinition, Consumer<List<RequestDefinition>> listConsumer) {
        retrieveLogEntries(
            requestDefinition,
            REQUEST_LOG_PREDICATE,
            LOG_ENTRY_TO_REQUEST,
            logEventStream -> listConsumer.accept(
                logEventStream
                    .filter(Objects::nonNull)
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList())
            )
        );
    }

    public void retrieveRequestResponses(RequestDefinition requestDefinition, Consumer<List<LogEventRequestAndResponse>> listConsumer) {
        retrieveLogEntries(
            requestDefinition,
            REQUEST_RESPONSE_LOG_PREDICATE,
            LOG_ENTRY_TO_HTTP_REQUEST_AND_HTTP_RESPONSE,
            logEventStream -> listConsumer.accept(logEventStream.filter(Objects::nonNull).collect(Collectors.toList()))
        );
    }

    public void retrieveRecordedExpectations(RequestDefinition requestDefinition, Consumer<List<Expectation>> listConsumer) {
        retrieveLogEntries(
            requestDefinition,
            RECORDED_EXPECTATION_LOG_PREDICATE,
            LOG_ENTRY_TO_EXPECTATION,
            logEventStream -> listConsumer.accept(logEventStream.filter(Objects::nonNull).collect(Collectors.toList()))
        );
    }

    private void retrieveLogEntries(RequestDefinition requestDefinition, Predicate<EventEntry> logEntryPredicate, Consumer<Stream<EventEntry>> consumer) {
        disruptor.publishEvent(new EventEntry()
            .setType(RUNNABLE)
            .setConsumer(() -> {
                HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(requestDefinition);
                consumer.accept(this.eventLog
                    .stream()
                    .filter(logItem -> logItem.matches(httpRequestMatcher))
                    .filter(logEntryPredicate)
                );
            })
        );
    }

    private <T> void retrieveLogEntries(RequestDefinition requestDefinition, Predicate<EventEntry> logEntryPredicate, Function<EventEntry, T> logEntryMapper, Consumer<Stream<T>> consumer) {
        disruptor.publishEvent(new EventEntry()
            .setType(RUNNABLE)
            .setConsumer(() -> {
                RequestDefinition requestDefinitionMatcher = requestDefinition != null ? requestDefinition : request().withLogCorrelationId(UUIDService.getUUID());
                HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(requestDefinitionMatcher);
                consumer.accept(this.eventLog
                    .stream()
                    .filter(logItem -> logItem.matches(httpRequestMatcher))
                    .filter(logEntryPredicate)
                    .map(logEntryMapper)
                );
            })
        );
    }

    @SuppressWarnings("SameParameterValue")
    private <T> void retrieveLogEntries(List<String> expectationIds, Predicate<EventEntry> logEntryPredicate, Function<EventEntry, T> logEntryMapper, Consumer<Stream<T>> consumer) {
        disruptor.publishEvent(new EventEntry()
            .setType(RUNNABLE)
            .setConsumer(() -> consumer.accept(this.eventLog
                .stream()
                .filter(logEntryPredicate)
                .filter(logItem -> expectationIds == null || logItem.matchesAnyExpectationId(expectationIds))
                .map(logEntryMapper)
            ))
        );
    }

    public Future<String> verify(Verification verification) {
        CompletableFuture<String> result = new CompletableFuture<>();
        verify(verification, result::complete);
        return result;
    }

    public void verify(Verification verification, Consumer<String> resultConsumer) {
        final String logCorrelationId = UUIDService.getUUID();
        if (verification != null) {
            if (LOG.isInfoEnabled()) {
                LOG.info(VERIFICATION_REQUESTS_MESSAGE_FORMAT, verification);
            }
            retrieveRequests(verification, logCorrelationId, httpRequests -> {
                try {
                    if (!verification.getTimes().matches(httpRequests.size())) {
                        boolean matchByExpectationId = verification.getExpectationId() != null;
                        retrieveAllRequests(matchByExpectationId, allRequests -> {
                            String failureMessage;
                            String serializedRequestToBeVerified = requestDefinitionSerializer.serialize(true, verification.getHttpRequest());
                            Integer maximumNumberOfRequestToReturnInVerificationFailure = verification.getMaximumNumberOfRequestToReturnInVerificationFailure() != null ? verification.getMaximumNumberOfRequestToReturnInVerificationFailure() : configuration.maximumNumberOfRequestToReturnInVerificationFailure();
                            if (allRequests.size() < maximumNumberOfRequestToReturnInVerificationFailure) {
                                String serializedAllRequestInLog = allRequests.size() == 1 ? requestDefinitionSerializer.serialize(true, allRequests.get(0)) : requestDefinitionSerializer.serialize(true, allRequests);
                                failureMessage = "Request not found " + verification.getTimes() + ", expected:<" + serializedRequestToBeVerified + "> but was:<" + serializedAllRequestInLog + ">";
                            } else {
                                failureMessage = "Request not found " + verification.getTimes() + ", expected:<" + serializedRequestToBeVerified + "> but was not found, found " + allRequests.size() + " other requests";
                            }
                            if (LOG.isInfoEnabled()) {
                                LOG.info("Request not found {}, expected: {} but was: {}",
                                    verification.getTimes(),
                                    verification.getHttpRequest(),
                                    allRequests.size() == 1 ? allRequests.get(0) : allRequests);
                            }
                            resultConsumer.accept(failureMessage);
                        });
                    } else {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("request:{} found {}", verification.getHttpRequest(), verification.getTimes());
                        }
                        resultConsumer.accept("");
                    }
                } catch (Exception ex) {
                    LOG.error("exception while processing verification: {}", verification, ex);
                    resultConsumer.accept("exception while processing verification" + (isNotBlank(ex.getMessage()) ? " " + ex.getMessage() : ""));
                }
            });
        } else {
            resultConsumer.accept("");
        }
    }

    public Future<String> verify(VerificationSequence verification) {
        CompletableFuture<String> result = new CompletableFuture<>();
        verify(verification, result::complete);
        return result;
    }

    public void verify(VerificationSequence verificationSequence, Consumer<String> resultConsumer) {
        if (verificationSequence != null) {
            final String logCorrelationId = UUIDService.getUUID();
            if (LOG.isInfoEnabled()) {
                LOG.info(VERIFICATION_REQUEST_SEQUENCES_MESSAGE_FORMAT, verificationSequence);
            }
            if (verificationSequence.getExpectationIds() != null && !verificationSequence.getExpectationIds().isEmpty()) {
                retrieveAllRequests(verificationSequence.getExpectationIds().stream().map(ExpectationId::getId).collect(Collectors.toList()), allRequests -> {
                    List<RequestDefinition> requestDefinitions = allRequests.stream().map(RequestAndExpectationId::getRequestDefinition).collect(Collectors.toList());
                    try {
                        String failureMessage = "";
                        int requestLogCounter = 0;
                        for (ExpectationId expectationId : verificationSequence.getExpectationIds()) {
                            if (expectationId != null) {
                                boolean foundRequest = false;
                                for (; !foundRequest && requestLogCounter < allRequests.size(); requestLogCounter++) {
                                    if (allRequests.get(requestLogCounter).matches(expectationId)) {
                                        // move on to next request
                                        foundRequest = true;
                                    }
                                }
                                if (!foundRequest) {
                                    failureMessage = verificationSequenceFailureMessage(verificationSequence, logCorrelationId, requestDefinitions);
                                    break;
                                }
                            }
                        }
                        verificationSequenceSuccessMessage(verificationSequence, resultConsumer, logCorrelationId, failureMessage);

                    } catch (Exception ex) {
                        verificationSequenceExceptionHandler(verificationSequence, resultConsumer, logCorrelationId, ex, "exception while processing verification sequence:{}", "exception while processing verification sequence");
                    }
                });
            } else {
                retrieveAllRequests(false, allRequests -> {
                    try {
                        String failureMessage = "";
                        int requestLogCounter = 0;
                        for (RequestDefinition verificationHttpRequest : verificationSequence.getHttpRequests()) {
                            if (verificationHttpRequest != null) {
                                verificationHttpRequest.withLogCorrelationId(logCorrelationId);
                                HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(verificationHttpRequest);
                                boolean foundRequest = false;
                                for (; !foundRequest && requestLogCounter < allRequests.size(); requestLogCounter++) {
                                    if (httpRequestMatcher.matches(allRequests.get(requestLogCounter).cloneWithLogCorrelationId())) {
                                        // move on to next request
                                        foundRequest = true;
                                    }
                                }
                                if (!foundRequest) {
                                    failureMessage = verificationSequenceFailureMessage(verificationSequence, logCorrelationId, allRequests);
                                    break;
                                }
                            }
                        }
                        verificationSequenceSuccessMessage(verificationSequence, resultConsumer, logCorrelationId, failureMessage);

                    } catch (Exception ex) {
                        verificationSequenceExceptionHandler(verificationSequence, resultConsumer, logCorrelationId, ex, "exception:{} while processing verification sequence:{}", "exception while processing verification sequence");
                    }
                });
            }
        } else {
            resultConsumer.accept("");
        }
    }

    private void verificationSequenceSuccessMessage(VerificationSequence verificationSequence, Consumer<String> resultConsumer, String logCorrelationId, String failureMessage) {
        if (isBlank(failureMessage) && LOG.isInfoEnabled()) {
            LOG.info("request sequence found: {}", verificationSequence.getHttpRequests());
        }
        resultConsumer.accept(failureMessage);
    }

    private String verificationSequenceFailureMessage(VerificationSequence verificationSequence, String logCorrelationId, List<RequestDefinition> allRequests) {
        String failureMessage;
        String serializedRequestToBeVerified = requestDefinitionSerializer.serialize(true, verificationSequence.getHttpRequests());
        Integer maximumNumberOfRequestToReturnInVerificationFailure = verificationSequence.getMaximumNumberOfRequestToReturnInVerificationFailure() != null ? verificationSequence.getMaximumNumberOfRequestToReturnInVerificationFailure() : configuration.maximumNumberOfRequestToReturnInVerificationFailure();
        if (allRequests.size() < maximumNumberOfRequestToReturnInVerificationFailure) {
            String serializedAllRequestInLog = allRequests.size() == 1 ? requestDefinitionSerializer.serialize(true, allRequests.get(0)) : requestDefinitionSerializer.serialize(true, allRequests);
            failureMessage = "Request sequence not found, expected:<" + serializedRequestToBeVerified + "> but was:<" + serializedAllRequestInLog + ">";
        } else {
            failureMessage = "Request sequence not found, expected:<" + serializedRequestToBeVerified + "> but was not found, found " + allRequests.size() + " other requests";
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Request sequence not found, expected: {} but was: {}",
                verificationSequence.getHttpRequests(),
                allRequests.size() == 1 ? allRequests.get(0) : allRequests);
        }
        return failureMessage;
    }

    private void verificationSequenceExceptionHandler(VerificationSequence verificationSequence, Consumer<String> resultConsumer, String logCorrelationId, Exception ex, String s, String s2) {
        LOG.error(s, verificationSequence, ex);
        resultConsumer.accept(s2 + (isNotBlank(ex.getMessage()) ? " " + ex.getMessage() : ""));
    }

    @Override
    public boolean equals(final Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof final EventBus that))
        {
            return false;
        }
        if(!super.equals(o))
        {
            return false;
        }
		return asynchronousEventProcessing == that.asynchronousEventProcessing
            && Objects.equals(configuration, that.configuration)
            && Objects.equals(eventLog, that.eventLog)
            && Objects.equals(matcherBuilder, that.matcherBuilder)
            && Objects.equals(requestDefinitionSerializer, that.requestDefinitionSerializer);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(
            super.hashCode(),
            configuration,
            eventLog,
            matcherBuilder,
            requestDefinitionSerializer,
            asynchronousEventProcessing);
    }
}
