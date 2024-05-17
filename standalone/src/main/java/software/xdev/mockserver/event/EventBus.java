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

import static java.util.concurrent.TimeUnit.SECONDS;
import static software.xdev.mockserver.event.model.EventEntry.EventType.EXPECTATION_RESPONSE;
import static software.xdev.mockserver.event.model.EventEntry.EventType.FORWARDED_REQUEST;
import static software.xdev.mockserver.event.model.EventEntry.EventType.NO_MATCH_RESPONSE;
import static software.xdev.mockserver.event.model.EventEntry.EventType.RECEIVED_REQUEST;
import static software.xdev.mockserver.event.model.EventEntry.EventType.RUNNABLE;
import static software.xdev.mockserver.logging.LoggingMessages.VERIFICATION_REQUESTS_MESSAGE_FORMAT;
import static software.xdev.mockserver.logging.LoggingMessages.VERIFICATION_REQUEST_SEQUENCES_MESSAGE_FORMAT;
import static software.xdev.mockserver.model.HttpRequest.request;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


public class EventBus extends MockServerEventLogNotifier
{
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
	private final CircularConcurrentLinkedDeque<EventEntry> eventLog;
	private final MatcherBuilder matcherBuilder;
	private final RequestDefinitionSerializer requestDefinitionSerializer;
	private final boolean asynchronousEventProcessing;
	private Disruptor<EventEntry> disruptor;
	
	public EventBus(
		final ServerConfiguration configuration,
		final Scheduler scheduler,
		final boolean asynchronousEventProcessing)
	{
		super(scheduler);
		this.configuration = configuration;
		this.matcherBuilder = new MatcherBuilder(configuration);
		this.requestDefinitionSerializer = new RequestDefinitionSerializer();
		this.asynchronousEventProcessing = asynchronousEventProcessing;
		this.eventLog = new CircularConcurrentLinkedDeque<>(configuration.maxLogEntries(), EventEntry::clear);
		this.startRingBuffer();
	}
	
	public void add(final EventEntry eventEntry)
	{
		if(this.asynchronousEventProcessing)
		{
			if(!this.disruptor.getRingBuffer().tryPublishEvent(eventEntry))
			{
				LOG.warn("Too many log events failed to add log event to ring buffer: {}", eventEntry);
			}
		}
		else
		{
			this.processLogEntry(eventEntry);
		}
	}
	
	public int size()
	{
		return this.eventLog.size();
	}
	
	private void startRingBuffer()
	{
		this.disruptor =
			new Disruptor<>(
				EventEntry::new,
				this.configuration.ringBufferSize(),
				new SchedulerThreadFactory("EventLog"));
		
		final ExceptionHandler<EventEntry> errorHandler = new ExceptionHandler<>()
		{
			@Override
			public void handleEventException(final Throwable ex, final long sequence, final EventEntry logEntry)
			{
				LOG.error("exception handling log entry in log ring buffer, for log entry: {}", logEntry, ex);
			}
			
			@Override
			public void handleOnStartException(final Throwable ex)
			{
				LOG.error("exception starting log ring buffer", ex);
			}
			
			@Override
			public void handleOnShutdownException(final Throwable ex)
			{
				LOG.error("exception during shutdown of log ring buffer", ex);
			}
		};
		this.disruptor.setDefaultExceptionHandler(errorHandler);
		
		this.disruptor.handleEventsWith((eventEntry, sequence, endOfBatch) -> {
			if(eventEntry.getType() != RUNNABLE)
			{
				this.processLogEntry(eventEntry);
			}
			else
			{
				eventEntry.getConsumer().run();
				eventEntry.clear();
			}
		});
		
		this.disruptor.start();
	}
	
	private void processLogEntry(EventEntry eventEntry)
	{
		eventEntry = eventEntry.cloneAndClear();
		this.eventLog.add(eventEntry);
		this.notifyListeners(this, false);
	}
	
