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

import static software.xdev.mockserver.logging.LoggingMessages.CREATED_EXPECTATION_MESSAGE_FORMAT;
import static software.xdev.mockserver.logging.LoggingMessages.REMOVED_EXPECTATION_MESSAGE_FORMAT;
import static software.xdev.mockserver.logging.LoggingMessages.UPDATED_EXPECTATION_MESSAGE_FORMAT;
import static software.xdev.mockserver.mock.SortableExpectationId.EXPECTATION_SORTABLE_PRIORITY_COMPARATOR;
import static software.xdev.mockserver.mock.SortableExpectationId.NULL;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.collections.CircularHashMap;
import software.xdev.mockserver.collections.CircularPriorityQueue;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.matchers.HttpRequestMatcher;
import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.matchers.MatcherBuilder;
import software.xdev.mockserver.mock.listeners.MockServerMatcherNotifier;
import software.xdev.mockserver.model.Action;
import software.xdev.mockserver.model.ExpectationId;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.uuid.UUIDService;


public class RequestMatchers extends MockServerMatcherNotifier
{
	private static final Logger LOG = LoggerFactory.getLogger(RequestMatchers.class);
	
	final CircularPriorityQueue<String, HttpRequestMatcher, SortableExpectationId> httpRequestMatchers;
	final CircularHashMap<String, RequestDefinition> expectationRequestDefinitions;
	private final ServerConfiguration configuration;
	private final Scheduler scheduler;
	private final WebSocketClientRegistry webSocketClientRegistry;
	private final MatcherBuilder matcherBuilder;
	
	public RequestMatchers(
		final ServerConfiguration configuration,
		final Scheduler scheduler,
		final WebSocketClientRegistry webSocketClientRegistry)
	{
		super(scheduler);
		this.configuration = configuration;
		this.scheduler = scheduler;
		this.matcherBuilder = new MatcherBuilder(configuration);
		this.webSocketClientRegistry = webSocketClientRegistry;
		this.httpRequestMatchers = new CircularPriorityQueue<>(
			configuration.maxExpectations(),
			EXPECTATION_SORTABLE_PRIORITY_COMPARATOR,
			httpRequestMatcher -> httpRequestMatcher.getExpectation() != null
				? httpRequestMatcher.getExpectation().getSortableId()
				: NULL,
			httpRequestMatcher -> httpRequestMatcher.getExpectation() != null
				? httpRequestMatcher.getExpectation().getId()
				: ""
		);
		this.expectationRequestDefinitions = new CircularHashMap<>(configuration.maxExpectations());
		if(LOG.isTraceEnabled())
		{
			LOG.trace("Expectation circular priority queue created, with size {}", configuration.maxExpectations());
		}
	}
	
	public Expectation add(final Expectation expectation, final Cause cause)
	{
		Expectation upsertedExpectation = null;
		if(expectation != null)
		{
			this.expectationRequestDefinitions.put(expectation.getId(), expectation.getHttpRequest());
			upsertedExpectation = this.httpRequestMatchers
				.getByKey(expectation.getId())
				.map(httpRequestMatcher -> {
					if(httpRequestMatcher.getExpectation() != null)
					{
						// propagate created time from previous entry to avoid re-ordering on update
						expectation.withCreated(httpRequestMatcher.getExpectation().getCreated());
					}
					this.httpRequestMatchers.removePriorityKey(httpRequestMatcher);
					if(httpRequestMatcher.update(expectation))
					{
						this.httpRequestMatchers.addPriorityKey(httpRequestMatcher);
						if(LOG.isInfoEnabled())
						{
							LOG.info(UPDATED_EXPECTATION_MESSAGE_FORMAT, expectation.clone(), expectation.getId());
						}
					}
					else
					{
						this.httpRequestMatchers.addPriorityKey(httpRequestMatcher);
					}
					return httpRequestMatcher;
				})
				.orElseGet(() -> this.addPrioritisedExpectation(expectation, cause))
				.getExpectation();
			this.notifyListeners(this, cause);
		}
		return upsertedExpectation;
	}
	
