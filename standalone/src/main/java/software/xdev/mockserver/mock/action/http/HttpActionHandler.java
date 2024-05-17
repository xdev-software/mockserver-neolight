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
package software.xdev.mockserver.mock.action.http;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpHeaderNames.PROXY_AUTHENTICATE;
import static io.netty.handler.codec.http.HttpHeaderNames.PROXY_AUTHORIZATION;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionException;
import static software.xdev.mockserver.exception.ExceptionHandling.sslHandshakeException;
import static software.xdev.mockserver.logging.LoggingMessages.NO_MATCH_RESPONSE_ERROR_MESSAGE_FORMAT;
import static software.xdev.mockserver.logging.LoggingMessages.NO_MATCH_RESPONSE_NO_EXPECTATION_MESSAGE_FORMAT;
import static software.xdev.mockserver.logging.LoggingMessages.RECEIVED_REQUEST_MESSAGE_FORMAT;
import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;
import static software.xdev.mockserver.model.HttpResponse.response;
import static software.xdev.mockserver.util.StringUtils.isEmpty;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.AttributeKey;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.cors.CORSHeaders;
import software.xdev.mockserver.event.model.EventEntry;
import software.xdev.mockserver.filters.HopByHopHeaderFilter;
import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.httpclient.SocketCommunicationException;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.model.Action;
import software.xdev.mockserver.model.HttpClassCallback;
import software.xdev.mockserver.model.HttpError;
import software.xdev.mockserver.model.HttpForward;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpOverrideForwardedRequest;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.scheduler.Scheduler;


