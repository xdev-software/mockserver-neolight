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

import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.logging.LoggingMessages.RECEIVED_REQUEST_MESSAGE_FORMAT;
import static software.xdev.mockserver.model.HttpRequest.request;
import static software.xdev.mockserver.model.HttpResponse.response;
import static software.xdev.mockserver.util.StringUtils.defaultIfEmpty;
import static software.xdev.mockserver.util.StringUtils.isEmpty;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.event.EventBus;
import software.xdev.mockserver.event.model.EventEntry;
import software.xdev.mockserver.mock.listeners.MockServerMatcherNotifier.Cause;
import software.xdev.mockserver.model.Action;
import software.xdev.mockserver.model.ClearType;
import software.xdev.mockserver.model.ExpectationId;
import software.xdev.mockserver.model.Format;
import software.xdev.mockserver.model.HttpError;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.MediaType;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.model.RetrieveType;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.serialization.ExpectationIdSerializer;
import software.xdev.mockserver.serialization.ExpectationSerializer;
import software.xdev.mockserver.serialization.LogEventRequestAndResponseSerializer;
import software.xdev.mockserver.serialization.RequestDefinitionSerializer;
import software.xdev.mockserver.serialization.VerificationSequenceSerializer;
import software.xdev.mockserver.serialization.VerificationSerializer;
import software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer;
import software.xdev.mockserver.uuid.UUIDService;
import software.xdev.mockserver.verify.Verification;
import software.xdev.mockserver.verify.VerificationSequence;


