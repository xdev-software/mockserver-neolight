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
package software.xdev.mockserver.client;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static software.xdev.mockserver.configuration.ClientConfiguration.clientConfiguration;
import static software.xdev.mockserver.formatting.StringFormatter.formatLogMessage;
import static software.xdev.mockserver.model.ExpectationId.expectationId;
import static software.xdev.mockserver.model.HttpRequest.request;
import static software.xdev.mockserver.model.MediaType.APPLICATION_JSON_UTF_8;
import static software.xdev.mockserver.model.PortBinding.portBinding;
import static software.xdev.mockserver.util.StringUtils.isEmpty;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.verify.Verification.verification;
import static software.xdev.mockserver.verify.VerificationTimes.exactly;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import software.xdev.mockserver.authentication.AuthenticationException;
import software.xdev.mockserver.client.MockServerClientEventBus.EventType;
import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.configuration.ClientConfiguration;
import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.httpclient.SocketConnectionException;
import software.xdev.mockserver.matchers.TimeToLive;
import software.xdev.mockserver.matchers.Times;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.model.ClearType;
import software.xdev.mockserver.model.ExpectationId;
import software.xdev.mockserver.model.Format;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.HttpStatusCode;
import software.xdev.mockserver.model.LogEventRequestAndResponse;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.model.RetrieveType;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;
import software.xdev.mockserver.scheduler.SchedulerThreadFactory;
import software.xdev.mockserver.serialization.ExpectationIdSerializer;
import software.xdev.mockserver.serialization.ExpectationSerializer;
import software.xdev.mockserver.serialization.LogEventRequestAndResponseSerializer;
import software.xdev.mockserver.serialization.PortBindingSerializer;
import software.xdev.mockserver.serialization.RequestDefinitionSerializer;
import software.xdev.mockserver.serialization.VerificationSequenceSerializer;
import software.xdev.mockserver.serialization.VerificationSerializer;
import software.xdev.mockserver.stop.Stoppable;
import software.xdev.mockserver.verify.Verification;
import software.xdev.mockserver.verify.VerificationSequence;
import software.xdev.mockserver.verify.VerificationTimes;


@SuppressWarnings({"UnusedReturnValue", "FieldMayBeFinal"})
public class MockServerClient implements Stoppable
{
	private static final Logger LOG = LoggerFactory.getLogger(MockServerClient.class);
	
	private static final Map<Integer, MockServerClientEventBus> EVENT_BUS_MAP = new ConcurrentHashMap<>();
	private final EventLoopGroup eventLoopGroup;
	private final String host;
	private final String contextPath;
	private final Class<MockServerClient> clientClass;
	protected CompletableFuture<Integer> portFuture;
	private Integer port;
	private HttpRequest requestOverride;
	private ClientConfiguration configuration;
	private ProxyConfiguration proxyConfiguration;
	private NettyHttpClient nettyHttpClient;
	private RequestDefinitionSerializer requestDefinitionSerializer = new RequestDefinitionSerializer();
	private ExpectationIdSerializer expectationIdSerializer = new ExpectationIdSerializer();
	private LogEventRequestAndResponseSerializer httpRequestResponseSerializer =
		new LogEventRequestAndResponseSerializer();
	private PortBindingSerializer portBindingSerializer = new PortBindingSerializer();
	private ExpectationSerializer expectationSerializer = new ExpectationSerializer();
	private VerificationSerializer verificationSerializer = new VerificationSerializer();
	private VerificationSequenceSerializer verificationSequenceSerializer = new VerificationSequenceSerializer();
	private final CompletableFuture<MockServerClient> stopFuture = new CompletableFuture<>();
	
	/**
	 * Start the client communicating to a MockServer on localhost at the port specified with the Future
	 *
	 * @param portFuture the port for the MockServer to communicate with
	 */
	@SuppressWarnings("checkstyle:FinalParameters")
	public MockServerClient(ClientConfiguration configuration, final CompletableFuture<Integer> portFuture)
	{
		if(configuration == null)
		{
			configuration = clientConfiguration();
		}
		this.clientClass = MockServerClient.class;
		this.host = "127.0.0.1";
		this.portFuture = portFuture;
		this.contextPath = "";
		this.configuration = configuration;
		this.eventLoopGroup = this.eventLoopGroup();
		LocalCallbackRegistry.setMaxWebSocketExpectations(configuration.maxWebSocketExpectations());
	}
	
	/**
	 * Start the client communicating to a MockServer at the specified host and port for example:
	 * <p>
	 * MockServerClient mockServerClient = new MockServerClient("localhost", 1080);
	 *
	 * @param host the host for the MockServer to communicate with
	 * @param port the port for the MockServer to communicate with
	 */
	public MockServerClient(final String host, final int port)
	{
		this(host, port, "");
	}
	
	/**
	 * Start the client communicating to a MockServer at the specified host and port for example:
	 * <p>
	 * MockServerClient mockServerClient = new MockServerClient("localhost", 1080);
	 *
	 * @param host the host for the MockServer to communicate with
	 * @param port the port for the MockServer to communicate with
	 */
	public MockServerClient(final ClientConfiguration configuration, final String host, final int port)
	{
		this(configuration, host, port, "");
	}
	
	/**
	 * Start the client communicating to a MockServer at the specified host and port and contextPath for example:
	 * <p>
	 * MockServerClient mockServerClient = new MockServerClient("localhost", 1080, "/mockserver");
	 *
	 * @param host        the host for the MockServer to communicate with
	 * @param port        the port for the MockServer to communicate with
	 * @param contextPath the context path that the MockServer war is deployed to
	 */
	public MockServerClient(final String host, final int port, final String contextPath)
	{
		this.clientClass = MockServerClient.class;
		if(isEmpty(host))
		{
			throw new IllegalArgumentException("Host can not be null or empty");
		}
		if(contextPath == null)
		{
			throw new IllegalArgumentException("ContextPath can not be null");
		}
		this.host = host;
		this.port = port;
		this.contextPath = contextPath;
		this.configuration = clientConfiguration();
		this.eventLoopGroup = this.eventLoopGroup();
		LocalCallbackRegistry.setMaxWebSocketExpectations(this.configuration.maxWebSocketExpectations());
	}
	
