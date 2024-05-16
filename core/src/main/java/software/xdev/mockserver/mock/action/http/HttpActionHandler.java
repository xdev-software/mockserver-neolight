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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.AttributeKey;
import org.apache.commons.text.StringEscapeUtils;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.cors.CORSHeaders;
import software.xdev.mockserver.filters.HopByHopHeaderFilter;
import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.httpclient.SocketCommunicationException;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.proxyconfiguration.ProxyConfiguration;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.scheduler.Scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.util.StringUtils.isEmpty;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.exception.ExceptionHandling.*;
import static software.xdev.mockserver.log.model.LogEntryMessages.*;
import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;
import static software.xdev.mockserver.model.HttpResponse.response;

@SuppressWarnings({"rawtypes", "FieldMayBeFinal"})
public class HttpActionHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(HttpActionHandler.class);
    
    public static final AttributeKey<InetSocketAddress> REMOTE_SOCKET = AttributeKey.valueOf("REMOTE_SOCKET");
    
    private final Configuration configuration;
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

    public HttpActionHandler(Configuration configuration, EventLoopGroup eventLoopGroup, HttpState httpStateHandler, List<ProxyConfiguration> proxyConfigurations) {
        this.configuration = configuration;
        this.httpStateHandler = httpStateHandler;
        this.scheduler = httpStateHandler.getScheduler();
        this.httpClient = new NettyHttpClient(configuration, eventLoopGroup, proxyConfigurations, true);
    }

    public void processAction(final HttpRequest request, final ResponseWriter responseWriter, final ChannelHandlerContext ctx, Set<String> localAddresses, boolean proxyingRequest, final boolean synchronous) {
        if (request.getHeaders() == null
            || !request.getHeaders().containsEntry(
                httpStateHandler.getUniqueLoopPreventionHeaderName(),
                httpStateHandler.getUniqueLoopPreventionHeaderValue())) {
            LOG.info(RECEIVED_REQUEST_MESSAGE_FORMAT, request);
        }
        final Expectation expectation = httpStateHandler.firstMatchingExpectation(request);
        Runnable expectationPostProcessor = () -> httpStateHandler.postProcess(expectation);
        final boolean potentiallyHttpProxy = !proxyingRequest && configuration.attemptToProxyIfNoMatchingExpectation() && !isEmpty(request.getFirstHeader(HOST.toString())) && !localAddresses.contains(request.getFirstHeader(HOST.toString()));

        if (expectation != null && expectation.getAction() != null) {

            final Action action = expectation.getAction();
            switch (action.getType()) {
                case RESPONSE: {
                    scheduler.schedule(() -> handleAnyException(request, responseWriter, synchronous, action, () -> {
                        final HttpResponse response = getHttpResponseActionHandler().handle((HttpResponse) action);
                        writeResponseActionResponse(response, responseWriter, request, action, synchronous);
                        expectationPostProcessor.run();
                    }), synchronous);
                    break;
                }
                case RESPONSE_CLASS_CALLBACK: {
                    scheduler.schedule(() -> handleAnyException(request, responseWriter, synchronous, action, () -> {
                        final HttpResponse response = getHttpResponseClassCallbackActionHandler().handle((HttpClassCallback) action, request);
                        writeResponseActionResponse(response, responseWriter, request, action, synchronous);
                        expectationPostProcessor.run();
                    }), synchronous, action.getDelay());
                    break;
                }
                case RESPONSE_OBJECT_CALLBACK: {
                    scheduler.schedule(() ->
                            getHttpResponseObjectCallbackActionHandler().handle(HttpActionHandler.this, (HttpObjectCallback) action, request, responseWriter, synchronous, expectationPostProcessor),
                        synchronous, action.getDelay());
                    break;
                }
                case FORWARD: {
                    scheduler.schedule(() -> handleAnyException(request, responseWriter, synchronous, action, () -> {
                        final HttpForwardActionResult responseFuture = getHttpForwardActionHandler().handle((HttpForward) action, request);
                        writeForwardActionResponse(responseFuture, responseWriter, request, action, synchronous);
                        expectationPostProcessor.run();
                    }), synchronous, action.getDelay());
                    break;
                }
                case FORWARD_CLASS_CALLBACK: {
                    scheduler.schedule(() -> handleAnyException(request, responseWriter, synchronous, action, () -> {
                        final HttpForwardActionResult responseFuture = getHttpForwardClassCallbackActionHandler().handle((HttpClassCallback) action, request);
                        writeForwardActionResponse(responseFuture, responseWriter, request, action, synchronous);
                        expectationPostProcessor.run();
                    }), synchronous, action.getDelay());
                    break;
                }
                case FORWARD_OBJECT_CALLBACK: {
                    scheduler.schedule(() ->
                            getHttpForwardObjectCallbackActionHandler().handle(HttpActionHandler.this, (HttpObjectCallback) action, request, responseWriter, synchronous, expectationPostProcessor),
                        synchronous, action.getDelay());
                    break;
                }
                case FORWARD_REPLACE: {
                    scheduler.schedule(() -> handleAnyException(request, responseWriter, synchronous, action, () -> {
                        final HttpForwardActionResult responseFuture = getHttpOverrideForwardedRequestCallbackActionHandler().handle((HttpOverrideForwardedRequest) action, request);
                        writeForwardActionResponse(responseFuture, responseWriter, request, action, synchronous);
                        expectationPostProcessor.run();
                    }), synchronous, action.getDelay());
                    break;
                }
                case ERROR: {
                    scheduler.schedule(() -> handleAnyException(request, responseWriter, synchronous, action, () -> {
                        getHttpErrorActionHandler().handle((HttpError) action, ctx);
                        LOG.info("Returning error: {} for request: {} for action: {} from expectation: {}",
                            action,
                            request,
                            action,
                            action.getExpectationId());
                        expectationPostProcessor.run();
                    }), synchronous, action.getDelay());
                    break;
                }
            }

        } else if (CORSHeaders.isPreflightRequest(configuration, request) && (configuration.enableCORSForAPI() || configuration.enableCORSForAllResponses())) {

            responseWriter.writeResponse(request, OK);
            if (LOG.isInfoEnabled()) {
                LOG.info("Returning CORS response for OPTIONS request");
            }

        } else if (proxyingRequest || potentiallyHttpProxy) {

            if (request.getHeaders() != null && request.getHeaders().containsEntry(httpStateHandler.getUniqueLoopPreventionHeaderName(), httpStateHandler.getUniqueLoopPreventionHeaderValue())) {

                if (LOG.isTraceEnabled()) {
                    LOG.trace("Received \"x-forwarded-by\" header caused by exploratory HTTP proxy or proxy loop "
                        + "- falling back to no proxy: {}",
                        request);
                }
                returnNotFound(responseWriter, request, null);

            } else {

                String username = configuration.proxyAuthenticationUsername();
                String password = configuration.proxyAuthenticationPassword();
                // only authenticate potentiallyHttpProxy because other proxied requests should have already been authenticated (i.e. in CONNECT request)
                if (potentiallyHttpProxy && isNotBlank(username) && isNotBlank(password) &&
                    !request.containsHeader(PROXY_AUTHORIZATION.toString(), "Basic " + Base64.encode(Unpooled.copiedBuffer(username + ':' + password, StandardCharsets.UTF_8), false).toString(StandardCharsets.US_ASCII))) {

                    HttpResponse response = response()
                        .withStatusCode(PROXY_AUTHENTICATION_REQUIRED.code())
                        .withHeader(PROXY_AUTHENTICATE.toString(), "Basic realm=\"" + StringEscapeUtils.escapeJava(configuration.proxyAuthenticationRealm()) + "\", charset=\"UTF-8\"");
                    responseWriter.writeResponse(request, response, false);
                    LOG.info("Proxy authentication failed so returning response: {} for forwarded request: {}", response, request);

                } else {

                    final InetSocketAddress remoteAddress = getRemoteAddress(ctx);
                    final HttpRequest clonedRequest = hopByHopHeaderFilter.onRequest(request).withHeader(httpStateHandler.getUniqueLoopPreventionHeaderName(), httpStateHandler.getUniqueLoopPreventionHeaderValue());
                    final HttpForwardActionResult responseFuture = new HttpForwardActionResult(clonedRequest, httpClient.sendRequest(clonedRequest, remoteAddress, potentiallyHttpProxy ? 1000 : configuration.socketConnectionTimeoutInMillis()), null, remoteAddress);
                    scheduler.submit(responseFuture, () -> {
                            try {
                                HttpResponse response = responseFuture.getHttpResponse().get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
                                if (response == null) {
                                    response = notFoundResponse();
                                }
                                if (response.containsHeader(httpStateHandler.getUniqueLoopPreventionHeaderName(), httpStateHandler.getUniqueLoopPreventionHeaderValue())) {
                                    response.removeHeader(httpStateHandler.getUniqueLoopPreventionHeaderName());
                                    LOG.info(NO_MATCH_RESPONSE_NO_EXPECTATION_MESSAGE_FORMAT, request, response);
                                } else {
                                    LOG.info("Returning response: {} for forwarded request in json:{}",
                                        request,
                                        response);
                                }
                                responseWriter.writeResponse(request, response, false);
                            } catch (SocketCommunicationException sce) {
                                returnNotFound(responseWriter, request, sce.getMessage());
                            } catch (Exception ex) {
                                if (potentiallyHttpProxy && connectionException(ex)) {
                                    if (LOG.isTraceEnabled()) {
                                        LOG.trace("Failed to connect to proxied socket due to exploratory HTTP proxy "
                                            + "for: {} due to (see below); Falling back to no proxy",
                                            request,
                                            ex.getCause());
                                    }
                                    returnNotFound(responseWriter, request, null);
                                } else if (sslHandshakeException(ex)) {
                                    LOG.error("TLS handshake exception while proxying request {} to "
                                        + "remote address {} with channel {}",
                                        request,
                                        remoteAddress,
                                        ctx != null ? String.valueOf(ctx.channel()) : "", ex);
                                    returnNotFound(responseWriter, request, "TLS handshake exception while proxying request to remote address" + remoteAddress);
                                } else if (!connectionClosedException(ex)) {
                                    LOG.error("", ex);
                                    returnNotFound(responseWriter, request, "connection closed while proxying request to remote address" + remoteAddress);
                                } else {
                                    returnNotFound(responseWriter, request, ex.getMessage());
                                }
                            }
                        },
                        synchronous,
                        throwable -> !(potentiallyHttpProxy && isNotBlank(throwable.getMessage()) || !throwable.getMessage().contains("Connection refused"))
                    );

                }

            }

        } else {

            returnNotFound(responseWriter, request, null);

        }
    }

    private void handleAnyException(HttpRequest request, ResponseWriter responseWriter, boolean synchronous, Action action, Runnable processAction) {
        try {
            processAction.run();
        } catch (Exception ex) {
            writeResponseActionResponse(notFoundResponse(), responseWriter, request, action, synchronous);
            if (LOG.isInfoEnabled()) {
                LOG.warn("", ex);
            }
        }
    }

    void writeResponseActionResponse(final HttpResponse response, final ResponseWriter responseWriter, final HttpRequest request, final Action action, boolean synchronous) {
        scheduler.schedule(() -> {
            LOG.info("Returning response: {} for request: {} for action: {} from expectation: {}",
                response, request, action, action.getExpectationId());
            responseWriter.writeResponse(request, response, false);
        }, synchronous, response.getDelay());
    }

    void executeAfterForwardActionResponse(final HttpForwardActionResult responseFuture, final BiConsumer<HttpResponse, Throwable> command, final boolean synchronous) {
        scheduler.submit(responseFuture, command, synchronous);
    }

    void writeForwardActionResponse(final HttpForwardActionResult responseFuture, final ResponseWriter responseWriter, final HttpRequest request, final Action action, boolean synchronous) {
        scheduler.submit(responseFuture, () -> {
            try {
                HttpResponse response = responseFuture.getHttpResponse().get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
                responseWriter.writeResponse(request, response, false);
                LOG.info("Returning response: {} for forwarded request {} for action: {} from expectation: {}",
                    response,
                    responseFuture.getHttpRequest(),
                    action,
                    action.getExpectationId());
            } catch (Exception ex) {
                handleExceptionDuringForwardingRequest(action, request, responseWriter, ex);
            }
        }, synchronous, throwable -> true);
    }

    void writeForwardActionResponse(final HttpResponse response, final ResponseWriter responseWriter, final HttpRequest request, final Action action) {
        try {
            responseWriter.writeResponse(request, response, false);
            LOG.info("Returning response: {} for forwarded request in json:{} for action:{} from expectation:{}",
                response,
                response,
                action,
                action.getExpectationId());
        } catch (Exception ex) {
            LOG.error("", ex);
        }
    }

    void handleExceptionDuringForwardingRequest(Action action, HttpRequest request, ResponseWriter responseWriter, Throwable exception) {
        if (connectionException(exception)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Failed to connect to remote socket while forwarding request {}for action {}",
                    request,
                    action);
            }
            returnNotFound(responseWriter, request, "failed to connect to remote socket while forwarding request");
        } else if (sslHandshakeException(exception)) {
            LOG.error("TLS handshake exception while forwarding request {} for action {}",
                request,
                action);
            returnNotFound(responseWriter, request, "TLS handshake exception while forwarding request");
        } else {
            LOG.error("Failed during request forwarding", exception);
            returnNotFound(responseWriter, request, exception != null ? exception.getMessage() : null);
        }
    }

    private void returnNotFound(ResponseWriter responseWriter, HttpRequest request, String error) {
        HttpResponse response = notFoundResponse();
        if (request.getHeaders() != null && request.getHeaders().containsEntry(httpStateHandler.getUniqueLoopPreventionHeaderName(), httpStateHandler.getUniqueLoopPreventionHeaderValue())) {
            response.withHeader(httpStateHandler.getUniqueLoopPreventionHeaderName(), httpStateHandler.getUniqueLoopPreventionHeaderValue());
            if (LOG.isTraceEnabled()) {
                LOG.trace(NO_MATCH_RESPONSE_NO_EXPECTATION_MESSAGE_FORMAT, request, notFoundResponse());
            }
        } else if (isNotBlank(error)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(NO_MATCH_RESPONSE_ERROR_MESSAGE_FORMAT, error, request, notFoundResponse());
            }
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info(NO_MATCH_RESPONSE_NO_EXPECTATION_MESSAGE_FORMAT, request, notFoundResponse());
            }
        }
        responseWriter.writeResponse(request, response, false);
    }

    private HttpResponseActionHandler getHttpResponseActionHandler() {
        if (httpResponseActionHandler == null) {
            httpResponseActionHandler = new HttpResponseActionHandler();
        }
        return httpResponseActionHandler;
    }

    private HttpResponseClassCallbackActionHandler getHttpResponseClassCallbackActionHandler() {
        if (httpResponseClassCallbackActionHandler == null) {
            httpResponseClassCallbackActionHandler = new HttpResponseClassCallbackActionHandler();
        }
        return httpResponseClassCallbackActionHandler;
    }

    private HttpResponseObjectCallbackActionHandler getHttpResponseObjectCallbackActionHandler() {
        if (httpResponseObjectCallbackActionHandler == null) {
            httpResponseObjectCallbackActionHandler = new HttpResponseObjectCallbackActionHandler(httpStateHandler);
        }
        return httpResponseObjectCallbackActionHandler;
    }

    private HttpForwardActionHandler getHttpForwardActionHandler() {
        if (httpForwardActionHandler == null) {
            httpForwardActionHandler = new HttpForwardActionHandler(httpClient);
        }
        return httpForwardActionHandler;
    }

    private HttpForwardClassCallbackActionHandler getHttpForwardClassCallbackActionHandler() {
        if (httpForwardClassCallbackActionHandler == null) {
            httpForwardClassCallbackActionHandler = new HttpForwardClassCallbackActionHandler(httpClient);
        }
        return httpForwardClassCallbackActionHandler;
    }

    private HttpForwardObjectCallbackActionHandler getHttpForwardObjectCallbackActionHandler() {
        if (httpForwardObjectCallbackActionHandler == null) {
            httpForwardObjectCallbackActionHandler = new HttpForwardObjectCallbackActionHandler(httpStateHandler, httpClient);
        }
        return httpForwardObjectCallbackActionHandler;
    }

    private HttpOverrideForwardedRequestActionHandler getHttpOverrideForwardedRequestCallbackActionHandler() {
        if (httpOverrideForwardedRequestCallbackActionHandler == null) {
            httpOverrideForwardedRequestCallbackActionHandler = new HttpOverrideForwardedRequestActionHandler(httpClient);
        }
        return httpOverrideForwardedRequestCallbackActionHandler;
    }

    private HttpErrorActionHandler getHttpErrorActionHandler() {
        if (httpErrorActionHandler == null) {
            httpErrorActionHandler = new HttpErrorActionHandler();
        }
        return httpErrorActionHandler;
    }

    public NettyHttpClient getHttpClient() {
        return httpClient;
    }


    public static InetSocketAddress getRemoteAddress(final ChannelHandlerContext ctx) {
        if (ctx != null && ctx.channel() != null && ctx.channel().attr(REMOTE_SOCKET) != null) {
            return ctx.channel().attr(REMOTE_SOCKET).get();
        } else {
            return null;
        }
    }


    public static void setRemoteAddress(final ChannelHandlerContext ctx, final InetSocketAddress inetSocketAddress) {
        if (ctx != null && ctx.channel() != null) {
            ctx.channel().attr(REMOTE_SOCKET).set(inetSocketAddress);
        }
    }
}