	public void stop()
	{
		try
		{
			this.notifyListeners(this, true);
			this.eventLog.clear();
			this.disruptor.shutdown(2, SECONDS);
		}
		catch(final Exception ex)
		{
			if(!(ex instanceof com.lmax.disruptor.TimeoutException) && LOG.isWarnEnabled())
			{
				LOG.warn("Exception while shutting down log ring buffer", ex);
			}
		}
	}
	
	public void reset()
	{
		final CompletableFuture<String> future = new CompletableFuture<>();
		this.disruptor.publishEvent(new EventEntry()
			.setType(RUNNABLE)
			.setConsumer(() -> {
				this.eventLog.clear();
				future.complete("done");
				this.notifyListeners(this, false);
			})
		);
		try
		{
			future.get(2, SECONDS);
		}
		catch(final ExecutionException | InterruptedException | TimeoutException ignore)
		{
		}
	}
	
	public void clear(final RequestDefinition requestDefinition)
	{
		final CompletableFuture<String> future = new CompletableFuture<>();
		final boolean markAsDeletedOnly = LOG.isInfoEnabled();
		this.disruptor.publishEvent(new EventEntry()
			.setType(RUNNABLE)
			.setConsumer(() -> {
				final String logCorrelationId = UUIDService.getUUID();
				final RequestDefinition matcher =
					requestDefinition != null ? requestDefinition : request().withLogCorrelationId(logCorrelationId);
				final HttpRequestMatcher requestMatcher = this.matcherBuilder.transformsToMatcher(matcher);
				for(final EventEntry eventEntry : new LinkedList<>(this.eventLog))
				{
					final RequestDefinition[] requests = eventEntry.getHttpRequests();
					boolean matches = false;
					if(requests != null)
					{
						for(final RequestDefinition request : requests)
						{
							if(requestMatcher.matches(request.cloneWithLogCorrelationId()))
							{
								matches = true;
							}
						}
					}
					else
					{
						matches = true;
					}
					if(matches)
					{
						if(markAsDeletedOnly)
						{
							eventEntry.setDeleted(true);
						}
						else
						{
							this.eventLog.removeItem(eventEntry);
						}
					}
				}
				if(LOG.isInfoEnabled())
				{
					LOG.info("Cleared logs that match: {}", requestDefinition);
				}
				future.complete("done");
				this.notifyListeners(this, false);
			})
		);
		try
		{
			future.get(2, SECONDS);
		}
		catch(final ExecutionException | InterruptedException | TimeoutException ignore)
		{
		}
	}
	