	/**
	 * Start the client communicating to a MockServer at the specified host and port and contextPath for example:
	 * <p>
	 * MockServerClient mockServerClient = new MockServerClient("localhost", 1080, "/mockserver");
	 *
	 * @param host        the host for the MockServer to communicate with
	 * @param port        the port for the MockServer to communicate with
	 * @param contextPath the context path that the MockServer war is deployed to
	 */
	@SuppressWarnings("checkstyle:FinalParameters")
	public MockServerClient(
		ClientConfiguration configuration,
		final String host,
		final int port,
		final String contextPath)
	{
		this.clientClass = MockServerClient.class;
		if(isEmpty(host))
		{
			throw new IllegalArgumentException("Host can not be null or empty");
		}
		if(contextPath == null)
		{
			throw new IllegalArgumentException("ContextPath can not be null");
		}
		if(configuration == null)
		{
			configuration = clientConfiguration();
		}
		this.configuration = configuration;
		this.host = host;
		this.port = port;
		this.contextPath = contextPath;
		this.eventLoopGroup = this.eventLoopGroup();
	}
	
	private NioEventLoopGroup eventLoopGroup()
	{
		return new NioEventLoopGroup(
			this.configuration.clientNioEventLoopThreadCount(),
			new SchedulerThreadFactory(this.getClass().getSimpleName() + "-eventLoop"));
	}
	
	/**
	 * @deprecated use withProxyConfiguration which is more consistent with MockServer API style
	 */
	@Deprecated
	public MockServerClient setProxyConfiguration(final ProxyConfiguration proxyConfiguration)
	{
		return this.withProxyConfiguration(proxyConfiguration);
	}
	
	/**
	 * Configure communication to MockServer to go via a proxy
	 */
	public MockServerClient withProxyConfiguration(final ProxyConfiguration proxyConfiguration)
	{
		this.proxyConfiguration = proxyConfiguration;
		return this;
	}
	
	/**
	 * @deprecated use withRequestOverride which is more consistent with MockServer API style
	 */
	@Deprecated
	public MockServerClient setRequestOverride(final HttpRequest requestOverride)
	{
		return this.withRequestOverride(requestOverride);
	}
	
	public MockServerClient withRequestOverride(final HttpRequest requestOverride)
	{
		if(requestOverride == null)
		{
			throw new IllegalArgumentException("Request with default properties can not be null");
		}
		
		this.requestOverride = requestOverride;
		return this;
	}
	
	private MockServerClientEventBus getMockServerEventBus()
	{
		if(EVENT_BUS_MAP.get(this.port()) == null)
		{
			EVENT_BUS_MAP.put(this.port(), new MockServerClientEventBus());
		}
		return EVENT_BUS_MAP.get(this.port());
	}
	
	private void removeMockServerEventBus()
	{
		EVENT_BUS_MAP.remove(this.port());
	}
	
