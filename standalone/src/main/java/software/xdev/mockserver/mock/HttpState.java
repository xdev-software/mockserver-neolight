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

import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.log.MockServerEventLog;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.mock.listeners.MockServerMatcherNotifier.Cause;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.scheduler.Scheduler;
import software.xdev.mockserver.serialization.*;
import software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer;
import software.xdev.mockserver.uuid.UUIDService;
import software.xdev.mockserver.verify.Verification;
import software.xdev.mockserver.verify.VerificationSequence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.util.StringUtils.*;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.log.model.LogEntryMessages.RECEIVED_REQUEST_MESSAGE_FORMAT;
import static software.xdev.mockserver.model.HttpRequest.request;
import static software.xdev.mockserver.model.HttpResponse.response;

public class HttpState {
    
    private static final Logger LOG = LoggerFactory.getLogger(HttpState.class);
    
    public static final String LOG_SEPARATOR = NEW_LINE + "------------------------------------" + NEW_LINE;
    public static final String PATH_PREFIX = "/mockserver";
    private static final ThreadLocal<Integer> LOCAL_PORT = new ThreadLocal<>();
    private final String uniqueLoopPreventionHeaderValue = "MockServer_" + UUIDService.getUUID();
    private final MockServerEventLog mockServerLog;
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

    public static void setPort(final HttpRequest request) {
        if (request != null && request.getSocketAddress() != null) {
            setPort(request.getSocketAddress().getPort());
            request.withSocketAddress(null);
        }
    }

    public static void setPort(final Integer port) {
        LOCAL_PORT.set(port);
    }

    public static void setPort(final Integer... port) {
        if (port != null && port.length > 0) {
            setPort(port[0]);
        }
    }

    public static void setPort(final List<Integer> port) {
        if (port != null && !port.isEmpty()) {
            setPort(port.get(0));
        }
    }

    public static Integer getPort() {
        return LOCAL_PORT.get();
    }

    public HttpState(ServerConfiguration configuration, Scheduler scheduler) {
        this.configuration = configuration;
        this.scheduler = scheduler;
        this.webSocketClientRegistry = new WebSocketClientRegistry(configuration);
        LocalCallbackRegistry.setMaxWebSocketExpectations(configuration.maxWebSocketExpectations());
        this.mockServerLog = new MockServerEventLog(configuration, scheduler, true);
        this.requestMatchers = new RequestMatchers(configuration, scheduler, webSocketClientRegistry);
        if(LOG.isTraceEnabled()) {
            LOG.trace("Log ring buffer created, with size {}", configuration.ringBufferSize());
        }
    }

    public void clear(HttpRequest request) {
        final String logCorrelationId = UUIDService.getUUID();
        RequestDefinition requestDefinition = null;
        ExpectationId expectationId = null;
        if (isNotBlank(request.getBodyAsString())) {
            String body = request.getBodyAsJsonOrXmlString();
            try {
                expectationId = getExpectationIdSerializer().deserialize(body);
            } catch (Throwable throwable) {
                // assume not expectationId
                requestDefinition = getRequestDefinitionSerializer().deserialize(body);
            }
            if (expectationId != null) {
                requestDefinition = resolveExpectationId(expectationId);
            }
        }
        if (requestDefinition != null) {
            requestDefinition.withLogCorrelationId(logCorrelationId);
        }
        try {
            ClearType type = ClearType.valueOf(defaultIfEmpty(request.getFirstQueryStringParameter("type").toUpperCase(), "ALL"));
            switch (type) {
                case LOG:
                    mockServerLog.clear(requestDefinition);
                    break;
                case EXPECTATIONS:
                    if (expectationId != null) {
                        requestMatchers.clear(expectationId, logCorrelationId);
                    } else {
                        requestMatchers.clear(requestDefinition);
                    }
                    break;
                case ALL:
                    mockServerLog.clear(requestDefinition);
                    if (expectationId != null) {
                        requestMatchers.clear(expectationId, logCorrelationId);
                    } else {
                        requestMatchers.clear(requestDefinition);
                    }
                    break;
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("type") + "\" is not a valid value for \"type\" parameter, only the following values are supported " + Arrays.stream(ClearType.values()).map(input -> input.name().toLowerCase()).collect(Collectors.toList()));
        }
    }