@SuppressWarnings({"rawtypes", "FieldMayBeFinal"})
public class HttpActionHandler
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpActionHandler.class);
	
	public static final AttributeKey<InetSocketAddress> REMOTE_SOCKET = AttributeKey.valueOf("REMOTE_SOCKET");
	
	private final ServerConfiguration configuration;
	private final HttpState httpStateHandler;
	private final Scheduler scheduler;
	private HttpResponseActionHandler httpResponseActionHandler;
	private HttpResponseClassCallbackActionHandler httpResponseClassCallbackActionHandler;
	private HttpResponseObjectCallbackActionHandler httpResponseObjectCallbackActionHandler;
	private HttpForwardActionHandler httpForwardActionHandler;
	private HttpForwardClassCallbackActionHandler httpForwardClassCallbackActionHandler;
	private HttpForwardObjectCallbackActionHandler httpForwardObjectCallbackActionHandler;
	private HttpOverrideForwardedRequestActionHandler httpOverrideForwardedRequestCallbackActionHandler;
	private HttpErrorActionHandler httpErrorActionHandler;
	
	// forwarding
	private NettyHttpClient httpClient;
	private HopByHopHeaderFilter hopByHopHeaderFilter = new HopByHopHeaderFilter();
	
	public HttpActionHandler(
		final ServerConfiguration configuration,
		final EventLoopGroup eventLoopGroup,
		final HttpState httpStateHandler,
		final List<ProxyConfiguration> proxyConfigurations)
	{
		this.configuration = configuration;
		this.httpStateHandler = httpStateHandler;
		this.scheduler = httpStateHandler.getScheduler();
		this.httpClient = new NettyHttpClient(configuration, eventLoopGroup, proxyConfigurations, true);
	}
	
	public void processAction(
		final HttpRequest request,
		final ResponseWriter responseWriter,
		final ChannelHandlerContext ctx,
		final Set<String> localAddresses,
		final boolean proxyingRequest,
		final boolean synchronous)
	{
		if(request.getHeaders() == null
			|| !request.getHeaders().containsEntry(
			this.httpStateHandler.getUniqueLoopPreventionHeaderName(),
			this.httpStateHandler.getUniqueLoopPreventionHeaderValue()))
		{
			this.logEvent(new EventEntry()
				.setType(EventEntry.EventType.RECEIVED_REQUEST)
				.setCorrelationId(request.getLogCorrelationId())
				.setHttpRequest(request)
			);
			LOG.info(RECEIVED_REQUEST_MESSAGE_FORMAT, request);
		}
		final Expectation expectation = this.httpStateHandler.firstMatchingExpectation(request);
		final Runnable expectationPostProcessor = () -> this.httpStateHandler.postProcess(expectation);
		final boolean potentiallyHttpProxy =
			!proxyingRequest && this.configuration.attemptToProxyIfNoMatchingExpectation()
				&& !isEmpty(request.getFirstHeader(HOST.toString()))
				&& !localAddresses.contains(request.getFirstHeader(HOST.toString()));
		
		if(expectation != null && expectation.getAction() != null)
		{
			
			final Action action = expectation.getAction();
			switch(action.getType())
			{
				case RESPONSE:
				{
					this.scheduler.schedule(() -> this.handleAnyException(
						request,
						responseWriter,
						synchronous,
						action,
						() -> {
							final HttpResponse response =
								this.getHttpResponseActionHandler().handle((HttpResponse)action);
							this.writeResponseActionResponse(response, responseWriter, request, action, synchronous);
							expectationPostProcessor.run();
						}), synchronous);
					break;
				}
				case RESPONSE_CLASS_CALLBACK:
				{
					this.scheduler.schedule(() -> this.handleAnyException(
						request,
						responseWriter,
						synchronous,
						action,
						() -> {
							final HttpResponse response =
								this.getHttpResponseClassCallbackActionHandler()
									.handle((HttpClassCallback)action, request);
							this.writeResponseActionResponse(response, responseWriter, request, action, synchronous);
							expectationPostProcessor.run();
						}), synchronous, action.getDelay());
					break;
				}
				case RESPONSE_OBJECT_CALLBACK:
				{
					this.scheduler.schedule(() ->
							this.getHttpResponseObjectCallbackActionHandler().handle(
								HttpActionHandler.this,
								(HttpObjectCallback)action,
								request,
								responseWriter,
								synchronous,
								expectationPostProcessor),
						synchronous, action.getDelay());
					break;
				}
				case FORWARD:
				{
					this.scheduler.schedule(() -> this.handleAnyException(
						request,
						responseWriter,
						synchronous,
						action,
						() -> {
							final HttpForwardActionResult responseFuture =
								this.getHttpForwardActionHandler().handle((HttpForward)action, request);
							this.writeForwardActionResponse(
								responseFuture,
								responseWriter,
								request,
								action,
								synchronous);
							expectationPostProcessor.run();
						}), synchronous, action.getDelay());
					break;
				}
				case FORWARD_CLASS_CALLBACK:
				{
					this.scheduler.schedule(() -> this.handleAnyException(
						request,
						responseWriter,
						synchronous,
						action,
						() -> {
							final HttpForwardActionResult responseFuture =
								this.getHttpForwardClassCallbackActionHandler()
									.handle((HttpClassCallback)action, request);
							this.writeForwardActionResponse(
								responseFuture,
								responseWriter,
								request,
								action,
								synchronous);
							expectationPostProcessor.run();
						}), synchronous, action.getDelay());
					break;
				}
				case FORWARD_OBJECT_CALLBACK:
				{
					this.scheduler.schedule(() ->
							this.getHttpForwardObjectCallbackActionHandler().handle(
								HttpActionHandler.this,
								(HttpObjectCallback)action,
								request,
								responseWriter,
								synchronous,
								expectationPostProcessor),
						synchronous, action.getDelay());
					break;
				}
				case FORWARD_REPLACE:
				{
					this.scheduler.schedule(() -> this.handleAnyException(
						request,
						responseWriter,
						synchronous,
						action,
						() -> {
							final HttpForwardActionResult responseFuture =
								this.getHttpOverrideForwardedRequestCallbackActionHandler().handle(
									(HttpOverrideForwardedRequest)action,
									request);
							this.writeForwardActionResponse(
								responseFuture,
								responseWriter,
								request,
								action,
								synchronous);
							expectationPostProcessor.run();
						}), synchronous, action.getDelay());
					break;
				}
				case ERROR:
				{
					this.scheduler.schedule(() -> this.handleAnyException(
						request,
						responseWriter,
						synchronous,
						action,
						() -> {
							this.getHttpErrorActionHandler().handle((HttpError)action, ctx);
							this.logEvent(new EventEntry()
								.setType(EventEntry.EventType.EXPECTATION_RESPONSE)
								.setCorrelationId(request.getLogCorrelationId())
								.setHttpRequest(request)
								.setHttpError((HttpError)action)
								.setExpectationId(action.getExpectationId()));
							LOG.info(
								"Returning error: {} for request: {} for action: {} from expectation: {}",
								action,
								request,
								action,
								action.getExpectationId());
							expectationPostProcessor.run();
						}), synchronous, action.getDelay());
					break;
				}
			}
		}
		else if(CORSHeaders.isPreflightRequest(this.configuration, request) && (this.configuration.enableCORSForAPI()
			|| this.configuration.enableCORSForAllResponses()))
		{
			
			responseWriter.writeResponse(request, OK);
			if(LOG.isInfoEnabled())
			{
				LOG.info("Returning CORS response for OPTIONS request");
			}
		}
		else if(proxyingRequest || potentiallyHttpProxy)
		{
			
			if(request.getHeaders() != null && request.getHeaders()
				.containsEntry(
					this.httpStateHandler.getUniqueLoopPreventionHeaderName(),
					this.httpStateHandler.getUniqueLoopPreventionHeaderValue()))
			{
				
				if(LOG.isTraceEnabled())
				{
					LOG.trace(
						"Received \"x-forwarded-by\" header caused by exploratory HTTP proxy or proxy loop "
							+ "- falling back to no proxy: {}",
						request);
				}
				this.returnNotFound(responseWriter, request, null);
			}
			else
			{
				
				final String username = this.configuration.proxyAuthenticationUsername();
				final String password = this.configuration.proxyAuthenticationPassword();
				// only authenticate potentiallyHttpProxy because other proxied requests should have already been
				// authenticated (i.e. in CONNECT request)
				if(potentiallyHttpProxy && isNotBlank(username) && isNotBlank(password) &&
					!request.containsHeader(
						PROXY_AUTHORIZATION.toString(),
						"Basic " + Base64.encode(Unpooled.copiedBuffer(
							username + ':' + password,
							StandardCharsets.UTF_8), false).toString(StandardCharsets.US_ASCII)))
				{
					
					final HttpResponse response = response()
						.withStatusCode(PROXY_AUTHENTICATION_REQUIRED.code())
						.withHeader(
							PROXY_AUTHENTICATE.toString(),
							"Basic realm=\""
								+ StringEscapeUtils.escapeJava(this.configuration.proxyAuthenticationRealm())
								+ "\", charset=\"UTF-8\"");
					responseWriter.writeResponse(request, response, false);
					LOG.info(
						"Proxy authentication failed so returning response: {} for forwarded request: {}",
						response,
						request);
				}
				else
				{
					
					final InetSocketAddress remoteAddress = getRemoteAddress(ctx);
					final HttpRequest clonedRequest = this.hopByHopHeaderFilter.onRequest(request)
						.withHeader(
							this.httpStateHandler.getUniqueLoopPreventionHeaderName(),
							this.httpStateHandler.getUniqueLoopPreventionHeaderValue());
					final HttpForwardActionResult responseFuture = new HttpForwardActionResult(
						clonedRequest,
						this.httpClient.sendRequest(
							clonedRequest,
							remoteAddress,
							potentiallyHttpProxy ? 1000 : this.configuration.socketConnectionTimeoutInMillis()),
						null,
						remoteAddress);
					this.scheduler.submit(responseFuture, () -> {
							try
							{
								HttpResponse response = responseFuture.getHttpResponse()
									.get(this.configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
								if(response == null)
								{
									response = notFoundResponse();
								}
								if(response.containsHeader(
									this.httpStateHandler.getUniqueLoopPreventionHeaderName(),
									this.httpStateHandler.getUniqueLoopPreventionHeaderValue()))
								{
									response.removeHeader(this.httpStateHandler.getUniqueLoopPreventionHeaderName());
									LOG.info(NO_MATCH_RESPONSE_NO_EXPECTATION_MESSAGE_FORMAT, request, response);
									this.logEvent(new EventEntry()
										.setType(EventEntry.EventType.NO_MATCH_RESPONSE)
										.setCorrelationId(request.getLogCorrelationId())
										.setHttpRequest(request)
										.setHttpResponse(notFoundResponse()));
								}
								else
								{
									LOG.info(
										"Returning response: {} for forwarded request in json:{}",
										request,
										response);
									this.logEvent(new EventEntry()
										.setType(EventEntry.EventType.FORWARDED_REQUEST)
										.setCorrelationId(request.getLogCorrelationId())
										.setHttpRequest(request)
										.setHttpResponse(response)
										.setExpectation(request, response));
								}
								responseWriter.writeResponse(request, response, false);
							}
							catch(final SocketCommunicationException sce)
							{
								this.returnNotFound(responseWriter, request, sce.getMessage());
							}
							catch(final Exception ex)
							{
								if(potentiallyHttpProxy && connectionException(ex))
								{
									if(LOG.isTraceEnabled())
									{
										LOG.trace(
											"Failed to connect to proxied socket due to exploratory HTTP proxy "
												+ "for: {} due to (see below); Falling back to no proxy",
											request,
											ex.getCause());
									}
									this.returnNotFound(responseWriter, request, null);
								}
								else if(sslHandshakeException(ex))
								{
									LOG.error("TLS handshake exception while proxying request {} to "
											+ "remote address {} with channel {}",
										request,
										remoteAddress,
										ctx != null ? String.valueOf(ctx.channel()) : "", ex);
									this.returnNotFound(
										responseWriter,
										request,
										"TLS handshake exception while proxying request to remote address" + remoteAddress);
								}
								else if(!connectionClosedException(ex))
								{
									LOG.error("", ex);
									this.returnNotFound(
										responseWriter,
										request,
										"connection closed while proxying request to remote address" + remoteAddress);
								}
								else
								{
									this.returnNotFound(responseWriter, request, ex.getMessage());
								}
							}
						},
						synchronous,
						throwable -> !(potentiallyHttpProxy && isNotBlank(throwable.getMessage())
							|| !throwable.getMessage().contains("Connection refused"))
					);
				}
			}
		}
		else
		{
			
			this.returnNotFound(responseWriter, request, null);
		}
	}
	
	private void handleAnyException(
		final HttpRequest request,
		final ResponseWriter responseWriter,
		final boolean synchronous,
		final Action action,
		final Runnable processAction)
	{
		try
		{
			processAction.run();
		}
		catch(final Exception ex)
		{
			this.writeResponseActionResponse(notFoundResponse(), responseWriter, request, action, synchronous);
			if(LOG.isInfoEnabled())
			{
				LOG.warn("", ex);
			}
		}
	}
	
	void writeResponseActionResponse(
		final HttpResponse response,
		final ResponseWriter responseWriter,
		final HttpRequest request,
		final Action action,
		final boolean synchronous)
	{
		this.scheduler.schedule(() -> {
			this.logEvent(new EventEntry()
				.setType(EventEntry.EventType.EXPECTATION_RESPONSE)
				.setCorrelationId(request.getLogCorrelationId())
				.setHttpRequest(request)
				.setHttpResponse(response)
				.setExpectationId(action.getExpectationId())
			);
			LOG.info("Returning response: {} for request: {} for action: {} from expectation: {}",
				response, request, action, action.getExpectationId());
			responseWriter.writeResponse(request, response, false);
		}, synchronous, response.getDelay());
	}
	
	void executeAfterForwardActionResponse(
		final HttpForwardActionResult responseFuture,
		final BiConsumer<HttpResponse, Throwable> command,
		final boolean synchronous)
	{
		this.scheduler.submit(responseFuture, command, synchronous);
	}
	
	void writeForwardActionResponse(
		final HttpForwardActionResult responseFuture,
		final ResponseWriter responseWriter,
		final HttpRequest request,
		final Action action,
		final boolean synchronous)
	{
		this.scheduler.submit(responseFuture, () -> {
			try
			{
				final HttpResponse response =
					responseFuture.getHttpResponse().get(this.configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
				responseWriter.writeResponse(request, response, false);
				
				this.logEvent(new EventEntry()
					.setType(EventEntry.EventType.FORWARDED_REQUEST)
					.setCorrelationId(request.getLogCorrelationId())
					.setHttpRequest(request)
					.setHttpResponse(response)
					.setExpectation(request, response)
					.setExpectationId(action.getExpectationId()));
				
				LOG.info(
					"Returning response: {} for forwarded request {} for action: {} from expectation: {}",
					response,
					responseFuture.getHttpRequest(),
					action,
					action.getExpectationId());
			}
			catch(final Exception ex)
			{
				this.handleExceptionDuringForwardingRequest(action, request, responseWriter, ex);
			}
		}, synchronous, throwable -> true);
	}
	
	void writeForwardActionResponse(
		final HttpResponse response,
		final ResponseWriter responseWriter,
		final HttpRequest request,
		final Action action)
	{
		try
		{
			responseWriter.writeResponse(request, response, false);
			
			this.logEvent(new EventEntry()
				.setType(EventEntry.EventType.FORWARDED_REQUEST)
				.setCorrelationId(request.getLogCorrelationId())
				.setHttpRequest(request)
				.setHttpResponse(response)
				.setExpectation(request, response)
				.setExpectationId(action.getExpectationId()));
			
			LOG.info(
				"Returning response: {} for forwarded request in json: {} for action: {} from expectation: {}",
				response,
				response,
				action,
				action.getExpectationId());
		}
		catch(final Exception ex)
		{
			LOG.error("", ex);
		}
	}
	
	void handleExceptionDuringForwardingRequest(
		final Action action,
		final HttpRequest request,
		final ResponseWriter responseWriter,
		final Throwable exception)
	{
		if(connectionException(exception))
		{
			if(LOG.isTraceEnabled())
			{
				LOG.trace(
					"Failed to connect to remote socket while forwarding request {}for action {}",
					request,
					action);
			}
			this.returnNotFound(responseWriter, request, "failed to connect to remote socket while forwarding "
				+ "request");
		}
		else if(sslHandshakeException(exception))
		{
			LOG.error(
				"TLS handshake exception while forwarding request {} for action {}",
				request,
				action);
			this.returnNotFound(responseWriter, request, "TLS handshake exception while forwarding request");
		}
		else
		{
			LOG.error("Failed during request forwarding", exception);
			this.returnNotFound(responseWriter, request, exception != null ? exception.getMessage() : null);
		}
	}
	
	private void returnNotFound(final ResponseWriter responseWriter, final HttpRequest request, final String error)
	{
		final HttpResponse response = notFoundResponse();
		if(request.getHeaders() != null && request.getHeaders()
			.containsEntry(
				this.httpStateHandler.getUniqueLoopPreventionHeaderName(),
				this.httpStateHandler.getUniqueLoopPreventionHeaderValue()))
		{
			response.withHeader(
				this.httpStateHandler.getUniqueLoopPreventionHeaderName(),
				this.httpStateHandler.getUniqueLoopPreventionHeaderValue());
			if(LOG.isTraceEnabled())
			{
				LOG.trace(NO_MATCH_RESPONSE_NO_EXPECTATION_MESSAGE_FORMAT, request, notFoundResponse());
			}
		}
		else
		{
			this.logEvent(new EventEntry()
				.setType(EventEntry.EventType.NO_MATCH_RESPONSE)
				.setCorrelationId(request.getLogCorrelationId())
				.setHttpRequest(request)
				.setHttpResponse(notFoundResponse()));
			if(LOG.isInfoEnabled())
			{
				if(isNotBlank(error))
				{
					LOG.info(NO_MATCH_RESPONSE_ERROR_MESSAGE_FORMAT, error, request, notFoundResponse());
				}
				else
				{
					LOG.info(NO_MATCH_RESPONSE_NO_EXPECTATION_MESSAGE_FORMAT, request, notFoundResponse());
				}
			}
		}
		
		responseWriter.writeResponse(request, response, false);
	}
	
	private void logEvent(final EventEntry entry)
	{
		this.httpStateHandler.logEvent(entry);
	}
	
	private HttpResponseActionHandler getHttpResponseActionHandler()
	{
		if(this.httpResponseActionHandler == null)
		{
			this.httpResponseActionHandler = new HttpResponseActionHandler();
		}
		return this.httpResponseActionHandler;
	}
	
	private HttpResponseClassCallbackActionHandler getHttpResponseClassCallbackActionHandler()
	{
		if(this.httpResponseClassCallbackActionHandler == null)
		{
			this.httpResponseClassCallbackActionHandler = new HttpResponseClassCallbackActionHandler();
		}
		return this.httpResponseClassCallbackActionHandler;
	}
	
	private HttpResponseObjectCallbackActionHandler getHttpResponseObjectCallbackActionHandler()
	{
		if(this.httpResponseObjectCallbackActionHandler == null)
		{
			this.httpResponseObjectCallbackActionHandler =
				new HttpResponseObjectCallbackActionHandler(this.httpStateHandler);
		}
		return this.httpResponseObjectCallbackActionHandler;
	}
	
	private HttpForwardActionHandler getHttpForwardActionHandler()
	{
		if(this.httpForwardActionHandler == null)
		{
			this.httpForwardActionHandler = new HttpForwardActionHandler(this.httpClient);
		}
		return this.httpForwardActionHandler;
	}
	
	private HttpForwardClassCallbackActionHandler getHttpForwardClassCallbackActionHandler()
	{
		if(this.httpForwardClassCallbackActionHandler == null)
		{
			this.httpForwardClassCallbackActionHandler = new HttpForwardClassCallbackActionHandler(this.httpClient);
		}
		return this.httpForwardClassCallbackActionHandler;
	}
	
	private HttpForwardObjectCallbackActionHandler getHttpForwardObjectCallbackActionHandler()
	{
		if(this.httpForwardObjectCallbackActionHandler == null)
		{
			this.httpForwardObjectCallbackActionHandler =
				new HttpForwardObjectCallbackActionHandler(this.httpStateHandler, this.httpClient);
		}
		return this.httpForwardObjectCallbackActionHandler;
	}
	
	private HttpOverrideForwardedRequestActionHandler getHttpOverrideForwardedRequestCallbackActionHandler()
	{
		if(this.httpOverrideForwardedRequestCallbackActionHandler == null)
		{
			this.httpOverrideForwardedRequestCallbackActionHandler =
				new HttpOverrideForwardedRequestActionHandler(this.httpClient);
		}
		return this.httpOverrideForwardedRequestCallbackActionHandler;
	}
	
	private HttpErrorActionHandler getHttpErrorActionHandler()
	{
		if(this.httpErrorActionHandler == null)
		{
			this.httpErrorActionHandler = new HttpErrorActionHandler();
		}
		return this.httpErrorActionHandler;
	}
	
	public NettyHttpClient getHttpClient()
	{
		return this.httpClient;
	}
	
	public static InetSocketAddress getRemoteAddress(final ChannelHandlerContext ctx)
	{
		if(ctx != null && ctx.channel() != null && ctx.channel().attr(REMOTE_SOCKET) != null)
		{
			return ctx.channel().attr(REMOTE_SOCKET).get();
		}
		else
		{
			return null;
		}
	}
	
	public static void setRemoteAddress(final ChannelHandlerContext ctx, final InetSocketAddress inetSocketAddress)
	{
		if(ctx != null && ctx.channel() != null)
		{
			ctx.channel().attr(REMOTE_SOCKET).set(inetSocketAddress);
		}
	}
}
