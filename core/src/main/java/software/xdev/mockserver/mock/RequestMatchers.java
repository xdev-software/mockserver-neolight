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
package software.xdev.mockserver.mock;

import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.collections.CircularHashMap;
import software.xdev.mockserver.collections.CircularPriorityQueue;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.matchers.HttpRequestMatcher;
import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.matchers.MatcherBuilder;
import software.xdev.mockserver.mock.listeners.MockServerMatcherNotifier;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.uuid.UUIDService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.log.model.LogEntryMessages.*;
import static software.xdev.mockserver.mock.SortableExpectationId.EXPECTATION_SORTABLE_PRIORITY_COMPARATOR;
import static software.xdev.mockserver.mock.SortableExpectationId.NULL;
import static org.slf4j.event.Level.DEBUG;

@SuppressWarnings("FieldMayBeFinal")
public class RequestMatchers extends MockServerMatcherNotifier {
    
    private static final Logger LOG = LoggerFactory.getLogger(RequestMatchers.class);
    
    final CircularPriorityQueue<String, HttpRequestMatcher, SortableExpectationId> httpRequestMatchers;
    final CircularHashMap<String, RequestDefinition> expectationRequestDefinitions;
    private final Configuration configuration;
    private final Scheduler scheduler;
    private WebSocketClientRegistry webSocketClientRegistry;
    private MatcherBuilder matcherBuilder;