    private RequestDefinition resolveExpectationId(ExpectationId expectationId) {
        return requestMatchers
            .retrieveRequestDefinitions(Collections.singletonList(expectationId))
            .findFirst()
            .orElse(null);
    }

    private List<RequestDefinition> resolveExpectationIds(List<ExpectationId> expectationIds) {
        return requestMatchers
            .retrieveRequestDefinitions(expectationIds)
            .collect(Collectors.toList());
    }

    public void reset() {
        requestMatchers.reset();
        mockServerLog.reset();
        webSocketClientRegistry.reset();
        if (LOG.isInfoEnabled()) {
            LOG.info("Resetting all expectations and request logs");
        }
    }

    public List<Expectation> add(Expectation... expectations) {
        List<Expectation> upsertedExpectations = new ArrayList<>();
        for (Expectation expectation : expectations) {
            upsertedExpectations.add(requestMatchers.add(expectation, Cause.API));
        }
        return upsertedExpectations;
    }

    public Expectation firstMatchingExpectation(HttpRequest request) {
        if (requestMatchers.isEmpty()) {
            return null;
        } else {
            return requestMatchers.firstMatchingExpectation(request);
        }
    }

    public List<Expectation> allMatchingExpectation(HttpRequest request) {
        if (requestMatchers.isEmpty()) {
            return Collections.emptyList();
        } else {
            return requestMatchers.retrieveActiveExpectations(request);
        }
    }

    public void postProcess(Expectation expectation) {
        requestMatchers.postProcess(expectation);
    }

    public void log(LogEntry logEntry) {
        if (mockServerLog != null) {
            mockServerLog.add(logEntry);
        }
    }