	private int port()
	{
		if(this.port == null)
		{
			try
			{
				this.port = this.portFuture.get(this.configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
			}
			catch(final Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}
		return this.port;
	}
	
	public InetSocketAddress remoteAddress()
	{
		return new InetSocketAddress(this.host, this.port());
	}
	
	public String contextPath()
	{
		return this.contextPath;
	}
	
	public Integer getPort()
	{
		return this.port();
	}
	
	private String calculatePath(final String path)
	{
		String cleanedPath = "/mockserver/" + path;
		if(isNotBlank(this.contextPath))
		{
			cleanedPath =
				(!this.contextPath.startsWith("/") ? "/" : "")
					+ this.contextPath
					+ (!this.contextPath.endsWith("/") ? "/" : "")
					+ (cleanedPath.startsWith("/") ? cleanedPath.substring(1) : cleanedPath);
		}
		return (!cleanedPath.startsWith("/") ? "/" : "") + cleanedPath;
	}
	
	private NettyHttpClient getNettyHttpClient()
	{
		if(this.nettyHttpClient == null)
		{
			this.nettyHttpClient = new NettyHttpClient(
				this.configuration,
				this.eventLoopGroup,
				this.proxyConfiguration != null ? List.of(this.proxyConfiguration) : null,
				false
			);
		}
		return this.nettyHttpClient;
	}
	
	@SuppressWarnings({"checkstyle:FinalParameters", "checkstyle:MagicNumber"})
	private HttpResponse sendRequest(
		HttpRequest request,
		final boolean ignoreErrors,
		final boolean throwClientException)
	{
		if(!this.stopFuture.isDone())
		{
			try
			{
				if(!request.containsHeader(CONTENT_TYPE.toString())
					&& request.getBody() != null
					&& isNotBlank(request.getBody().getContentType()))
				{
					request.withHeader(CONTENT_TYPE.toString(), request.getBody().getContentType());
				}
				if(this.requestOverride != null)
				{
					request = request.update(this.requestOverride, null);
				}
				final HttpResponse response = this.getNettyHttpClient().sendRequest(
					request.withHeader(HOST.toString(), this.host + ":" + this.port()),
					this.configuration.maxSocketTimeoutInMillis(),
					TimeUnit.MILLISECONDS,
					ignoreErrors
				);
				
				if(response != null)
				{
					if(response.getStatusCode() != null)
					{
						if(response.getStatusCode() == BAD_REQUEST.code())
						{
							throw new IllegalArgumentException(response.getBodyAsString());
						}
						else if(response.getStatusCode() == UNAUTHORIZED.code())
						{
							throw new AuthenticationException(response.getBodyAsString());
						}
					}
				}
				
				if(throwClientException && response != null && response.getStatusCode() != null
					&& response.getStatusCode() >= 400)
				{
					throw new ClientException(formatLogMessage("error:{}while sending request:{}", response, request));
				}
				
				return response;
			}
			catch(final RuntimeException rex)
			{
				if(isNotBlank(rex.getMessage()) && (rex.getMessage().contains("executor not accepting a task")
					|| rex.getMessage().contains("loop shut down")))
				{
					throw new IllegalStateException(
						this.getClass().getSimpleName() + " has already been closed, please create new "
							+ this.getClass().getSimpleName() + " instance");
				}
				else
				{
					throw rex;
				}
			}
		}
		else
		{
			throw new IllegalStateException(
				this.getClass().getSimpleName() + " has already been stopped, please create new " + this.getClass()
					.getSimpleName() + " instance");
		}
	}
	
	private HttpResponse sendRequest(final HttpRequest request, final boolean throwClientException)
	{
		return this.sendRequest(request, false, throwClientException);
	}
	
	/**
	 * Returns whether MockServer has stopped, if called too quickly after starting MockServer this may return false
	 * because MockServer has not yet started, to ensure MockServer has started use hasStarted()
	 */
	@SuppressWarnings("checkstyle:MagicNumber")
	public boolean hasStopped()
	{
		return this.hasStopped(10, 500, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Returns whether server MockServer has stopped, by polling the MockServer a configurable amount of times.  If
	 * called too quickly after starting MockServer this may return false because MockServer has not yet started, to
	 * ensure MockServer has started use hasStarted()
	 */
	public boolean hasStopped(final int attempts, final long timeout, final TimeUnit timeUnit)
	{
		try
		{
			final HttpResponse httpResponse =
				this.sendRequest(request().withMethod("PUT").withPath(this.calculatePath("status")), true, false);
			if(httpResponse != null && httpResponse.getStatusCode() == HttpStatusCode.OK_200.code())
			{
				if(attempts <= 0)
				{
					return false;
				}
				else
				{
					try
					{
						timeUnit.sleep(timeout);
					}
					catch(final InterruptedException e)
					{
						// ignore interrupted exception
					}
					return this.hasStopped(attempts - 1, timeout, timeUnit);
				}
			}
			else
			{
				return true;
			}
		}
		catch(final SocketConnectionException | IllegalStateException sce)
		{
			return true;
		}
	}
	
	/**
	 * Returns whether MockServer has started, if called after MockServer has been stopped this method will block for 5
	 * seconds while confirming MockServer is not starting
	 */
	@SuppressWarnings("checkstyle:MagicNumber")
	public boolean hasStarted()
	{
		return this.hasStarted(10, 500, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Returns whether server MockServer has started, by polling the MockServer a configurable amount of times
	 */
	public boolean hasStarted(final int attempts, final long timeout, final TimeUnit timeUnit)
	{
		try
		{
			final HttpResponse httpResponse =
				this.sendRequest(request().withMethod("PUT").withPath(this.calculatePath("status")), false);
			if(httpResponse.getStatusCode() == HttpStatusCode.OK_200.code())
			{
				return true;
			}
			else if(attempts <= 0)
			{
				return false;
			}
			else
			{
				try
				{
					timeUnit.sleep(timeout);
				}
				catch(final InterruptedException e)
				{
					// ignore interrupted exception
				}
				return this.hasStarted(attempts - 1, timeout, timeUnit);
			}
		}
		catch(final SocketConnectionException | IllegalStateException sce)
		{
			if(attempts <= 0)
			{
				if(LOG.isDebugEnabled())
				{
					LOG.debug("Exception while checking if MockServer has started", sce);
				}
				return false;
			}
			else
			{
				try
				{
					timeUnit.sleep(timeout);
				}
				catch(final InterruptedException e)
				{
					// ignore interrupted exception
				}
				return this.hasStarted(attempts - 1, timeout, timeUnit);
			}
		}
	}
	
	/**
	 * Bind new ports to listen on
	 */
	public List<Integer> bind(final Integer... ports)
	{
		final String boundPorts = this.sendRequest(
			request()
				.withMethod("PUT")
				.withPath(this.calculatePath("bind"))
				.withBody(this.portBindingSerializer.serialize(portBinding(ports)), StandardCharsets.UTF_8),
			true
		).getBodyAsString();
		return this.portBindingSerializer.deserialize(boundPorts).getPorts();
	}
	
	/**
	 * Stop MockServer gracefully (only support for Netty version, not supported for WAR version)
	 */
	public Future<MockServerClient> stopAsync()
	{
		return this.stop(true);
	}
	
	/**
	 * Stop MockServer gracefully (only support for Netty version, not supported for WAR version)
	 */
	@Override
	public void stop()
	{
		try
		{
			this.stopAsync().get(10, SECONDS);
		}
		catch(final Exception ex)
		{
			if(LOG.isDebugEnabled())
			{
				LOG.debug("Exception while stopping", ex);
			}
		}
	}
	
	/**
	 * Stop MockServer gracefully (only support for Netty version, not supported for WAR version)
	 */
	@SuppressWarnings("checkstyle:MagicNumber")
	public CompletableFuture<MockServerClient> stop(final boolean ignoreFailure)
	{
		if(!this.stopFuture.isDone())
		{
			this.getMockServerEventBus().publish(EventType.STOP);
			this.removeMockServerEventBus();
			new SchedulerThreadFactory("ClientStop").newThread(() -> {
				try
				{
					this.sendRequest(request().withMethod("PUT").withPath(this.calculatePath("stop")), false);
					if(!this.hasStopped())
					{
						for(int i = 0; !this.hasStopped() && i < 50; i++)
						{
							TimeUnit.MILLISECONDS.sleep(5);
						}
					}
				}
				catch(final RejectedExecutionException ree)
				{
					if(!ignoreFailure && LOG.isTraceEnabled())
					{
						LOG.trace("Request rejected while closing down, logging in case due other error", ree);
					}
				}
				catch(final Exception e)
				{
					if(!ignoreFailure)
					{
						LOG.trace("Failed to send stop request to MockServer", e);
					}
				}
				if(!this.eventLoopGroup.isShuttingDown())
				{
					this.eventLoopGroup.shutdownGracefully();
				}
				this.stopFuture.complete(this.clientClass.cast(this));
			}).start();
		}
		return this.stopFuture;
	}
	
	@Override
	public void close()
	{
		this.stop();
	}
	
	/**
	 * Reset MockServer by clearing all expectations
	 */
	public MockServerClient reset()
	{
		this.getMockServerEventBus().publish(EventType.RESET);
		this.sendRequest(
			request()
				.withMethod("PUT")
				.withPath(this.calculatePath("reset")),
			true
		);
		return this.clientClass.cast(this);
	}
	
	/**
	 * Clear all expectations and logs that match the request matcher
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to clear each
	 *                             expectation
	 *                          if null all expectations are cleared
	 */
	public MockServerClient clear(final RequestDefinition requestDefinition)
	{
		this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("clear"))
				.withBody(
					requestDefinition != null ? this.requestDefinitionSerializer.serialize(requestDefinition) : "",
					StandardCharsets.UTF_8),
			true
		);
		return this.clientClass.cast(this);
	}
	
	/**
	 * Clear all expectations and logs that match the expectation id
	 *
	 * @param expectationId the expectation id that is used to clear expectations and logs
	 */
	public MockServerClient clear(final String expectationId)
	{
		return this.clear(expectationId(expectationId));
	}
	
	/**
	 * Clear all expectations and logs that match the expectation id
	 *
	 * @param expectationId the expectation id that is used to clear expectations and logs
	 */
	public MockServerClient clear(final ExpectationId expectationId)
	{
		this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("clear"))
				.withBody(
					expectationId != null ? this.expectationIdSerializer.serialize(expectationId) : "",
					StandardCharsets.UTF_8),
			true
		);
		return this.clientClass.cast(this);
	}
	
	/**
	 * Clear expectations, logs or both that match the request matcher
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to clear each
	 *                             expectation
	 *                          if null all expectations are cleared
	 * @param type              the type to clear, EXPECTATION, LOG or BOTH
	 */
	public MockServerClient clear(final RequestDefinition requestDefinition, final ClearType type)
	{
		this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("clear"))
				.withQueryStringParameter("type", type.name().toLowerCase())
				.withBody(
					requestDefinition != null ? this.requestDefinitionSerializer.serialize(requestDefinition) : "",
					StandardCharsets.UTF_8),
			true
		);
		return this.clientClass.cast(this);
	}
	