    public RequestMatchers(Configuration configuration, Scheduler scheduler, WebSocketClientRegistry webSocketClientRegistry) {
        super(scheduler);
        this.configuration = configuration;
        this.scheduler = scheduler;
        this.matcherBuilder = new MatcherBuilder(configuration);
        this.webSocketClientRegistry = webSocketClientRegistry;
        httpRequestMatchers = new CircularPriorityQueue<>(
            configuration.maxExpectations(),
            EXPECTATION_SORTABLE_PRIORITY_COMPARATOR,
            httpRequestMatcher -> httpRequestMatcher.getExpectation() != null ? httpRequestMatcher.getExpectation().getSortableId() : NULL,
            httpRequestMatcher -> httpRequestMatcher.getExpectation() != null ? httpRequestMatcher.getExpectation().getId() : ""
        );
        expectationRequestDefinitions = new CircularHashMap<>(configuration.maxExpectations());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Expectation circular priority queue created, with size {}", configuration.maxExpectations());
        }
    }

    public Expectation add(Expectation expectation, Cause cause) {
        Expectation upsertedExpectation = null;
        if (expectation != null) {
            expectationRequestDefinitions.put(expectation.getId(), expectation.getHttpRequest());
            upsertedExpectation = httpRequestMatchers
                .getByKey(expectation.getId())
                .map(httpRequestMatcher -> {
                    if (httpRequestMatcher.getExpectation() != null) {
                        // propagate created time from previous entry to avoid re-ordering on update
                        expectation.withCreated(httpRequestMatcher.getExpectation().getCreated());
                    }
                    httpRequestMatchers.removePriorityKey(httpRequestMatcher);
                    if (httpRequestMatcher.update(expectation)) {
                        httpRequestMatchers.addPriorityKey(httpRequestMatcher);
                        if (LOG.isInfoEnabled()) {
                            LOG.info(UPDATED_EXPECTATION_MESSAGE_FORMAT, expectation.clone(), expectation.getId());
                        }
                    } else {
                        httpRequestMatchers.addPriorityKey(httpRequestMatcher);
                    }
                    return httpRequestMatcher;
                })
                .orElseGet(() -> addPrioritisedExpectation(expectation, cause))
                .getExpectation();
            notifyListeners(this, cause);
        }
        return upsertedExpectation;
    }

    public void update(Expectation[] expectations, Cause cause) {
        AtomicInteger numberOfChanges = new AtomicInteger(0);
        if (expectations != null) {
            Map<String, HttpRequestMatcher> httpRequestMatchersByKey = httpRequestMatchers.keyMap();
            Set<String> existingKeysForCause = httpRequestMatchersByKey
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().getSource().equals(cause))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
            Set<String> addedIds = new HashSet<>();
            Arrays
                .stream(expectations)
                .forEach(expectation -> {
                    // ensure duplicate ids are skipped in input array
                    if (!addedIds.contains(expectation.getId())) {
                        addedIds.add(expectation.getId());
                        expectationRequestDefinitions.put(expectation.getId(), expectation.getHttpRequest());
                        existingKeysForCause.remove(expectation.getId());
                        if (httpRequestMatchersByKey.containsKey(expectation.getId())) {
                            HttpRequestMatcher httpRequestMatcher = httpRequestMatchersByKey.get(expectation.getId());
                            // update source to new cause
                            httpRequestMatcher.withSource(cause);
                            if (httpRequestMatcher.getExpectation() != null) {
                                // propagate created time from previous entry to avoid re-ordering on update
                                expectation.withCreated(httpRequestMatcher.getExpectation().getCreated());
                            }
                            httpRequestMatchers.removePriorityKey(httpRequestMatcher);
                            if (httpRequestMatcher.update(expectation)) {
                                httpRequestMatchers.addPriorityKey(httpRequestMatcher);
                                numberOfChanges.getAndIncrement();
                                if (LOG.isInfoEnabled()) {
                                    LOG.info(UPDATED_EXPECTATION_MESSAGE_FORMAT, expectation.clone(), expectation.getId());
                                }
                            } else {
                                httpRequestMatchers.addPriorityKey(httpRequestMatcher);
                            }
                        } else {
                            addPrioritisedExpectation(expectation, cause);
                            numberOfChanges.getAndIncrement();
                        }
                    }
                });
            existingKeysForCause
                .forEach(key -> {
                    numberOfChanges.getAndIncrement();
                    HttpRequestMatcher httpRequestMatcher = httpRequestMatchersByKey.get(key);
                    removeHttpRequestMatcher(httpRequestMatcher, cause, false, UUIDService.getUUID());
                });
            if (numberOfChanges.get() > 0) {
                notifyListeners(this, cause);
            }
        }
    }

    private HttpRequestMatcher addPrioritisedExpectation(Expectation expectation, Cause cause) {
        HttpRequestMatcher httpRequestMatcher = matcherBuilder.transformsToMatcher(expectation);
        httpRequestMatchers.add(httpRequestMatcher);
        httpRequestMatcher.withSource(cause);
        if (LOG.isInfoEnabled()) {
            LOG.info(CREATED_EXPECTATION_MESSAGE_FORMAT, expectation.clone(), expectation.getId());
        }
        return httpRequestMatcher;
    }

    public int size() {
        return httpRequestMatchers.size();
    }

    public void reset(Cause cause) {
        httpRequestMatchers.stream().forEach(httpRequestMatcher -> removeHttpRequestMatcher(httpRequestMatcher, cause, false, UUIDService.getUUID()));
        expectationRequestDefinitions.clear();
        notifyListeners(this, cause);
    }

    public void reset() {
        reset(Cause.API);
    }

    public Expectation firstMatchingExpectation(HttpRequest httpRequest) {
        Optional<Expectation> first = getHttpRequestMatchersCopy()
            .map(httpRequestMatcher -> {
                Expectation matchingExpectation = null;
                boolean remainingMatchesDecremented = false;
                if (httpRequestMatcher.matches(LOG.isDebugEnabled() ? new MatchDifference(configuration.detailedMatchFailures(), httpRequest) : null, httpRequest)) {
                    matchingExpectation = httpRequestMatcher.getExpectation();
                    httpRequestMatcher.setResponseInProgress(true);
                    if (matchingExpectation.decrementRemainingMatches()) {
                        remainingMatchesDecremented = true;
                    }
                } else if (!httpRequestMatcher.isResponseInProgress() && !httpRequestMatcher.isActive()) {
                    scheduler.submit(() -> removeHttpRequestMatcher(httpRequestMatcher, UUIDService.getUUID()));
                }
                if (remainingMatchesDecremented) {
                    notifyListeners(this, Cause.API);
                }
                return matchingExpectation;
            })
            .filter(Objects::nonNull)
            .findFirst();
        return first.orElse(null);
    }

    public void clear(RequestDefinition requestDefinition) {
        if (requestDefinition != null) {
            HttpRequestMatcher clearHttpRequestMatcher = matcherBuilder.transformsToMatcher(requestDefinition);
            getHttpRequestMatchersCopy().forEach(httpRequestMatcher -> {
                RequestDefinition request = httpRequestMatcher
                    .getExpectation()
                    .getHttpRequest();
                if (isNotBlank(requestDefinition.getLogCorrelationId())) {
                    request = request
                        .shallowClone()
                        .withLogCorrelationId(requestDefinition.getLogCorrelationId());
                }
                if (clearHttpRequestMatcher.matches(request)) {
                    removeHttpRequestMatcher(httpRequestMatcher, requestDefinition.getLogCorrelationId());
                }
            });
            if (LOG.isInfoEnabled()) {
                LOG.info("Cleared expectations that match: {}", requestDefinition);
            }
        } else {
            reset();
        }
    }

    public void clear(ExpectationId expectationId, String logCorrelationId) {
        if (expectationId != null) {
            httpRequestMatchers
                .getByKey(expectationId.getId())
                .ifPresent(httpRequestMatcher -> removeHttpRequestMatcher(httpRequestMatcher, logCorrelationId));
            if (LOG.isInfoEnabled()) {
                LOG.info("Cleared expectations that have id: {}", expectationId.getId());
            }
        } else {
            reset();
        }
    }

    Expectation postProcess(Expectation expectation) {
        if (expectation != null) {
            getHttpRequestMatchersCopy()
                .filter(httpRequestMatcher -> httpRequestMatcher.getExpectation() == expectation)
                .findFirst()
                .ifPresent(httpRequestMatcher -> {
                    if (!expectation.isActive()) {
                        removeHttpRequestMatcher(httpRequestMatcher, UUIDService.getUUID());
                    }
                    httpRequestMatcher.setResponseInProgress(false);
                });
        }
        return expectation;
    }

    private void removeHttpRequestMatcher(HttpRequestMatcher httpRequestMatcher, String logCorrelationId) {
        removeHttpRequestMatcher(httpRequestMatcher, Cause.API, true, logCorrelationId);
    }

    @SuppressWarnings("rawtypes")
    private void removeHttpRequestMatcher(HttpRequestMatcher httpRequestMatcher, Cause cause, boolean notifyAndUpdateMetrics, String logCorrelationId) {
        if (httpRequestMatchers.remove(httpRequestMatcher)) {
            if (httpRequestMatcher.getExpectation() != null && LOG.isInfoEnabled()) {
                Expectation expectation = httpRequestMatcher.getExpectation().clone();
                LOG.info(REMOVED_EXPECTATION_MESSAGE_FORMAT, expectation, expectation.getId());
            }
            if (httpRequestMatcher.getExpectation() != null) {
                final Action action = httpRequestMatcher.getExpectation().getAction();
                if (action instanceof HttpObjectCallback callback) {
                    webSocketClientRegistry.unregisterClient(callback.getClientId());
                }
            }
            if (notifyAndUpdateMetrics) {
                notifyListeners(this, cause);
            }
        }
    }

    public Stream<RequestDefinition> retrieveRequestDefinitions(List<ExpectationId> expectationIds) {
        return expectationIds
            .stream()
            .map(expectationId -> {
                if (isBlank(expectationId.getId())) {
                    throw new IllegalArgumentException("No expectation id specified found \"" + expectationId.getId() + "\"");
                }
                if (expectationRequestDefinitions.containsKey(expectationId.getId())) {
                    return expectationRequestDefinitions.get(expectationId.getId());
                } else {
                    throw new IllegalArgumentException("No expectation found with id " + expectationId.getId());
                }
            })
            .filter(Objects::nonNull);
    }

    public List<Expectation> retrieveActiveExpectations(RequestDefinition requestDefinition) {
        if (requestDefinition == null) {
            return httpRequestMatchers.stream().map(HttpRequestMatcher::getExpectation).collect(Collectors.toList());
        } else {
            List<Expectation> expectations = new ArrayList<>();
            HttpRequestMatcher requestMatcher = matcherBuilder.transformsToMatcher(requestDefinition);
            getHttpRequestMatchersCopy().forEach(httpRequestMatcher -> {
                if (requestMatcher.matches(httpRequestMatcher.getExpectation().getHttpRequest())) {
                    expectations.add(httpRequestMatcher.getExpectation());
                }
            });
            return expectations;
        }
    }

    public List<HttpRequestMatcher> retrieveRequestMatchers(RequestDefinition requestDefinition) {
        if (requestDefinition == null) {
            return httpRequestMatchers.stream().collect(Collectors.toList());
        } else {
            List<HttpRequestMatcher> httpRequestMatchers = new ArrayList<>();
            HttpRequestMatcher requestMatcher = matcherBuilder.transformsToMatcher(requestDefinition);
            getHttpRequestMatchersCopy().forEach(httpRequestMatcher -> {
                if (requestMatcher.matches(httpRequestMatcher.getExpectation().getHttpRequest())) {
                    httpRequestMatchers.add(httpRequestMatcher);
                }
            });
            return httpRequestMatchers;
        }
    }

    public boolean isEmpty() {
        return httpRequestMatchers.isEmpty();
    }

    protected void notifyListeners(final RequestMatchers notifier, Cause cause) {
        super.notifyListeners(notifier, cause);
    }

    private Stream<HttpRequestMatcher> getHttpRequestMatchersCopy() {
        return httpRequestMatchers.stream();
    }
}