	public void retrieveRequests(
		final Verification verification,
		final String logCorrelationId,
		final Consumer<List<RequestDefinition>> listConsumer)
	{
		if(verification.getExpectationId() != null)
		{
			this.retrieveLogEntries(
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
		}
		else
		{
			this.retrieveLogEntries(
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
	
	public void retrieveAllRequests(
		final boolean matchingExpectationsOnly,
		final Consumer<List<RequestDefinition>> listConsumer)
	{
		if(matchingExpectationsOnly)
		{
			this.retrieveLogEntries(
				(List<String>)null,
				EXPECTATION_LOG_PREDICATE,
				LOG_ENTRY_TO_REQUEST,
				logEventStream -> listConsumer.accept(
					logEventStream
						.filter(Objects::nonNull)
						.flatMap(Arrays::stream)
						.collect(Collectors.toList())
				)
			);
		}
		else
		{
			this.retrieveLogEntries(
				(RequestDefinition)null,
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
	
	public void retrieveAllRequests(
		final List<String> expectationIds,
		final Consumer<List<RequestAndExpectationId>> listConsumer)
	{
		this.retrieveLogEntries(
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
	
	public void retrieveRequests(
		final RequestDefinition requestDefinition,
		final Consumer<List<RequestDefinition>> listConsumer)
	{
		this.retrieveLogEntries(
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
	
	public void retrieveRequestResponses(
		final RequestDefinition requestDefinition,
		final Consumer<List<LogEventRequestAndResponse>> listConsumer)
	{
		this.retrieveLogEntries(
			requestDefinition,
			REQUEST_RESPONSE_LOG_PREDICATE,
			LOG_ENTRY_TO_HTTP_REQUEST_AND_HTTP_RESPONSE,
			logEventStream -> listConsumer.accept(logEventStream.filter(Objects::nonNull).collect(Collectors.toList()))
		);
	}
	
	public void retrieveRecordedExpectations(
		final RequestDefinition requestDefinition,
		final Consumer<List<Expectation>> listConsumer)
	{
		this.retrieveLogEntries(
			requestDefinition,
			RECORDED_EXPECTATION_LOG_PREDICATE,
			LOG_ENTRY_TO_EXPECTATION,
			logEventStream -> listConsumer.accept(logEventStream.filter(Objects::nonNull).collect(Collectors.toList()))
		);
	}
	
	private void retrieveLogEntries(
		final RequestDefinition requestDefinition,
		final Predicate<EventEntry> logEntryPredicate,
		final Consumer<Stream<EventEntry>> consumer)
	{
		this.disruptor.publishEvent(new EventEntry()
			.setType(RUNNABLE)
			.setConsumer(() -> {
				final HttpRequestMatcher httpRequestMatcher =
					this.matcherBuilder.transformsToMatcher(requestDefinition);
				consumer.accept(this.eventLog
					.stream()
					.filter(logItem -> logItem.matches(httpRequestMatcher))
					.filter(logEntryPredicate)
				);
			})
		);
	}
	
	private <T> void retrieveLogEntries(
		final RequestDefinition requestDefinition,
		final Predicate<EventEntry> logEntryPredicate,
		final Function<EventEntry, T> logEntryMapper,
		final Consumer<Stream<T>> consumer)
	{
		this.disruptor.publishEvent(new EventEntry()
			.setType(RUNNABLE)
			.setConsumer(() -> {
				final RequestDefinition requestDefinitionMatcher = requestDefinition != null ?
					requestDefinition :
					request().withLogCorrelationId(UUIDService.getUUID());
				final HttpRequestMatcher httpRequestMatcher =
					this.matcherBuilder.transformsToMatcher(requestDefinitionMatcher);
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
	private <T> void retrieveLogEntries(
		final List<String> expectationIds,
		final Predicate<EventEntry> logEntryPredicate,
		final Function<EventEntry, T> logEntryMapper,
		final Consumer<Stream<T>> consumer)
	{
		this.disruptor.publishEvent(new EventEntry()
			.setType(RUNNABLE)
			.setConsumer(() -> consumer.accept(this.eventLog
				.stream()
				.filter(logEntryPredicate)
				.filter(logItem -> expectationIds == null || logItem.matchesAnyExpectationId(expectationIds))
				.map(logEntryMapper)
			))
		);
	}
	
	public Future<String> verify(final Verification verification)
	{
		final CompletableFuture<String> result = new CompletableFuture<>();
		this.verify(verification, result::complete);
		return result;
	}
	
	public void verify(final Verification verification, final Consumer<String> resultConsumer)
	{
		final String logCorrelationId = UUIDService.getUUID();
		if(verification != null)
		{
			if(LOG.isInfoEnabled())
			{
				LOG.info(VERIFICATION_REQUESTS_MESSAGE_FORMAT, verification);
			}
			this.retrieveRequests(verification, logCorrelationId, httpRequests -> {
				try
				{
					if(!verification.getTimes().matches(httpRequests.size()))
					{
						final boolean matchByExpectationId = verification.getExpectationId() != null;
						this.retrieveAllRequests(matchByExpectationId, allRequests -> {
							final String failureMessage;
							final String serializedRequestToBeVerified =
								this.requestDefinitionSerializer.serialize(true, verification.getHttpRequest());
							final Integer maximumNumberOfRequestToReturnInVerificationFailure =
								verification.getMaximumNumberOfRequestToReturnInVerificationFailure() != null ?
									verification.getMaximumNumberOfRequestToReturnInVerificationFailure() :
									this.configuration.maximumNumberOfRequestToReturnInVerificationFailure();
							if(allRequests.size() < maximumNumberOfRequestToReturnInVerificationFailure)
							{
								final String serializedAllRequestInLog = allRequests.size() == 1 ?
									this.requestDefinitionSerializer.serialize(true, allRequests.get(0)) :
									this.requestDefinitionSerializer.serialize(true, allRequests);
								failureMessage = "Request not found " + verification.getTimes() + ", expected:<"
									+ serializedRequestToBeVerified + "> but was:<" + serializedAllRequestInLog + ">";
							}
							else
							{
								failureMessage = "Request not found " + verification.getTimes() + ", expected:<"
									+ serializedRequestToBeVerified + "> but was not found, found " + allRequests.size()
									+ " other requests";
							}
							if(LOG.isInfoEnabled())
							{
								LOG.info(
									"Request not found {}, expected: {} but was: {}",
									verification.getTimes(),
									verification.getHttpRequest(),
									allRequests.size() == 1 ? allRequests.get(0) : allRequests);
							}
							resultConsumer.accept(failureMessage);
						});
					}
					else
					{
						if(LOG.isInfoEnabled())
						{
							LOG.info("request:{} found {}", verification.getHttpRequest(), verification.getTimes());
						}
						resultConsumer.accept("");
					}
				}
				catch(final Exception ex)
				{
					LOG.error("exception while processing verification: {}", verification, ex);
					resultConsumer.accept("exception while processing verification" + (isNotBlank(ex.getMessage()) ?
						" " + ex.getMessage() :
						""));
				}
			});
		}
		else
		{
			resultConsumer.accept("");
		}
	}
	
	public Future<String> verify(final VerificationSequence verification)
	{
		final CompletableFuture<String> result = new CompletableFuture<>();
		this.verify(verification, result::complete);
		return result;
	}
	
	public void verify(final VerificationSequence verificationSequence, final Consumer<String> resultConsumer)
	{
		if(verificationSequence != null)
		{
			final String logCorrelationId = UUIDService.getUUID();
			if(LOG.isInfoEnabled())
			{
				LOG.info(VERIFICATION_REQUEST_SEQUENCES_MESSAGE_FORMAT, verificationSequence);
			}
			if(verificationSequence.getExpectationIds() != null && !verificationSequence.getExpectationIds().isEmpty())
			{
				this.retrieveAllRequests(verificationSequence.getExpectationIds()
					.stream()
					.map(ExpectationId::getId)
					.collect(Collectors.toList()), allRequests -> {
					final List<RequestDefinition> requestDefinitions = allRequests.stream()
						.map(RequestAndExpectationId::getRequestDefinition)
						.collect(Collectors.toList());
					try
					{
						String failureMessage = "";
						int requestLogCounter = 0;
						for(final ExpectationId expectationId : verificationSequence.getExpectationIds())
						{
							if(expectationId != null)
							{
								boolean foundRequest = false;
								for(; !foundRequest && requestLogCounter < allRequests.size(); requestLogCounter++)
								{
									if(allRequests.get(requestLogCounter).matches(expectationId))
									{
										// move on to next request
										foundRequest = true;
									}
								}
								if(!foundRequest)
								{
									failureMessage = this.verificationSequenceFailureMessage(
										verificationSequence,
										logCorrelationId,
										requestDefinitions);
									break;
								}
							}
						}
						this.verificationSequenceSuccessMessage(
							verificationSequence,
							resultConsumer,
							logCorrelationId,
							failureMessage);
					}
					catch(final Exception ex)
					{
						this.verificationSequenceExceptionHandler(
							verificationSequence,
							resultConsumer,
							logCorrelationId,
							ex,
							"exception while processing verification sequence:{}",
							"exception while processing verification sequence");
					}
				});
			}
			else
			{
				this.retrieveAllRequests(false, allRequests -> {
					try
					{
						String failureMessage = "";
						int requestLogCounter = 0;
						for(final RequestDefinition verificationHttpRequest : verificationSequence.getHttpRequests())
						{
							if(verificationHttpRequest != null)
							{
								verificationHttpRequest.withLogCorrelationId(logCorrelationId);
								final HttpRequestMatcher httpRequestMatcher =
									this.matcherBuilder.transformsToMatcher(verificationHttpRequest);
								boolean foundRequest = false;
								for(; !foundRequest && requestLogCounter < allRequests.size(); requestLogCounter++)
								{
									if(httpRequestMatcher.matches(allRequests.get(requestLogCounter)
										.cloneWithLogCorrelationId()))
									{
										// move on to next request
										foundRequest = true;
									}
								}
								if(!foundRequest)
								{
									failureMessage = this.verificationSequenceFailureMessage(
										verificationSequence,
										logCorrelationId,
										allRequests);
									break;
								}
							}
						}
						this.verificationSequenceSuccessMessage(
							verificationSequence,
							resultConsumer,
							logCorrelationId,
							failureMessage);
					}
					catch(final Exception ex)
					{
						this.verificationSequenceExceptionHandler(
							verificationSequence,
							resultConsumer,
							logCorrelationId,
							ex,
							"exception:{} while processing verification sequence:{}",
							"exception while processing verification sequence");
					}
				});
			}
		}
		else
		{
			resultConsumer.accept("");
		}
	}
	
	private void verificationSequenceSuccessMessage(
		final VerificationSequence verificationSequence,
		final Consumer<String> resultConsumer,
		final String logCorrelationId,
		final String failureMessage)
	{
		if(isBlank(failureMessage) && LOG.isInfoEnabled())
		{
			LOG.info("request sequence found: {}", verificationSequence.getHttpRequests());
		}
		resultConsumer.accept(failureMessage);
	}
	
	private String verificationSequenceFailureMessage(
		final VerificationSequence verificationSequence,
		final String logCorrelationId,
		final List<RequestDefinition> allRequests)
	{
		final String failureMessage;
		final String serializedRequestToBeVerified =
			this.requestDefinitionSerializer.serialize(true, verificationSequence.getHttpRequests());
		final Integer maximumNumberOfRequestToReturnInVerificationFailure =
			verificationSequence.getMaximumNumberOfRequestToReturnInVerificationFailure() != null ?
				verificationSequence.getMaximumNumberOfRequestToReturnInVerificationFailure() :
				this.configuration.maximumNumberOfRequestToReturnInVerificationFailure();
		if(allRequests.size() < maximumNumberOfRequestToReturnInVerificationFailure)
		{
			final String serializedAllRequestInLog = allRequests.size() == 1 ?
				this.requestDefinitionSerializer.serialize(true, allRequests.get(0)) :
				this.requestDefinitionSerializer.serialize(true, allRequests);
			failureMessage = "Request sequence not found, expected:<" + serializedRequestToBeVerified + "> but was:<"
				+ serializedAllRequestInLog + ">";
		}
		else
		{
			failureMessage =
				"Request sequence not found, expected:<" + serializedRequestToBeVerified + "> but was not found, "
					+ "found "
					+ allRequests.size() + " other requests";
		}
		if(LOG.isInfoEnabled())
		{
			LOG.info(
				"Request sequence not found, expected: {} but was: {}",
				verificationSequence.getHttpRequests(),
				allRequests.size() == 1 ? allRequests.get(0) : allRequests);
		}
		return failureMessage;
	}
	
	private void verificationSequenceExceptionHandler(
		final VerificationSequence verificationSequence,
		final Consumer<String> resultConsumer,
		final String logCorrelationId,
		final Exception ex,
		final String s,
		final String s2)
	{
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
		return this.asynchronousEventProcessing == that.asynchronousEventProcessing
			&& Objects.equals(this.configuration, that.configuration)
			&& Objects.equals(this.eventLog, that.eventLog)
			&& Objects.equals(this.matcherBuilder, that.matcherBuilder)
			&& Objects.equals(this.requestDefinitionSerializer, that.requestDefinitionSerializer);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			super.hashCode(),
			this.configuration,
			this.eventLog,
			this.matcherBuilder,
			this.requestDefinitionSerializer,
			this.asynchronousEventProcessing);
	}
}