	public void update(final Expectation[] expectations, final Cause cause)
	{
		final AtomicInteger numberOfChanges = new AtomicInteger(0);
		if(expectations != null)
		{
			final Map<String, HttpRequestMatcher> httpRequestMatchersByKey = this.httpRequestMatchers.keyMap();
			final Set<String> existingKeysForCause = httpRequestMatchersByKey
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue().getSource().equals(cause))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
			final Set<String> addedIds = new HashSet<>();
			Arrays
				.stream(expectations)
				.forEach(expectation -> {
					// ensure duplicate ids are skipped in input array
					if(!addedIds.contains(expectation.getId()))
					{
						addedIds.add(expectation.getId());
						this.expectationRequestDefinitions.put(expectation.getId(), expectation.getHttpRequest());
						existingKeysForCause.remove(expectation.getId());
						if(httpRequestMatchersByKey.containsKey(expectation.getId()))
						{
							final HttpRequestMatcher httpRequestMatcher =
								httpRequestMatchersByKey.get(expectation.getId());
							// update source to new cause
							httpRequestMatcher.withSource(cause);
							if(httpRequestMatcher.getExpectation() != null)
							{
								// propagate created time from previous entry to avoid re-ordering on update
								expectation.withCreated(httpRequestMatcher.getExpectation().getCreated());
							}
							this.httpRequestMatchers.removePriorityKey(httpRequestMatcher);
							if(httpRequestMatcher.update(expectation))
							{
								this.httpRequestMatchers.addPriorityKey(httpRequestMatcher);
								numberOfChanges.getAndIncrement();
								if(LOG.isInfoEnabled())
								{
									LOG.info(
										UPDATED_EXPECTATION_MESSAGE_FORMAT,
										expectation.clone(),
										expectation.getId());
								}
							}
							else
							{
								this.httpRequestMatchers.addPriorityKey(httpRequestMatcher);
							}
						}
						else
						{
							this.addPrioritisedExpectation(expectation, cause);
							numberOfChanges.getAndIncrement();
						}
					}
				});
			existingKeysForCause
				.forEach(key -> {
					numberOfChanges.getAndIncrement();
					final HttpRequestMatcher httpRequestMatcher = httpRequestMatchersByKey.get(key);
					this.removeHttpRequestMatcher(httpRequestMatcher, cause, false, UUIDService.getUUID());
				});
			if(numberOfChanges.get() > 0)
			{
				this.notifyListeners(this, cause);
			}
		}
	}
	
	private HttpRequestMatcher addPrioritisedExpectation(final Expectation expectation, final Cause cause)
	{
		final HttpRequestMatcher httpRequestMatcher = this.matcherBuilder.transformsToMatcher(expectation);
		this.httpRequestMatchers.add(httpRequestMatcher);
		httpRequestMatcher.withSource(cause);
		if(LOG.isInfoEnabled())
		{
			LOG.info(CREATED_EXPECTATION_MESSAGE_FORMAT, expectation.clone(), expectation.getId());
		}
		return httpRequestMatcher;
	}
	
	public int size()
	{
		return this.httpRequestMatchers.size();
	}
	
	public void reset(final Cause cause)
	{
		this.httpRequestMatchers.stream()
			.forEach(httpRequestMatcher -> this.removeHttpRequestMatcher(
				httpRequestMatcher,
				cause,
				false,
				UUIDService.getUUID()));
		this.expectationRequestDefinitions.clear();
		this.notifyListeners(this, cause);
	}
	
	public void reset()
	{
		this.reset(Cause.API);
	}
	
	public Expectation firstMatchingExpectation(final HttpRequest httpRequest)
	{
		final Optional<Expectation> first = this.getHttpRequestMatchersCopy()
			.map(httpRequestMatcher -> {
				Expectation matchingExpectation = null;
				boolean remainingMatchesDecremented = false;
				if(httpRequestMatcher.matches(LOG.isDebugEnabled()
					? new MatchDifference(this.configuration.detailedMatchFailures(), httpRequest)
					: null, httpRequest))
				{
					matchingExpectation = httpRequestMatcher.getExpectation();
					httpRequestMatcher.setResponseInProgress(true);
					if(matchingExpectation.decrementRemainingMatches())
					{
						remainingMatchesDecremented = true;
					}
				}
				else if(!httpRequestMatcher.isResponseInProgress() && !httpRequestMatcher.isActive())
				{
					this.scheduler.submit(() -> this.removeHttpRequestMatcher(
						httpRequestMatcher,
						UUIDService.getUUID()));
				}
				if(remainingMatchesDecremented)
				{
					this.notifyListeners(this, Cause.API);
				}
				return matchingExpectation;
			})
			.filter(Objects::nonNull)
			.findFirst();
		return first.orElse(null);
	}
	
	public void clear(final RequestDefinition requestDefinition)
	{
		if(requestDefinition != null)
		{
			final HttpRequestMatcher clearHttpRequestMatcher =
				this.matcherBuilder.transformsToMatcher(requestDefinition);
			this.getHttpRequestMatchersCopy().forEach(httpRequestMatcher -> {
				RequestDefinition request = httpRequestMatcher
					.getExpectation()
					.getHttpRequest();
				if(isNotBlank(requestDefinition.getLogCorrelationId()))
				{
					request = request
						.shallowClone()
						.withLogCorrelationId(requestDefinition.getLogCorrelationId());
				}
				if(clearHttpRequestMatcher.matches(request))
				{
					this.removeHttpRequestMatcher(httpRequestMatcher, requestDefinition.getLogCorrelationId());
				}
			});
			if(LOG.isInfoEnabled())
			{
				LOG.info("Cleared expectations that match: {}", requestDefinition);
			}
		}
		else
		{
			this.reset();
		}
	}
	
	public void clear(final ExpectationId expectationId, final String logCorrelationId)
	{
		if(expectationId != null)
		{
			this.httpRequestMatchers
				.getByKey(expectationId.getId())
				.ifPresent(httpRequestMatcher -> this.removeHttpRequestMatcher(httpRequestMatcher, logCorrelationId));
			if(LOG.isInfoEnabled())
			{
				LOG.info("Cleared expectations that have id: {}", expectationId.getId());
			}
		}
		else
		{
			this.reset();
		}
	}
	
	Expectation postProcess(final Expectation expectation)
	{
		if(expectation != null)
		{
			this.getHttpRequestMatchersCopy()
				.filter(httpRequestMatcher -> httpRequestMatcher.getExpectation() == expectation)
				.findFirst()
				.ifPresent(httpRequestMatcher -> {
					if(!expectation.isActive())
					{
						this.removeHttpRequestMatcher(httpRequestMatcher, UUIDService.getUUID());
					}
					httpRequestMatcher.setResponseInProgress(false);
				});
		}
		return expectation;
	}
	
	private void removeHttpRequestMatcher(final HttpRequestMatcher httpRequestMatcher, final String logCorrelationId)
	{
		this.removeHttpRequestMatcher(httpRequestMatcher, Cause.API, true, logCorrelationId);
	}
	
	@SuppressWarnings("rawtypes")
	private void removeHttpRequestMatcher(
		final HttpRequestMatcher httpRequestMatcher,
		final Cause cause,
		final boolean notifyAndUpdateMetrics,
		final String logCorrelationId)
	{
		if(this.httpRequestMatchers.remove(httpRequestMatcher))
		{
			if(httpRequestMatcher.getExpectation() != null && LOG.isInfoEnabled())
			{
				final Expectation expectation = httpRequestMatcher.getExpectation().clone();
				LOG.info(REMOVED_EXPECTATION_MESSAGE_FORMAT, expectation, expectation.getId());
			}
			if(httpRequestMatcher.getExpectation() != null)
			{
				final Action action = httpRequestMatcher.getExpectation().getAction();
				if(action instanceof final HttpObjectCallback callback)
				{
					this.webSocketClientRegistry.unregisterClient(callback.getClientId());
				}
			}
			if(notifyAndUpdateMetrics)
			{
				this.notifyListeners(this, cause);
			}
		}
	}
	
	public Stream<RequestDefinition> retrieveRequestDefinitions(final List<ExpectationId> expectationIds)
	{
		return expectationIds
			.stream()
			.map(expectationId -> {
				if(isBlank(expectationId.getId()))
				{
					throw new IllegalArgumentException(
						"No expectation id specified found \"" + expectationId.getId() + "\"");
				}
				if(this.expectationRequestDefinitions.containsKey(expectationId.getId()))
				{
					return this.expectationRequestDefinitions.get(expectationId.getId());
				}
				else
				{
					throw new IllegalArgumentException("No expectation found with id " + expectationId.getId());
				}
			})
			.filter(Objects::nonNull);
	}
	
	public List<Expectation> retrieveActiveExpectations(final RequestDefinition requestDefinition)
	{
		if(requestDefinition == null)
		{
			return this.httpRequestMatchers.stream()
				.map(HttpRequestMatcher::getExpectation)
				.collect(Collectors.toList());
		}
		else
		{
			final List<Expectation> expectations = new ArrayList<>();
			final HttpRequestMatcher requestMatcher = this.matcherBuilder.transformsToMatcher(requestDefinition);
			this.getHttpRequestMatchersCopy().forEach(httpRequestMatcher -> {
				if(requestMatcher.matches(httpRequestMatcher.getExpectation().getHttpRequest()))
				{
					expectations.add(httpRequestMatcher.getExpectation());
				}
			});
			return expectations;
		}
	}
	
	public List<HttpRequestMatcher> retrieveRequestMatchers(final RequestDefinition requestDefinition)
	{
		if(requestDefinition == null)
		{
			return this.httpRequestMatchers.stream().collect(Collectors.toList());
		}
		else
		{
			final List<HttpRequestMatcher> httpRequestMatchers = new ArrayList<>();
			final HttpRequestMatcher requestMatcher = this.matcherBuilder.transformsToMatcher(requestDefinition);
			this.getHttpRequestMatchersCopy().forEach(httpRequestMatcher -> {
				if(requestMatcher.matches(httpRequestMatcher.getExpectation().getHttpRequest()))
				{
					httpRequestMatchers.add(httpRequestMatcher);
				}
			});
			return httpRequestMatchers;
		}
	}
	
	public boolean isEmpty()
	{
		return this.httpRequestMatchers.isEmpty();
	}
	
	@Override
	protected void notifyListeners(final RequestMatchers notifier, final Cause cause)
	{
		super.notifyListeners(notifier, cause);
	}
	
	private Stream<HttpRequestMatcher> getHttpRequestMatchersCopy()
	{
		return this.httpRequestMatchers.stream();
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final RequestMatchers that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.httpRequestMatchers, that.httpRequestMatchers)
			&& Objects.equals(this.expectationRequestDefinitions, that.expectationRequestDefinitions)
			&& Objects.equals(this.configuration, that.configuration)
			&& Objects.equals(this.scheduler, that.scheduler)
			&& Objects.equals(this.webSocketClientRegistry, that.webSocketClientRegistry)
			&& Objects.equals(this.matcherBuilder, that.matcherBuilder);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			super.hashCode(),
			this.httpRequestMatchers,
			this.expectationRequestDefinitions,
			this.configuration,
			this.scheduler,
			this.webSocketClientRegistry,
			this.matcherBuilder);
	}
}