    public HttpResponse retrieve(HttpRequest request) {
        final String logCorrelationId = UUIDService.getUUID();
        CompletableFuture<HttpResponse> httpResponseFuture = new CompletableFuture<>();
        HttpResponse response = response().withStatusCode(OK.code());
        if (request != null) {
            try {
                final RequestDefinition requestDefinition = isNotBlank(request.getBodyAsString()) ? getRequestDefinitionSerializer().deserialize(request.getBodyAsJsonOrXmlString()) : request();
                requestDefinition.withLogCorrelationId(logCorrelationId);
                Format format = Format.valueOf(defaultIfEmpty(request.getFirstQueryStringParameter("format").toUpperCase(), "JSON"));
                RetrieveType type = RetrieveType.valueOf(defaultIfEmpty(request.getFirstQueryStringParameter("type").toUpperCase(), "REQUESTS"));
                switch (type) {
                    case REQUESTS: {
                        if (LOG.isInfoEnabled()) {
                            LOG.info(
                                "Retrieved requests in {} that match: {}",
                                format.name().toLowerCase(),
                                requestDefinition);
                        }
                        switch (format) {
                            case JAVA:
                                mockServerLog
                                    .retrieveRequests(
                                        requestDefinition,
                                        requests -> {
                                            response.withBody(
                                                getRequestDefinitionSerializer().serialize(requests),
                                                MediaType.create("application", "java").withCharset(UTF_8)
                                            );
                                            httpResponseFuture.complete(response);
                                        }
                                    );
                                break;
                            case JSON:
                                mockServerLog
                                    .retrieveRequests(
                                        requestDefinition,
                                        requests -> {
                                            response.withBody(
                                                getRequestDefinitionSerializer().serialize(true, requests),
                                                MediaType.JSON_UTF_8
                                            );
                                            httpResponseFuture.complete(response);
                                        }
                                    );
                                break;
                        }
                        break;
                    }
                    case REQUEST_RESPONSES: {
                        if (LOG.isInfoEnabled()) {
                            LOG.info(
                                "Retrieved requests and responses in {} that match: {}",
                                format.name().toLowerCase(),
                                requestDefinition);
                        }
                        switch (format) {
                            case JAVA:
                                response.withBody("JAVA not supported for REQUEST_RESPONSES", MediaType.create("text", "plain").withCharset(UTF_8));
                                httpResponseFuture.complete(response);
                                break;
                            case JSON:
                                mockServerLog
                                    .retrieveRequestResponses(
                                        requestDefinition,
                                        httpRequestAndHttpResponses -> {
                                            response.withBody(
                                                getHttpRequestResponseSerializer().serialize(httpRequestAndHttpResponses),
                                                MediaType.JSON_UTF_8
                                            );
                                            httpResponseFuture.complete(response);
                                        }
                                    );
                                break;
                        }
                        break;
                    }
                    case RECORDED_EXPECTATIONS: {
                        if (LOG.isInfoEnabled()) {
                            LOG.info(
                                "Retrieved recorded expectations in {} that match: {}",
                                format.name().toLowerCase(),
                                requestDefinition);
                        }
                        switch (format) {
                            case JAVA:
                                mockServerLog
                                    .retrieveRecordedExpectations(
                                        requestDefinition,
                                        requests -> {
                                            response.withBody(
                                                getExpectationToJavaSerializer().serialize(requests),
                                                MediaType.create("application", "java").withCharset(UTF_8)
                                            );
                                            httpResponseFuture.complete(response);
                                        }
                                    );
                                break;
                            case JSON:
                                mockServerLog
                                    .retrieveRecordedExpectations(
                                        requestDefinition,
                                        requests -> {
                                            response.withBody(
                                                getExpectationSerializerThatSerializesBodyDefault().serialize(requests),
                                                MediaType.JSON_UTF_8
                                            );
                                            httpResponseFuture.complete(response);
                                        }
                                    );
                                break;
                        }
                        break;
                    }
                    case ACTIVE_EXPECTATIONS: {
                        List<Expectation> expectations = requestMatchers.retrieveActiveExpectations(requestDefinition);
                        switch (format) {
                            case JAVA:
                                response.withBody(getExpectationToJavaSerializer().serialize(expectations), MediaType.create("application", "java").withCharset(UTF_8));
                                break;
                            case JSON:
                                response.withBody(getExpectationSerializer().serialize(expectations), MediaType.JSON_UTF_8);
                                break;
                        }
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Retrieved {} active expectations in {} that match: {}",
                                expectations.size(),
                                format.name().toLowerCase(),
                                requestDefinition);
                        }
                        httpResponseFuture.complete(response);
                        break;
                    }
                }

                try {
                    return httpResponseFuture.get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
                } catch (ExecutionException | InterruptedException | TimeoutException ex) {
                    LOG.error("Exception handling request: {}", request, ex);
                    throw new IllegalStateException("Exception retrieving state for " + request, ex);
                }
            } catch (IllegalArgumentException iae) {
                LOG.error("Exception handling request: {}", request, iae);
                if (iae.getMessage().contains(RetrieveType.class.getSimpleName())) {
                    throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("type") + "\" is not a valid value for \"type\" parameter, only the following values are supported " + Arrays.stream(RetrieveType.values()).map(input -> input.name().toLowerCase()).collect(Collectors.toList()));
                }
                if (iae.getMessage().contains(Format.class.getSimpleName())) {
                    throw new IllegalArgumentException("\"" + request.getFirstQueryStringParameter("format") + "\" is not a valid value for \"format\" parameter, only the following values are supported " + Arrays.stream(Format.values()).map(input -> input.name().toLowerCase()).collect(Collectors.toList()));
                }
                throw iae;
            }
        } else {
            return response().withStatusCode(200);
        }
    }

    public Future<String> verify(Verification verification) {
        CompletableFuture<String> result = new CompletableFuture<>();
        verify(verification, result::complete);
        return result;
    }

    public void verify(Verification verification, Consumer<String> resultConsumer) {
        if (verification.getExpectationId() != null) {
            // check valid expectation id and populate for error message
            verification.withRequest(resolveExpectationId(verification.getExpectationId()));
        }
        mockServerLog.verify(verification, resultConsumer);
    }

    public Future<String> verify(VerificationSequence verification) {
        CompletableFuture<String> result = new CompletableFuture<>();
        verify(verification, result::complete);
        return result;
    }

    public void verify(VerificationSequence verificationSequence, Consumer<String> resultConsumer) {
        if (verificationSequence.getExpectationIds() != null && !verificationSequence.getExpectationIds().isEmpty()) {
            verificationSequence.withRequests(resolveExpectationIds(verificationSequence.getExpectationIds()));
        }
        mockServerLog.verify(verificationSequence, resultConsumer);
    }

    public boolean handle(HttpRequest request, ResponseWriter responseWriter, boolean warDeployment) {

        request.withLogCorrelationId(UUIDService.getUUID());
        setPort(request);

        if (LOG.isTraceEnabled()) {
            LOG.trace(RECEIVED_REQUEST_MESSAGE_FORMAT, request);
        }

        if (request.matches("PUT")) {

            CompletableFuture<Boolean> canHandle = new CompletableFuture<>();

            if (request.matchesPath(PATH_PREFIX + "/expectation", "/expectation")) {

                if (controlPlaneRequestAuthenticated(request, responseWriter)) {
                    List<Expectation> upsertedExpectations = new ArrayList<>();
                    for (Expectation expectation : getExpectationSerializer().deserializeArray(request.getBodyAsJsonOrXmlString(), false)) {
                        if (!warDeployment || validateSupportedFeatures(expectation, request, responseWriter)) {
                            upsertedExpectations.addAll(add(expectation));
                        }
                    }

                    responseWriter.writeResponse(request, response()
                        .withStatusCode(CREATED.code())
                        .withBody(getExpectationSerializer().serialize(upsertedExpectations), MediaType.JSON_UTF_8), true);
                }
                canHandle.complete(true);
            } else if (request.matchesPath(PATH_PREFIX + "/clear", "/clear")) {

                if (controlPlaneRequestAuthenticated(request, responseWriter)) {
                    clear(request);
                    responseWriter.writeResponse(request, OK);
                }
                canHandle.complete(true);

            } else if (request.matchesPath(PATH_PREFIX + "/reset", "/reset")) {

                if (controlPlaneRequestAuthenticated(request, responseWriter)) {
                    reset();
                    responseWriter.writeResponse(request, OK);
                }
                canHandle.complete(true);

            } else if (request.matchesPath(PATH_PREFIX + "/retrieve", "/retrieve")) {

                if (controlPlaneRequestAuthenticated(request, responseWriter)) {
                    responseWriter.writeResponse(request, retrieve(request), true);
                }
                canHandle.complete(true);

            } else if (request.matchesPath(PATH_PREFIX + "/verify", "/verify")) {

                if (controlPlaneRequestAuthenticated(request, responseWriter)) {
                    verify(getVerificationSerializer().deserialize(request.getBodyAsJsonOrXmlString()), result -> {
                        if (isEmpty(result)) {
                            responseWriter.writeResponse(request, ACCEPTED);
                        } else {
                            responseWriter.writeResponse(request, NOT_ACCEPTABLE, result, MediaType.create("text", "plain").toString());
                        }
                        canHandle.complete(true);
                    });
                } else {
                    canHandle.complete(true);
                }

            } else if (request.matchesPath(PATH_PREFIX + "/verifySequence", "/verifySequence")) {

                if (controlPlaneRequestAuthenticated(request, responseWriter)) {
                    verify(getVerificationSequenceSerializer().deserialize(request.getBodyAsJsonOrXmlString()), result -> {
                        if (isEmpty(result)) {
                            responseWriter.writeResponse(request, ACCEPTED);
                        } else {
                            responseWriter.writeResponse(request, NOT_ACCEPTABLE, result, MediaType.create("text", "plain").toString());
                        }
                        canHandle.complete(true);
                    });
                } else {
                    canHandle.complete(true);
                }

            } else {
                canHandle.complete(false);
            }

            try {
                return canHandle.get(configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                LOG.error("Exception handling request: {}", request, ex);
                return false;
            }

        } else {

            return false;

        }

    }

    private boolean controlPlaneRequestAuthenticated(HttpRequest request, ResponseWriter responseWriter) {
        return true;
    }

    @SuppressWarnings("rawtypes")
    private boolean validateSupportedFeatures(Expectation expectation, HttpRequest request, ResponseWriter responseWriter) {
        boolean valid = true;
        Action action = expectation.getAction();
        String NOT_SUPPORTED_MESSAGE = " is not supported by MockServer deployed as a WAR due to limitations in the JEE specification; use mockserver-netty to enable these features";
        if (action instanceof HttpResponse && ((HttpResponse) action).getConnectionOptions() != null) {
            valid = false;
            responseWriter.writeResponse(request, response("ConnectionOptions" + NOT_SUPPORTED_MESSAGE), true);
        } else if (action instanceof HttpObjectCallback) {
            valid = false;
            responseWriter.writeResponse(request, response("HttpObjectCallback" + NOT_SUPPORTED_MESSAGE), true);
        } else if (action instanceof HttpError) {
            valid = false;
            responseWriter.writeResponse(request, response("HttpError" + NOT_SUPPORTED_MESSAGE), true);
        }
        return valid;
    }

    public WebSocketClientRegistry getWebSocketClientRegistry() {
        return webSocketClientRegistry;
    }

    public RequestMatchers getRequestMatchers() {
        return requestMatchers;
    }

    public MockServerEventLog getMockServerLog() {
        return mockServerLog;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public String getUniqueLoopPreventionHeaderName() {
        return "x-forwarded-by";
    }

    public String getUniqueLoopPreventionHeaderValue() {
        return uniqueLoopPreventionHeaderValue;
    }

    public void stop() {
        getMockServerLog().stop();
    }

    private ExpectationIdSerializer getExpectationIdSerializer() {
        if (this.expectationIdSerializer == null) {
            this.expectationIdSerializer = new ExpectationIdSerializer();
        }
        return expectationIdSerializer;
    }

    private RequestDefinitionSerializer getRequestDefinitionSerializer() {
        if (this.requestDefinitionSerializer == null) {
            this.requestDefinitionSerializer = new RequestDefinitionSerializer();
        }
        return requestDefinitionSerializer;
    }

    private LogEventRequestAndResponseSerializer getHttpRequestResponseSerializer() {
        if (this.httpRequestResponseSerializer == null) {
            this.httpRequestResponseSerializer = new LogEventRequestAndResponseSerializer();
        }
        return httpRequestResponseSerializer;
    }

    private ExpectationSerializer getExpectationSerializer() {
        if (this.expectationSerializer == null) {
            this.expectationSerializer = new ExpectationSerializer();
        }
        return expectationSerializer;
    }

    private ExpectationSerializer getExpectationSerializerThatSerializesBodyDefault() {
        if (this.expectationSerializerThatSerializesBodyDefault == null) {
            this.expectationSerializerThatSerializesBodyDefault = new ExpectationSerializer(true);
        }
        return expectationSerializerThatSerializesBodyDefault;
    }

    private ExpectationToJavaSerializer getExpectationToJavaSerializer() {
        if (this.expectationToJavaSerializer == null) {
            this.expectationToJavaSerializer = new ExpectationToJavaSerializer();
        }
        return expectationToJavaSerializer;
    }

    private VerificationSerializer getVerificationSerializer() {
        if (this.verificationSerializer == null) {
            this.verificationSerializer = new VerificationSerializer();
        }
        return verificationSerializer;
    }

    private VerificationSequenceSerializer getVerificationSequenceSerializer() {
        if (this.verificationSequenceSerializer == null) {
            this.verificationSequenceSerializer = new VerificationSequenceSerializer();
        }
        return verificationSequenceSerializer;
    }
}