	/**
	 * Clear expectations, logs or both that match the expectation id
	 *
	 * @param expectationId the expectation id that is used to clear expectations and logs
	 * @param type          the type to clear, EXPECTATION, LOG or BOTH
	 */
	public MockServerClient clear(final String expectationId, final ClearType type)
	{
		return this.clear(expectationId(expectationId), type);
	}
	
	/**
	 * Clear expectations, logs or both that match the expectation id
	 *
	 * @param expectationId the expectation id that is used to clear expectations and logs
	 * @param type          the type to clear, EXPECTATION, LOG or BOTH
	 */
	public MockServerClient clear(final ExpectationId expectationId, final ClearType type)
	{
		this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("clear"))
				.withQueryStringParameter("type", type.name().toLowerCase())
				.withBody(
					expectationId != null ? this.expectationIdSerializer.serialize(expectationId) : "",
					StandardCharsets.UTF_8),
			true
		);
		return this.clientClass.cast(this);
	}
	
	/**
	 * Verify a list of requests have been sent in the order specified for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/first_request")
	 *          .withBody("some_request_body"),
	 *      request()
	 *          .withPath("/second_request")
	 *          .withBody("some_request_body")
	 *  );
	 * </pre>
	 *
	 * @param requestDefinitions the http requests that must be matched for this verification to pass
	 * @throws AssertionError if the request has not been found
	 */
	public MockServerClient verify(final RequestDefinition... requestDefinitions) throws AssertionError
	{
		return this.verify(null, requestDefinitions);
	}
	
	/**
	 * Verify a list of requests have been sent in the order specified for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/first_request")
	 *          .withBody("some_request_body"),
	 *      request()
	 *          .withPath("/second_request")
	 *          .withBody("some_request_body")
	 *  );
	 * </pre>
	 *
	 * @param maximumNumberOfRequestToReturnInVerificationFailure the maximum number requests return in the error
	 *                                                            response when the verification fails
	 * @param requestDefinitions                                  the http requests that must be matched for this
	 *                                                            verification to pass
	 */
	public MockServerClient verify(
		final Integer maximumNumberOfRequestToReturnInVerificationFailure,
		final RequestDefinition... requestDefinitions) throws AssertionError
	{
		if(requestDefinitions == null || requestDefinitions.length == 0 || requestDefinitions[0] == null)
		{
			throw new IllegalArgumentException(
				"verify(RequestDefinition...) requires a non-null non-empty array of RequestDefinition objects");
		}
		
		final VerificationSequence verificationSequence = new VerificationSequence()
			.withRequests(requestDefinitions)
			.withMaximumNumberOfRequestToReturnInVerificationFailure(
				maximumNumberOfRequestToReturnInVerificationFailure);
		final String result = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("verifySequence"))
				.withBody(this.verificationSequenceSerializer.serialize(verificationSequence), StandardCharsets.UTF_8),
			false
		).getBodyAsString();
		
		if(result != null && !result.isEmpty())
		{
			throw new IllegalStateException(result);
		}
		return this.clientClass.cast(this);
	}
	
	/**
	 * Verify a list of requests have been sent in the order specified for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/first_request")
	 *          .withBody("some_request_body"),
	 *      request()
	 *          .withPath("/second_request")
	 *          .withBody("some_request_body")
	 *  );
	 * </pre>
	 *
	 * @param expectationIds the http requests that must be matched for this verification to pass
	 * @throws AssertionError if the request has not been found
	 */
	public MockServerClient verify(final String... expectationIds) throws AssertionError
	{
		return this.verify(Arrays.stream(expectationIds)
			.map(ExpectationId::expectationId)
			.toArray(ExpectationId[]::new));
	}
	
	/**
	 * Verify a list of requests have been sent in the order specified for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/first_request")
	 *          .withBody("some_request_body"),
	 *      request()
	 *          .withPath("/second_request")
	 *          .withBody("some_request_body")
	 *  );
	 * </pre>
	 *
	 * @param expectationIds the http requests that must be matched for this verification to pass
	 * @throws AssertionError if the request has not been found
	 */
	public MockServerClient verify(final ExpectationId... expectationIds) throws AssertionError
	{
		return this.verify(null, expectationIds);
	}
	
	/**
	 * Verify a list of requests have been sent in the order specified for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/first_request")
	 *          .withBody("some_request_body"),
	 *      request()
	 *          .withPath("/second_request")
	 *          .withBody("some_request_body")
	 *  );
	 * </pre>
	 *
	 * @param maximumNumberOfRequestToReturnInVerificationFailure the maximum number requests return in the error
	 *                                                            response when the verification fails
	 * @param expectationIds                                      the http requests that must be matched for this
	 *                                                            verification to pass
	 */
	public MockServerClient verify(
		final Integer maximumNumberOfRequestToReturnInVerificationFailure,
		final ExpectationId... expectationIds) throws AssertionError
	{
		if(expectationIds == null || expectationIds.length == 0 || expectationIds[0] == null)
		{
			throw new IllegalArgumentException(
				"verify(ExpectationId...) requires a non-null non-empty array of ExpectationId objects");
		}
		
		final VerificationSequence verificationSequence = new VerificationSequence()
			.withExpectationIds(expectationIds)
			.withMaximumNumberOfRequestToReturnInVerificationFailure(
				maximumNumberOfRequestToReturnInVerificationFailure);
		final String result = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("verifySequence"))
				.withBody(this.verificationSequenceSerializer.serialize(verificationSequence), StandardCharsets.UTF_8),
			false
		).getBodyAsString();
		
		if(result != null && !result.isEmpty())
		{
			throw new IllegalStateException(result);
		}
		return this.clientClass.cast(this);
	}
	
	/**
	 * Verify a request has been sent for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      VerificationTimes.exactly(3)
	 *  );
	 * </pre>
	 * VerificationTimes supports multiple static factory methods:
	 * <p>
	 * once()      - verify the request was only received once exactly(n)  - verify the request was only received
	 * exactly n times atLeast(n)  - verify the request was only received at least n times
	 *
	 * @param requestDefinition the http request that must be matched for this verification to pass
	 * @param times             the number of times this request must be matched
	 * @throws AssertionError if the request has not been found
	 */
	public MockServerClient verify(final RequestDefinition requestDefinition, final VerificationTimes times)
		throws AssertionError
	{
		return this.verify(requestDefinition, times, null);
	}
	
	/**
	 * Verify a request has been sent for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      VerificationTimes.exactly(3)
	 *  );
	 * </pre>
	 * VerificationTimes supports multiple static factory methods:
	 * <p>
	 * once()      - verify the request was only received once exactly(n)  - verify the request was only received
	 * exactly n times atLeast(n)  - verify the request was only received at least n times
	 *
	 * @param requestDefinition                                   the http request that must be matched for this
	 *                                                            verification to pass
	 * @param times                                               the number of times this request must be matched
	 * @param maximumNumberOfRequestToReturnInVerificationFailure the maximum number requests return in the error
	 *                                                            response when the verification fails
	 * @throws AssertionError if the request has not been found
	 */
	public MockServerClient verify(
		final RequestDefinition requestDefinition,
		final VerificationTimes times,
		final Integer maximumNumberOfRequestToReturnInVerificationFailure) throws AssertionError
	{
		if(requestDefinition == null)
		{
			throw new IllegalArgumentException(
				"verify(RequestDefinition, VerificationTimes) requires a non null RequestDefinition object");
		}
		if(times == null)
		{
			throw new IllegalArgumentException(
				"verify(RequestDefinition, VerificationTimes) requires a non null VerificationTimes object");
		}
		
		final Verification verification = verification()
			.withRequest(requestDefinition)
			.withTimes(times)
			.withMaximumNumberOfRequestToReturnInVerificationFailure(
				maximumNumberOfRequestToReturnInVerificationFailure);
		final String result = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("verify"))
				.withBody(this.verificationSerializer.serialize(verification), StandardCharsets.UTF_8),
			false
		).getBodyAsString();
		
		if(result != null && !result.isEmpty())
		{
			throw new IllegalStateException(result);
		}
		return this.clientClass.cast(this);
	}
	
	/**
	 * Verify a request has been sent for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      VerificationTimes.exactly(3)
	 *  );
	 * </pre>
	 * VerificationTimes supports multiple static factory methods:
	 * <p>
	 * once()      - verify the request was only received once exactly(n)  - verify the request was only received
	 * exactly n times atLeast(n)  - verify the request was only received at least n times
	 *
	 * @param expectationId the http request that must be matched for this verification to pass
	 * @param times         the number of times this request must be matched
	 * @throws AssertionError if the request has not been found
	 */
	public MockServerClient verify(final String expectationId, final VerificationTimes times) throws AssertionError
	{
		return this.verify(expectationId(expectationId), times);
	}
	
	/**
	 * Verify a request has been sent for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      VerificationTimes.exactly(3)
	 *  );
	 * </pre>
	 * VerificationTimes supports multiple static factory methods:
	 * <p>
	 * once()      - verify the request was only received once exactly(n)  - verify the request was only received
	 * exactly n times atLeast(n)  - verify the request was only received at least n times
	 *
	 * @param expectationId the http request that must be matched for this verification to pass
	 * @param times         the number of times this request must be matched
	 * @throws AssertionError if the request has not been found
	 */
	public MockServerClient verify(final ExpectationId expectationId, final VerificationTimes times)
		throws AssertionError
	{
		return this.verify(expectationId, times, null);
	}
	
	/**
	 * Verify a request has been sent for example:
	 * <pre>
	 * mockServerClient
	 *  .verify(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      VerificationTimes.exactly(3)
	 *  );
	 * </pre>
	 * VerificationTimes supports multiple static factory methods:
	 * <p>
	 * once()      - verify the request was only received once exactly(n)  - verify the request was only received
	 * exactly n times atLeast(n)  - verify the request was only received at least n times
	 *
	 * @param expectationId                                       the http request that must be matched for this
	 *                                                            verification to pass
	 * @param times                                               the number of times this request must be matched
	 * @param maximumNumberOfRequestToReturnInVerificationFailure the maximum number requests return in the error
	 *                                                            response when the verification fails
	 * @throws AssertionError if the request has not been found
	 */
	public MockServerClient verify(
		final ExpectationId expectationId,
		final VerificationTimes times,
		final Integer maximumNumberOfRequestToReturnInVerificationFailure) throws AssertionError
	{
		if(expectationId == null)
		{
			throw new IllegalArgumentException(
				"verify(ExpectationId, VerificationTimes) requires a non null ExpectationId object");
		}
		if(times == null)
		{
			throw new IllegalArgumentException(
				"verify(ExpectationId, VerificationTimes) requires a non null VerificationTimes object");
		}
		
		final Verification verification = verification()
			.withExpectationId(expectationId)
			.withTimes(times)
			.withMaximumNumberOfRequestToReturnInVerificationFailure(
				maximumNumberOfRequestToReturnInVerificationFailure);
		final String result = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("verify"))
				.withBody(this.verificationSerializer.serialize(verification), StandardCharsets.UTF_8),
			false
		).getBodyAsString();
		
		if(result != null && !result.isEmpty())
		{
			throw new IllegalStateException(result);
		}
		return this.clientClass.cast(this);
	}
	
	/**
	 * Verify no requests have been sent.
	 *
	 * @throws AssertionError if any request has been found
	 */
	@SuppressWarnings({"DuplicatedCode", "UnusedReturnValue"})
	public MockServerClient verifyZeroInteractions() throws AssertionError
	{
		final Verification verification = verification().withRequest(request()).withTimes(exactly(0));
		final String result = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("verify"))
				.withBody(this.verificationSerializer.serialize(verification), StandardCharsets.UTF_8),
			false
		).getBodyAsString();
		
		if(result != null && !result.isEmpty())
		{
			throw new IllegalStateException(result);
		}
		return this.clientClass.cast(this);
	}
	
	/**
	 * Retrieve the recorded requests that match the httpRequest parameter, use null for the parameter to retrieve all
	 * requests
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to return each request,
	 *                          use null for the parameter to retrieve for all requests
	 * @return an array of all requests that have been recorded by the MockServer in the order they have been received
	 * and including duplicates where the same request has been received multiple times
	 */
	public HttpRequest[] retrieveRecordedRequests(final RequestDefinition requestDefinition)
	{
		RequestDefinition[] requestDefinitions = new RequestDefinition[0];
		final String recordedRequests = this.retrieveRecordedRequests(requestDefinition, Format.JSON);
		if(isNotBlank(recordedRequests) && !recordedRequests.equals("[]"))
		{
			requestDefinitions = this.requestDefinitionSerializer.deserializeArray(recordedRequests);
		}
		return Arrays.stream(requestDefinitions).map(HttpRequest.class::cast).toArray(HttpRequest[]::new);
	}
	
	/**
	 * Retrieve the recorded requests that match the httpRequest parameter, use null for the parameter to retrieve all
	 * requests
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to return each request,
	 *                          use null for the parameter to retrieve for all requests
	 * @param format            the format to retrieve the expectations, either JAVA or JSON
	 * @return an array of all requests that have been recorded by the MockServer in the order they have been received
	 * and including duplicates where the same request has been received multiple times
	 */
	public String retrieveRecordedRequests(final RequestDefinition requestDefinition, final Format format)
	{
		final HttpResponse httpResponse = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("retrieve"))
				.withQueryStringParameter("type", RetrieveType.REQUESTS.name())
				.withQueryStringParameter("format", format.name())
				.withBody(
					requestDefinition != null ? this.requestDefinitionSerializer.serialize(requestDefinition) : "",
					StandardCharsets.UTF_8),
			true
		);
		return httpResponse.getBodyAsString();
	}
	
	/**
	 * Retrieve the recorded requests and responses that match the httpRequest parameter, use null for the parameter to
	 * retrieve all requests and responses
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to return each request
	 *                          (and its corresponding response), use null for the parameter to retrieve for all
	 *                          requests
	 * @return an array of all requests and responses that have been recorded by the MockServer in the order they have
	 * been received and including duplicates where the same request has been received multiple times
	 */
	public LogEventRequestAndResponse[] retrieveRecordedRequestsAndResponses(final RequestDefinition requestDefinition)
	{
		final String recordedRequests = this.retrieveRecordedRequestsAndResponses(requestDefinition, Format.JSON);
		if(isNotBlank(recordedRequests) && !recordedRequests.equals("[]"))
		{
			return this.httpRequestResponseSerializer.deserializeArray(recordedRequests);
		}
		else
		{
			return new LogEventRequestAndResponse[0];
		}
	}
	
	/**
	 * Retrieve the recorded requests that match the httpRequest parameter, use null for the parameter to retrieve all
	 * requests
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to return each request,
	 *                          use null for the parameter to retrieve for all requests
	 * @param format            the format to retrieve the expectations, either JAVA or JSON
	 * @return an array of all requests that have been recorded by the MockServer in the order they have been received
	 * and including duplicates where the same request has been received multiple times
	 */
	public String retrieveRecordedRequestsAndResponses(final RequestDefinition requestDefinition, final Format format)
	{
		final HttpResponse httpResponse = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("retrieve"))
				.withQueryStringParameter("type", RetrieveType.REQUEST_RESPONSES.name())
				.withQueryStringParameter("format", format.name())
				.withBody(
					requestDefinition != null ? this.requestDefinitionSerializer.serialize(requestDefinition) : "",
					StandardCharsets.UTF_8),
			true
		);
		return httpResponse.getBodyAsString();
	}
	
	/**
	 * Retrieve the request-response combinations that have been recorded as a list of expectations, only those that
	 * match the httpRequest parameter are returned, use null to retrieve all requests
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to return each request,
	 *                          use null for the parameter to retrieve for all requests
	 * @return an array of all expectations that have been recorded by the MockServer in the order they have been
	 * received and including duplicates where the same request has been received multiple times
	 */
	public Expectation[] retrieveRecordedExpectations(final RequestDefinition requestDefinition)
	{
		final String recordedExpectations = this.retrieveRecordedExpectations(requestDefinition, Format.JSON);
		if(isNotBlank(recordedExpectations) && !recordedExpectations.equals("[]"))
		{
			return this.expectationSerializer.deserializeArray(recordedExpectations, true);
		}
		else
		{
			return new Expectation[0];
		}
	}
	
	/**
	 * Retrieve the request-response combinations that have been recorded as a list of expectations, only those that
	 * match the httpRequest parameter are returned, use null to retrieve all requests
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to return each request,
	 *                          use null for the parameter to retrieve for all requests
	 * @param format            the format to retrieve the expectations, either JAVA or JSON
	 * @return an array of all expectations that have been recorded by the MockServer in the order they have been
	 * received and including duplicates where the same request has been received multiple times
	 */
	public String retrieveRecordedExpectations(final RequestDefinition requestDefinition, final Format format)
	{
		final HttpResponse httpResponse = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("retrieve"))
				.withQueryStringParameter("type", RetrieveType.RECORDED_EXPECTATIONS.name())
				.withQueryStringParameter("format", format.name())
				.withBody(
					requestDefinition != null ? this.requestDefinitionSerializer.serialize(requestDefinition) : "",
					StandardCharsets.UTF_8),
			true
		);
		return httpResponse.getBodyAsString();
	}
	
	/**
	 * Specify an unlimited expectation that will respond regardless of the number of matching http for example:
	 * <pre>
	 * mockServerClient
	 *  .when(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body")
	 *  )
	 *  .respond(
	 *      response()
	 *          .withBody("some_response_body")
	 *          .withHeader("responseName", "responseValue")
	 *  )
	 * </pre>
	 *
	 * @param requestDefinition the http request that must be matched for this expectation to respond
	 * @return an Expectation object that can be used to specify the response
	 */
	public ForwardChainExpectation when(final RequestDefinition requestDefinition)
	{
		return this.when(requestDefinition, Times.unlimited());
	}
	
	/**
	 * Specify a limited expectation that will respond a specified number of times when the http is matched
	 * <p>
	 * Example use:
	 * <pre>
	 * mockServerClient
	 *  .when(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      Times.exactly(5)
	 *  )
	 *  .respond(
	 *      response()
	 *          .withBody("some_response_body")
	 *          .withHeader("responseName", "responseValue")
	 *  )
	 * </pre>
	 *
	 * @param requestDefinition the http request that must be matched for this expectation to respond
	 * @param times             the number of times to respond when this http is matched
	 * @return an Expectation object that can be used to specify the response
	 */
	public ForwardChainExpectation when(final RequestDefinition requestDefinition, final Times times)
	{
		return new ForwardChainExpectation(
			this.configuration,
			this.getMockServerEventBus(),
			this,
			new Expectation(requestDefinition, times, TimeToLive.unlimited(), 0));
	}
	
	/**
	 * Specify a limited expectation that will respond a specified number of times when the http is matched
	 * <p>
	 * Example use:
	 * <pre>
	 * mockServerClient
	 *  .when(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      Times.exactly(5),
	 *      TimeToLive.exactly(TimeUnit.SECONDS, 120)
	 *  )
	 *  .respond(
	 *      response()
	 *          .withBody("some_response_body")
	 *          .withHeader("responseName", "responseValue")
	 *  )
	 * </pre>
	 *
	 * @param requestDefinition the http request that must be matched for this expectation to respond
	 * @param times             the number of times to respond when this http is matched
	 * @param timeToLive        the length of time from when the server receives the expectation that the expectation
	 *                          should be active
	 * @return an Expectation object that can be used to specify the response
	 */
	public ForwardChainExpectation when(
		final RequestDefinition requestDefinition,
		final Times times,
		final TimeToLive timeToLive)
	{
		return new ForwardChainExpectation(
			this.configuration,
			this.getMockServerEventBus(),
			this,
			new Expectation(requestDefinition, times, timeToLive, 0));
	}
	
	/**
	 * Specify a limited expectation that will respond a specified number of times when the http is matched and will be
	 * matched according to priority as follows:
	 * <p>
	 * - higher priority expectation will be matched first - identical priority expectations will be match in the order
	 * they were submitted - default priority is 0
	 * <p>
	 * Example use:
	 * <pre>
	 * mockServerClient
	 *  .when(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      Times.exactly(5),
	 *      TimeToLive.exactly(TimeUnit.SECONDS, 120),
	 *      10
	 *  )
	 *  .respond(
	 *      response()
	 *          .withBody("some_response_body")
	 *          .withHeader("responseName", "responseValue")
	 *  )
	 * </pre>
	 *
	 * @param requestDefinition the http request that must be matched for this expectation to respond
	 * @param times             the number of times to respond when this http is matched
	 * @param timeToLive        the length of time from when the server receives the expectation that the expectation
	 *                          should be active
	 * @param priority          the priority for the expectation when matching, higher priority expectation will be
	 *                          matched first, identical priority expectations will be match in the order they were
	 *                          submitted
	 * @return an Expectation object that can be used to specify the response
	 */
	public ForwardChainExpectation when(
		final RequestDefinition requestDefinition,
		final Times times,
		final TimeToLive timeToLive,
		final Integer priority)
	{
		return new ForwardChainExpectation(
			this.configuration,
			this.getMockServerEventBus(),
			this,
			new Expectation(requestDefinition, times, timeToLive, priority));
	}
	
	/**
	 * Specify one or more expectations to be create, or updated (if the id matches).
	 * <p>
	 * This method should be used to update existing expectation by id.  All fields will be updated for expectations
	 * with a matching id as the existing expectation is deleted and recreated.
	 * <p>
	 * To retrieve the id(s) for existing expectation(s) the retrieveActiveExpectations(HttpRequest httpRequest) method
	 * can be used.
	 * <p>
	 * Typically, to create expectations this method should not be used directly instead the when(...) and response(
	 * ...) or forward(...) or error(...) methods should be used for example:
	 * <pre>
	 * mockServerClient
	 *  .when(
	 *      request()
	 *          .withPath("/some_path")
	 *          .withBody("some_request_body"),
	 *      Times.exactly(5),
	 *      TimeToLive.exactly(TimeUnit.SECONDS, 120)
	 *  )
	 *  .respond(
	 *      response()
	 *          .withBody("some_response_body")
	 *          .withHeader("responseName", "responseValue")
	 *  )
	 * </pre>
	 *
	 * @param expectations one or more expectations to create or update (if the id field matches)
	 * @return upserted expectations
	 */
	@SuppressWarnings("checkstyle:MagicNumber")
	public Expectation[] upsert(final Expectation... expectations)
	{
		if(expectations != null)
		{
			HttpResponse httpResponse = null;
			if(expectations.length == 1)
			{
				httpResponse =
					this.sendRequest(
						request()
							.withMethod("PUT")
							.withContentType(APPLICATION_JSON_UTF_8)
							.withPath(this.calculatePath("expectation"))
							.withBody(this.expectationSerializer.serialize(expectations[0]), StandardCharsets.UTF_8),
						false
					);
				if(httpResponse != null && httpResponse.getStatusCode() != 201)
				{
					throw new ClientException(formatLogMessage(
						"error:{}while submitted expectation:{}",
						httpResponse,
						expectations[0]));
				}
			}
			else if(expectations.length > 1)
			{
				httpResponse =
					this.sendRequest(
						request()
							.withMethod("PUT")
							.withContentType(APPLICATION_JSON_UTF_8)
							.withPath(this.calculatePath("expectation"))
							.withBody(this.expectationSerializer.serialize(expectations), StandardCharsets.UTF_8),
						false
					);
				if(httpResponse != null && httpResponse.getStatusCode() != 201)
				{
					throw new ClientException(formatLogMessage(
						"error:{}while submitted expectations:{}",
						httpResponse,
						expectations));
				}
			}
			if(httpResponse != null && isNotBlank(httpResponse.getBodyAsString()))
			{
				return this.expectationSerializer.deserializeArray(httpResponse.getBodyAsString(), true);
			}
		}
		return new Expectation[0];
	}
	
	/**
	 * Retrieve the active expectations match the httpRequest parameter, use null for the parameter to retrieve all
	 * expectations
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to return each
	 *                          expectation, use null for the parameter to retrieve for all requests
	 * @return an array of all expectations that have been setup and have not expired
	 */
	public Expectation[] retrieveActiveExpectations(final RequestDefinition requestDefinition)
	{
		final String activeExpectations = this.retrieveActiveExpectations(requestDefinition, Format.JSON);
		if(isNotBlank(activeExpectations) && !activeExpectations.equals("[]"))
		{
			return this.expectationSerializer.deserializeArray(activeExpectations, true);
		}
		else
		{
			return new Expectation[0];
		}
	}
	
	/**
	 * Retrieve the active expectations match the httpRequest parameter, use null for the parameter to retrieve all
	 * expectations
	 *
	 * @param requestDefinition the http request that is matched against when deciding whether to return each
	 *                          expectation, use null for the parameter to retrieve for all requests
	 * @param format            the format to retrieve the expectations, either JAVA or JSON
	 * @return an array of all expectations that have been setup and have not expired
	 */
	public String retrieveActiveExpectations(final RequestDefinition requestDefinition, final Format format)
	{
		final HttpResponse httpResponse = this.sendRequest(
			request()
				.withMethod("PUT")
				.withContentType(APPLICATION_JSON_UTF_8)
				.withPath(this.calculatePath("retrieve"))
				.withQueryStringParameter("type", RetrieveType.ACTIVE_EXPECTATIONS.name())
				.withQueryStringParameter("format", format.name())
				.withBody(
					requestDefinition != null ? this.requestDefinitionSerializer.serialize(requestDefinition) : "",
					StandardCharsets.UTF_8),
			false
		);
		return httpResponse.getBodyAsString();
	}
}