public class HttpState
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpState.class);
	
	public static final String PATH_PREFIX = "/mockserver";
	private static final ThreadLocal<Integer> LOCAL_PORT = new ThreadLocal<>();
	private final String uniqueLoopPreventionHeaderValue = "MockServer_" + UUIDService.getUUID();
	private final EventBus eventBus;
	private final Scheduler scheduler;
	// mockserver
	private final RequestMatchers requestMatchers;
	private final ServerConfiguration configuration;
	private final WebSocketClientRegistry webSocketClientRegistry;
	// serializers
	private ExpectationIdSerializer expectationIdSerializer;
	private RequestDefinitionSerializer requestDefinitionSerializer;
	private LogEventRequestAndResponseSerializer httpRequestResponseSerializer;
	private ExpectationSerializer expectationSerializer;
	private ExpectationSerializer expectationSerializerThatSerializesBodyDefault;
	private ExpectationToJavaSerializer expectationToJavaSerializer;
	private VerificationSerializer verificationSerializer;
	private VerificationSequenceSerializer verificationSequenceSerializer;
	
	public static void setPort(final HttpRequest request)
	{
		if(request != null && request.getSocketAddress() != null)
		{
			setPort(request.getSocketAddress().getPort());
			request.withSocketAddress(null);
		}
	}
	
	public static void setPort(final Integer port)
	{
		LOCAL_PORT.set(port);
	}
	
	public static void setPort(final Integer... port)
	{
		if(port != null && port.length > 0)
		{
			setPort(port[0]);
		}
	}
	
	public static void setPort(final List<Integer> port)
	{
		if(port != null && !port.isEmpty())
		{
			setPort(port.get(0));
		}
	}
	
	public static Integer getPort()
	{
		return LOCAL_PORT.get();
	}
	
	public HttpState(final ServerConfiguration configuration, final Scheduler scheduler)
	{
		this.configuration = configuration;
		this.scheduler = scheduler;
		this.webSocketClientRegistry = new WebSocketClientRegistry(configuration);
		LocalCallbackRegistry.setMaxWebSocketExpectations(configuration.maxWebSocketExpectations());
		this.eventBus = new EventBus(configuration, scheduler, true);
		this.requestMatchers = new RequestMatchers(configuration, scheduler, this.webSocketClientRegistry);
		if(LOG.isTraceEnabled())
		{
			LOG.trace("Log ring buffer created, with size {}", configuration.ringBufferSize());
		}
	}
	
	public void clear(final HttpRequest request)
	{
		final String logCorrelationId = UUIDService.getUUID();
		RequestDefinition requestDefinition = null;
		ExpectationId expectationId = null;
		if(isNotBlank(request.getBodyAsString()))
		{
			final String body = request.getBodyAsJsonOrXmlString();
			try
			{
				expectationId = this.getExpectationIdSerializer().deserialize(body);
			}
			catch(final Throwable throwable)
			{
				// assume not expectationId
				requestDefinition = this.getRequestDefinitionSerializer().deserialize(body);
			}
			if(expectationId != null)
			{
				requestDefinition = this.resolveExpectationId(expectationId);
			}
		}
		if(requestDefinition != null)
		{
			requestDefinition.withLogCorrelationId(logCorrelationId);
		}
		try
		{
			final ClearType type =
				ClearType.valueOf(defaultIfEmpty(request.getFirstQueryStringParameter("type").toUpperCase(), "ALL"));
			switch(type)
			{
				case LOG:
					this.eventBus.clear(requestDefinition);
					break;
				case EXPECTATIONS:
					if(expectationId != null)
					{
						this.requestMatchers.clear(expectationId, logCorrelationId);
					}
					else
					{
						this.requestMatchers.clear(requestDefinition);
					}
					break;
				case ALL:
					this.eventBus.clear(requestDefinition);
					if(expectationId != null)
					{
						this.requestMatchers.clear(expectationId, logCorrelationId);
					}
					else
					{
						this.requestMatchers.clear(requestDefinition);
					}
					break;
				default:
					throw new UnsupportedOperationException();
			}
		}
		catch(final IllegalArgumentException iae)
		{
			throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("type")
				+ "\" is not a valid value for \"type\" parameter, only the following values are supported "
				+ Arrays.stream(ClearType.values())
				.map(input -> input.name().toLowerCase())
				.collect(Collectors.toList()));
		}
	}
	
	private RequestDefinition resolveExpectationId(final ExpectationId expectationId)
	{
		return this.requestMatchers
			.retrieveRequestDefinitions(Collections.singletonList(expectationId))
			.findFirst()
			.orElse(null);
	}
	
	private List<RequestDefinition> resolveExpectationIds(final List<ExpectationId> expectationIds)
	{
		return this.requestMatchers
			.retrieveRequestDefinitions(expectationIds)
			.collect(Collectors.toList());
	}
	
	public void reset()
	{
		this.requestMatchers.reset();
		this.eventBus.reset();
		this.webSocketClientRegistry.reset();
		if(LOG.isInfoEnabled())
		{
			LOG.info("Resetting all expectations and request logs");
		}
	}
	
	public List<Expectation> add(final Expectation... expectations)
	{
		final List<Expectation> upsertedExpectations = new ArrayList<>();
		for(final Expectation expectation : expectations)
		{
			upsertedExpectations.add(this.requestMatchers.add(expectation, Cause.API));
		}
		return upsertedExpectations;
	}
	
	public Expectation firstMatchingExpectation(final HttpRequest request)
	{
		if(this.requestMatchers.isEmpty())
		{
			return null;
		}
		else
		{
			return this.requestMatchers.firstMatchingExpectation(request);
		}
	}
	
	public List<Expectation> allMatchingExpectation(final HttpRequest request)
	{
		if(this.requestMatchers.isEmpty())
		{
			return Collections.emptyList();
		}
		else
		{
			return this.requestMatchers.retrieveActiveExpectations(request);
		}
	}
	
	public void postProcess(final Expectation expectation)
	{
		this.requestMatchers.postProcess(expectation);
	}
	
	@SuppressWarnings({"checkstyle:MethodLength", "checkstyle:MagicNumber"})
	public HttpResponse retrieve(final HttpRequest request)
	{
		final String logCorrelationId = UUIDService.getUUID();
		final CompletableFuture<HttpResponse> httpResponseFuture = new CompletableFuture<>();
		final HttpResponse response = response().withStatusCode(OK.code());
		if(request != null)
		{
			try
			{
				final RequestDefinition requestDefinition = isNotBlank(request.getBodyAsString())
					? this.getRequestDefinitionSerializer().deserialize(request.getBodyAsJsonOrXmlString())
					: request();
				requestDefinition.withLogCorrelationId(logCorrelationId);
				final Format format = Format.valueOf(defaultIfEmpty(
					request.getFirstQueryStringParameter("format").toUpperCase(),
					"JSON"));
				final RetrieveType type = RetrieveType.valueOf(defaultIfEmpty(
					request.getFirstQueryStringParameter("type").toUpperCase(),
					"REQUESTS"));
				
				this.logEvent(new EventEntry()
					.setType(EventEntry.EventType.RETRIEVED)
					.setCorrelationId(logCorrelationId)
					.setHttpRequest(requestDefinition));
				switch(type)
				{
					case REQUESTS:
					{
						if(LOG.isInfoEnabled())
						{
							LOG.info(
								"Retrieved requests in {} that match: {}",
								format.name().toLowerCase(),
								requestDefinition);
						}
						switch(format)
						{
							case JAVA:
								this.eventBus
									.retrieveRequests(
										requestDefinition,
										requests -> {
											response.withBody(
												this.getRequestDefinitionSerializer().serialize(requests),
												MediaType.create("application", "java").withCharset(UTF_8)
											);
											httpResponseFuture.complete(response);
										}
									);
								break;
							case JSON:
								this.eventBus
									.retrieveRequests(
										requestDefinition,
										requests -> {
											response.withBody(
												this.getRequestDefinitionSerializer().serialize(true, requests),
												MediaType.JSON_UTF_8
											);
											httpResponseFuture.complete(response);
										}
									);
								break;
							default:
								throw new UnsupportedOperationException();
						}
						break;
					}
					case REQUEST_RESPONSES:
					{
						if(LOG.isInfoEnabled())
						{
							LOG.info(
								"Retrieved requests and responses in {} that match: {}",
								format.name().toLowerCase(),
								requestDefinition);
						}
						switch(format)
						{
							case JAVA:
								response.withBody(
									"JAVA not supported for REQUEST_RESPONSES",
									MediaType.create("text", "plain").withCharset(UTF_8));
								httpResponseFuture.complete(response);
								break;
							case JSON:
								this.eventBus
									.retrieveRequestResponses(
										requestDefinition,
										httpRequestAndHttpResponses -> {
											response.withBody(
												this.getHttpRequestResponseSerializer()
													.serialize(httpRequestAndHttpResponses),
												MediaType.JSON_UTF_8
											);
											httpResponseFuture.complete(response);
										}
									);
								break;
							default:
								throw new UnsupportedOperationException();
						}
						break;
					}
					case RECORDED_EXPECTATIONS:
					{
						if(LOG.isInfoEnabled())
						{
							LOG.info(
								"Retrieved recorded expectations in {} that match: {}",
								format.name().toLowerCase(),
								requestDefinition);
						}
						switch(format)
						{
							case JAVA:
								this.eventBus
									.retrieveRecordedExpectations(
										requestDefinition,
										requests -> {
											response.withBody(
												this.getExpectationToJavaSerializer().serialize(requests),
												MediaType.create("application", "java").withCharset(UTF_8)
											);
											httpResponseFuture.complete(response);
										}
									);
								break;
							case JSON:
								this.eventBus
									.retrieveRecordedExpectations(
										requestDefinition,
										requests -> {
											response.withBody(
												this.getExpectationSerializerThatSerializesBodyDefault()
													.serialize(requests),
												MediaType.JSON_UTF_8
											);
											httpResponseFuture.complete(response);
										}
									);
								break;
							default:
								throw new UnsupportedOperationException();
						}
						break;
					}
					case ACTIVE_EXPECTATIONS:
					{
						final List<Expectation> expectations =
							this.requestMatchers.retrieveActiveExpectations(requestDefinition);
						switch(format)
						{
							case JAVA:
								response.withBody(
									this.getExpectationToJavaSerializer().serialize(expectations),
									MediaType.create("application", "java").withCharset(UTF_8));
								break;
							case JSON:
								response.withBody(
									this.getExpectationSerializer().serialize(expectations),
									MediaType.JSON_UTF_8);
								break;
							default:
								throw new UnsupportedOperationException();
						}
						if(LOG.isInfoEnabled())
						{
							LOG.info(
								"Retrieved {} active expectations in {} that match: {}",
								expectations.size(),
								format.name().toLowerCase(),
								requestDefinition);
						}
						httpResponseFuture.complete(response);
						break;
					}
					default:
						throw new UnsupportedOperationException();
				}
				
				try
				{
					return httpResponseFuture.get(this.configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
				}
				catch(final ExecutionException | InterruptedException | TimeoutException ex)
				{
					LOG.error("Exception handling request: {}", request, ex);
					throw new IllegalStateException("Exception retrieving state for " + request, ex);
				}
			}
			catch(final IllegalArgumentException iae)
			{
				LOG.error("Exception handling request: {}", request, iae);
				if(iae.getMessage().contains(RetrieveType.class.getSimpleName()))
				{
					throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("type")
						+ "\" is not a valid value for \"type\" parameter, only the following values are supported "
						+ Arrays.stream(RetrieveType.values())
						.map(input -> input.name().toLowerCase())
						.collect(Collectors.toList()));
				}
				if(iae.getMessage().contains(Format.class.getSimpleName()))
				{
					throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("format")
						+ "\" is not a valid value for \"format\" parameter, only the following values are supported "
						+ Arrays.stream(Format.values())
						.map(input -> input.name().toLowerCase())
						.collect(Collectors.toList()));
				}
				throw iae;
			}
		}
		else
		{
			return response().withStatusCode(200);
		}
	}
	
	public Future<String> verify(final Verification verification)
	{
		final CompletableFuture<String> result = new CompletableFuture<>();
		this.verify(verification, result::complete);
		return result;
	}
	
	public void verify(final Verification verification, final Consumer<String> resultConsumer)
	{
		if(verification.getExpectationId() != null)
		{
			// check valid expectation id and populate for error message
			verification.withRequest(this.resolveExpectationId(verification.getExpectationId()));
		}
		this.eventBus.verify(verification, resultConsumer);
	}
	
	public Future<String> verify(final VerificationSequence verification)
	{
		final CompletableFuture<String> result = new CompletableFuture<>();
		this.verify(verification, result::complete);
		return result;
	}
	
	public void verify(final VerificationSequence verificationSequence, final Consumer<String> resultConsumer)
	{
		if(verificationSequence.getExpectationIds() != null && !verificationSequence.getExpectationIds().isEmpty())
		{
			verificationSequence.withRequests(this.resolveExpectationIds(verificationSequence.getExpectationIds()));
		}
		this.eventBus.verify(verificationSequence, resultConsumer);
	}
	
	public boolean handle(final HttpRequest request, final ResponseWriter responseWriter, final boolean warDeployment)
	{
		
		request.withLogCorrelationId(UUIDService.getUUID());
		setPort(request);
		
		if(LOG.isTraceEnabled())
		{
			LOG.trace(RECEIVED_REQUEST_MESSAGE_FORMAT, request);
		}
		
		if(request.matches("PUT"))
		{
			
			final CompletableFuture<Boolean> canHandle = new CompletableFuture<>();
			
			if(request.matchesPath(PATH_PREFIX + "/expectation", "/expectation"))
			{
				
				if(this.controlPlaneRequestAuthenticated(request, responseWriter))
				{
					final List<Expectation> upsertedExpectations = new ArrayList<>();
					for(final Expectation expectation : this.getExpectationSerializer().deserializeArray(
						request.getBodyAsJsonOrXmlString(),
						false))
					{
						if(!warDeployment || this.validateSupportedFeatures(expectation, request, responseWriter))
						{
							upsertedExpectations.addAll(this.add(expectation));
						}
					}
					
					responseWriter.writeResponse(
						request,
						response()
							.withStatusCode(CREATED.code())
							.withBody(
								this.getExpectationSerializer().serialize(upsertedExpectations),
								MediaType.JSON_UTF_8),
						true);
				}
				canHandle.complete(true);
			}
			else if(request.matchesPath(PATH_PREFIX + "/clear", "/clear"))
			{
				
				if(this.controlPlaneRequestAuthenticated(request, responseWriter))
				{
					this.clear(request);
					responseWriter.writeResponse(request, OK);
				}
				canHandle.complete(true);
			}
			else if(request.matchesPath(PATH_PREFIX + "/reset", "/reset"))
			{
				
				if(this.controlPlaneRequestAuthenticated(request, responseWriter))
				{
					this.reset();
					responseWriter.writeResponse(request, OK);
				}
				canHandle.complete(true);
			}
			else if(request.matchesPath(PATH_PREFIX + "/retrieve", "/retrieve"))
			{
				
				if(this.controlPlaneRequestAuthenticated(request, responseWriter))
				{
					responseWriter.writeResponse(request, this.retrieve(request), true);
				}
				canHandle.complete(true);
			}
			else if(request.matchesPath(PATH_PREFIX + "/verify", "/verify"))
			{
				
				if(this.controlPlaneRequestAuthenticated(request, responseWriter))
				{
					this.verify(
						this.getVerificationSerializer().deserialize(request.getBodyAsJsonOrXmlString()),
						result -> {
							if(isEmpty(result))
							{
								responseWriter.writeResponse(request, ACCEPTED);
							}
							else
							{
								responseWriter.writeResponse(
									request,
									NOT_ACCEPTABLE,
									result,
									MediaType.create("text", "plain").toString());
							}
							canHandle.complete(true);
						});
				}
				else
				{
					canHandle.complete(true);
				}
			}
			else if(request.matchesPath(PATH_PREFIX + "/verifySequence", "/verifySequence"))
			{
				
				if(this.controlPlaneRequestAuthenticated(request, responseWriter))
				{
					this.verify(
						this.getVerificationSequenceSerializer().deserialize(request.getBodyAsJsonOrXmlString()),
						result -> {
							if(isEmpty(result))
							{
								responseWriter.writeResponse(request, ACCEPTED);
							}
							else
							{
								responseWriter.writeResponse(
									request,
									NOT_ACCEPTABLE,
									result,
									MediaType.create("text", "plain").toString());
							}
							canHandle.complete(true);
						});
				}
				else
				{
					canHandle.complete(true);
				}
			}
			else
			{
				canHandle.complete(false);
			}
			
			try
			{
				return canHandle.get(this.configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
			}
			catch(final InterruptedException | ExecutionException | TimeoutException ex)
			{
				LOG.error("Exception handling request: {}", request, ex);
				return false;
			}
		}
		else
		{
			
			return false;
		}
	}
	
	private boolean controlPlaneRequestAuthenticated(final HttpRequest request, final ResponseWriter responseWriter)
	{
		return true;
	}
	
	@SuppressWarnings("rawtypes")
	private boolean validateSupportedFeatures(
		final Expectation expectation,
		final HttpRequest request,
		final ResponseWriter responseWriter)
	{
		boolean valid = true;
		final Action action = expectation.getAction();
		final String notSupportedMessage =
			" is not supported by MockServer deployed as a WAR due to limitations in the JEE specification; use "
				+ "mockserver-netty to enable these features";
		if(action instanceof HttpResponse && ((HttpResponse)action).getConnectionOptions() != null)
		{
			valid = false;
			responseWriter.writeResponse(request, response("ConnectionOptions" + notSupportedMessage), true);
		}
		else if(action instanceof HttpObjectCallback)
		{
			valid = false;
			responseWriter.writeResponse(request, response("HttpObjectCallback" + notSupportedMessage), true);
		}
		else if(action instanceof HttpError)
		{
			valid = false;
			responseWriter.writeResponse(request, response("HttpError" + notSupportedMessage), true);
		}
		return valid;
	}
	
	public WebSocketClientRegistry getWebSocketClientRegistry()
	{
		return this.webSocketClientRegistry;
	}
	
	public RequestMatchers getRequestMatchers()
	{
		return this.requestMatchers;
	}
	
	public EventBus getEventBus()
	{
		return this.eventBus;
	}
	
	public void logEvent(final EventEntry entry)
	{
		this.getEventBus().add(entry);
	}
	
	public Scheduler getScheduler()
	{
		return this.scheduler;
	}
	
	public String getUniqueLoopPreventionHeaderName()
	{
		return "x-forwarded-by";
	}
	
	public String getUniqueLoopPreventionHeaderValue()
	{
		return this.uniqueLoopPreventionHeaderValue;
	}
	
	public void stop()
	{
		this.eventBus.stop();
	}
	
	private ExpectationIdSerializer getExpectationIdSerializer()
	{
		if(this.expectationIdSerializer == null)
		{
			this.expectationIdSerializer = new ExpectationIdSerializer();
		}
		return this.expectationIdSerializer;
	}
	
	private RequestDefinitionSerializer getRequestDefinitionSerializer()
	{
		if(this.requestDefinitionSerializer == null)
		{
			this.requestDefinitionSerializer = new RequestDefinitionSerializer();
		}
		return this.requestDefinitionSerializer;
	}
	
	private LogEventRequestAndResponseSerializer getHttpRequestResponseSerializer()
	{
		if(this.httpRequestResponseSerializer == null)
		{
			this.httpRequestResponseSerializer = new LogEventRequestAndResponseSerializer();
		}
		return this.httpRequestResponseSerializer;
	}
	
	private ExpectationSerializer getExpectationSerializer()
	{
		if(this.expectationSerializer == null)
		{
			this.expectationSerializer = new ExpectationSerializer();
		}
		return this.expectationSerializer;
	}
	
	private ExpectationSerializer getExpectationSerializerThatSerializesBodyDefault()
	{
		if(this.expectationSerializerThatSerializesBodyDefault == null)
		{
			this.expectationSerializerThatSerializesBodyDefault = new ExpectationSerializer(true);
		}
		return this.expectationSerializerThatSerializesBodyDefault;
	}
	
	private ExpectationToJavaSerializer getExpectationToJavaSerializer()
	{
		if(this.expectationToJavaSerializer == null)
		{
			this.expectationToJavaSerializer = new ExpectationToJavaSerializer();
		}
		return this.expectationToJavaSerializer;
	}
	
	private VerificationSerializer getVerificationSerializer()
	{
		if(this.verificationSerializer == null)
		{
			this.verificationSerializer = new VerificationSerializer();
		}
		return this.verificationSerializer;
	}
	
	private VerificationSequenceSerializer getVerificationSequenceSerializer()
	{
		if(this.verificationSequenceSerializer == null)
		{
			this.verificationSequenceSerializer = new VerificationSequenceSerializer();
		}
		return this.verificationSequenceSerializer;
	}
}
