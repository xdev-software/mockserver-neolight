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
package software.xdev.mockserver.matchers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import software.xdev.mockserver.codec.ExpandedParameterDecoder;
import software.xdev.mockserver.codec.PathParametersDecoder;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.*;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.serialization.deserializers.body.StrictBodyDTODeserializer;
import software.xdev.mockserver.serialization.model.BodyDTO;
import org.slf4j.event.Level;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.EXPECTATION_MATCHED;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.EXPECTATION_NOT_MATCHED;
import static software.xdev.mockserver.matchers.MatchDifference.Field.*;
import static software.xdev.mockserver.model.NottableString.string;

@SuppressWarnings("rawtypes")
public class HttpRequestPropertiesMatcher extends AbstractHttpRequestMatcher {

    private static final String[] excludedFields = {"mockServerLogger", "methodMatcher", "pathMatcher", "pathParameterMatcher", "queryStringParameterMatcher", "bodyMatcher", "headerMatcher", "cookieMatcher", "keepAliveMatcher", "bodyDTOMatcher", "sslMatcher", "controlPlaneMatcher", "responseInProgress", "objectMapper"};
    private static final String COMMA = ",";
    private static final String REQUEST_NOT_OPERATOR_IS_ENABLED = COMMA + NEW_LINE + "request 'not' operator is enabled";
    private static final String EXPECTATION_REQUEST_NOT_OPERATOR_IS_ENABLED = COMMA + NEW_LINE + "expectation's request 'not' operator is enabled";
    private static final String EXPECTATION_REQUEST_MATCHER_NOT_OPERATOR_IS_ENABLED = COMMA + NEW_LINE + "expectation's request matcher 'not' operator is enabled";
    private static final PathParametersDecoder pathParametersParser = new PathParametersDecoder();
    private static final ObjectWriter TO_STRING_OBJECT_WRITER = ObjectMapperFactory.createObjectMapper(true, false);
    private final ExpandedParameterDecoder expandedParameterDecoder;
    private int hashCode;
    private HttpRequest httpRequest;
    private List<HttpRequest> httpRequests;
    private RegexStringMatcher methodMatcher = null;
    private RegexStringMatcher pathMatcher = null;
    private MultiValueMapMatcher pathParameterMatcher = null;
    private MultiValueMapMatcher queryStringParameterMatcher = null;
    private BodyMatcher bodyMatcher = null;
    private MultiValueMapMatcher headerMatcher = null;
    private HashMapMatcher cookieMatcher = null;
    private BooleanMatcher keepAliveMatcher = null;
    private BooleanMatcher sslMatcher = null;
    private ExactStringMatcher protocolMatcher = null;
    private ObjectMapper objectMapperWithStrictBodyDTODeserializer;

    public HttpRequestPropertiesMatcher(Configuration configuration, MockServerLogger mockServerLogger) {
        super(configuration, mockServerLogger);
        this.expandedParameterDecoder = new ExpandedParameterDecoder(configuration, mockServerLogger);
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    @Override
    public List<HttpRequest> getHttpRequests() {
        return httpRequests;
    }

    @Override
    public boolean apply(RequestDefinition requestDefinition) {
        HttpRequest httpRequest = requestDefinition instanceof HttpRequest ? (HttpRequest) requestDefinition : null;
        if (this.httpRequest == null || !this.httpRequest.equals(httpRequest)) {
            this.hashCode = 0;
            this.httpRequest = httpRequest;
            this.httpRequests = Collections.singletonList(this.httpRequest);
            if (httpRequest != null) {
                withMethod(httpRequest.getMethod());
                withPath(httpRequest);
                withPathParameters(httpRequest.getPathParameters());
                withQueryStringParameters(httpRequest.getQueryStringParameters());
                withBody(httpRequest.getBody());
                withHeaders(httpRequest.getHeaders());
                withCookies(httpRequest.getCookies());
                withKeepAlive(httpRequest.isKeepAlive());
                withSsl(httpRequest.isSecure());
                withProtocol(httpRequest.getProtocol());
            }
            return true;
        } else {
            return false;
        }
    }

    public HttpRequestPropertiesMatcher withControlPlaneMatcher(boolean controlPlaneMatcher) {
        this.controlPlaneMatcher = controlPlaneMatcher;
        return this;
    }

    private void withMethod(NottableString method) {
        this.methodMatcher = new RegexStringMatcher(mockServerLogger, method, controlPlaneMatcher);
    }

    private void withPath(HttpRequest httpRequest) {
        this.pathMatcher = new RegexStringMatcher(mockServerLogger, pathParametersParser.normalisePathWithParametersForMatching(httpRequest), controlPlaneMatcher);
    }

    private void withPathParameters(Parameters parameters) {
        this.pathParameterMatcher = new MultiValueMapMatcher(mockServerLogger, parameters, controlPlaneMatcher);
    }

    private void withQueryStringParameters(Parameters parameters) {
        this.queryStringParameterMatcher = new MultiValueMapMatcher(mockServerLogger, parameters, controlPlaneMatcher);
    }

    private void withBody(Body body) {
        this.bodyMatcher = buildBodyMatcher(body);
    }

    private BodyMatcher buildBodyMatcher(Body body) {
        BodyMatcher bodyMatcher = null;
        if (body != null) {
            switch (body.getType()) {
                case STRING:
                    StringBody stringBody = (StringBody) body;
                    if (stringBody.isSubString()) {
                        bodyMatcher = new SubStringMatcher(mockServerLogger, string(stringBody.getValue()));
                    } else {
                        bodyMatcher = new ExactStringMatcher(mockServerLogger, string(stringBody.getValue()));
                    }
                    break;
                case REGEX:
                    RegexBody regexBody = (RegexBody) body;
                    bodyMatcher = new RegexStringMatcher(mockServerLogger, string(regexBody.getValue()), controlPlaneMatcher);
                    break;
                case PARAMETERS:
                    ParameterBody parameterBody = (ParameterBody) body;
                    bodyMatcher = new ParameterStringMatcher(configuration, mockServerLogger, parameterBody.getValue(), controlPlaneMatcher);
                    break;
                case BINARY:
                    BinaryBody binaryBody = (BinaryBody) body;
                    bodyMatcher = new BinaryMatcher(mockServerLogger, binaryBody.getValue());
                    break;
            }
            if (body.isNot()) {
                //noinspection ConstantConditions
                bodyMatcher = notMatcher(bodyMatcher);
            }
        }
        return bodyMatcher;
    }

    private void withHeaders(Headers headers) {
        this.headerMatcher = new MultiValueMapMatcher(mockServerLogger, headers, controlPlaneMatcher);
    }

    private void withCookies(Cookies cookies) {
        this.cookieMatcher = new HashMapMatcher(mockServerLogger, cookies, controlPlaneMatcher);
    }

    private void withKeepAlive(Boolean keepAlive) {
        this.keepAliveMatcher = new BooleanMatcher(mockServerLogger, keepAlive);
    }

    private void withSsl(Boolean isSsl) {
        this.sslMatcher = new BooleanMatcher(mockServerLogger, isSsl);
    }

    private void withProtocol(Protocol protocol) {
        this.protocolMatcher = new ExactStringMatcher(mockServerLogger, protocol != null ? string(protocol.name()) : null);
    }

    public boolean matches(final MatchDifference context, final RequestDefinition requestDefinition) {
        if (requestDefinition instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) requestDefinition;
            StringBuilder becauseBuilder = new StringBuilder();
            boolean overallMatch = matches(context, request, becauseBuilder);
            if (!controlPlaneMatcher) {
                if (overallMatch) {
                    if (MockServerLogger.isEnabled(Level.INFO)) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setType(EXPECTATION_MATCHED)
                                .setLogLevel(Level.INFO)
                                .setCorrelationId(requestDefinition.getLogCorrelationId())
                                .setHttpRequest(request)
                                .setExpectation(this.expectation)
                                .setMessageFormat(this.expectation == null ? REQUEST_DID_MATCH : EXPECTATION_DID_MATCH)
                                .setArguments(request, (this.expectation == null ? this : this.expectation.clone()))
                        );
                    }
                } else {
                    becauseBuilder.replace(0, 1, "");
                    String because = becauseBuilder.toString();
                    if (MockServerLogger.isEnabled(Level.INFO)) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setType(EXPECTATION_NOT_MATCHED)
                                .setLogLevel(Level.INFO)
                                .setCorrelationId(requestDefinition.getLogCorrelationId())
                                .setHttpRequest(request)
                                .setExpectation(this.expectation)
                                .setMessageFormat(this.expectation == null ? didNotMatchRequestBecause : becauseBuilder.length() > 0 ? didNotMatchExpectationBecause : didNotMatchExpectationWithoutBecause)
                                .setArguments(request, (this.expectation == null ? this : this.expectation.clone()), because)
                                .setBecause(because)
                        );
                    }
                }
            }
            return overallMatch;
        } else {
            return requestDefinition == null;
        }
    }

    private boolean matches(MatchDifference context, HttpRequest request, StringBuilder becauseBuilder) {
        if (isActive()) {
            if (request == this.httpRequest) {
                return true;
            } else if (this.httpRequest == null) {
                return true;
            } else {
                MatchDifferenceCount matchDifferenceCount = new MatchDifferenceCount(request);
                if (request != null) {
                    boolean methodMatches = StringUtils.isBlank(request.getMethod().getValue()) || matches(METHOD, context, methodMatcher, request.getMethod());
                    if (failFast(methodMatcher, context, matchDifferenceCount, becauseBuilder, methodMatches, METHOD)) {
                        return false;
                    }

                    boolean pathMatches = StringUtils.isBlank(request.getPath().getValue()) || matches(PATH, context, pathMatcher, controlPlaneMatcher ? pathParametersParser.normalisePathWithParametersForMatching(request) : request.getPath());
                    Parameters pathParameters = null;
                    try {
                        pathParameters = pathParametersParser.extractPathParameters(httpRequest, request);
                    } catch (IllegalArgumentException iae) {
                        if (!httpRequest.getPath().isBlank()) {
                            if (context != null) {
                                context.currentField(PATH);
                                context.addDifference(mockServerLogger, iae.getMessage());
                            }
                            pathMatches = false;
                        }
                    }
                    if (failFast(pathMatcher, context, matchDifferenceCount, becauseBuilder, pathMatches, PATH)) {
                        return false;
                    }

                    boolean bodyMatches = bodyMatches(context, request);
                    if (failFast(bodyMatcher, context, matchDifferenceCount, becauseBuilder, bodyMatches, BODY)) {
                        return false;
                    }

                    boolean headersMatch = matches(HEADERS, context, headerMatcher, request.getHeaders());
                    if (failFast(headerMatcher, context, matchDifferenceCount, becauseBuilder, headersMatch, HEADERS)) {
                        return false;
                    }

                    boolean cookiesMatch = matches(COOKIES, context, cookieMatcher, request.getCookies());
                    if (failFast(cookieMatcher, context, matchDifferenceCount, becauseBuilder, cookiesMatch, COOKIES)) {
                        return false;
                    }

                    boolean pathParametersMatches = true;
                    if (!httpRequest.getPath().isBlank()) {
                        if (!controlPlaneMatcher) {
                            expandedParameterDecoder.splitParameters(httpRequest.getPathParameters(), pathParameters);
                        }
                        MultiValueMapMatcher pathParameterMatcher = this.pathParameterMatcher;
                        if (controlPlaneMatcher) {
                            Parameters controlPlaneParameters;
                            try {
                                controlPlaneParameters = pathParametersParser.extractPathParameters(request, httpRequest);
                            } catch (IllegalArgumentException iae) {
                                controlPlaneParameters = new Parameters();
                            }
                            pathParameterMatcher = new MultiValueMapMatcher(mockServerLogger, controlPlaneParameters, controlPlaneMatcher);

                        }
                        pathParametersMatches = matches(PATH_PARAMETERS, context, pathParameterMatcher, pathParameters);
                    }
                    if (failFast(this.pathParameterMatcher, context, matchDifferenceCount, becauseBuilder, pathParametersMatches, PATH_PARAMETERS)) {
                        return false;
                    }

                    if (!controlPlaneMatcher) {
                        expandedParameterDecoder.splitParameters(httpRequest.getQueryStringParameters(), request.getQueryStringParameters());
                    }
                    boolean queryStringParametersMatches = matches(QUERY_PARAMETERS, context, queryStringParameterMatcher, request.getQueryStringParameters());
                    if (failFast(queryStringParameterMatcher, context, matchDifferenceCount, becauseBuilder, queryStringParametersMatches, QUERY_PARAMETERS)) {
                        return false;
                    }

                    boolean keepAliveMatches = matches(KEEP_ALIVE, context, keepAliveMatcher, request.isKeepAlive());
                    if (failFast(keepAliveMatcher, context, matchDifferenceCount, becauseBuilder, keepAliveMatches, KEEP_ALIVE)) {
                        return false;
                    }

                    boolean sslMatches = matches(SECURE, context, sslMatcher, request.isSecure());
                    if (failFast(sslMatcher, context, matchDifferenceCount, becauseBuilder, sslMatches, SECURE)) {
                        return false;
                    }

                    boolean protocolMatches = matches(PROTOCOL, context, protocolMatcher, request.getProtocol() != null ? string(request.getProtocol().name()) : null);
                    if (failFast(protocolMatcher, context, matchDifferenceCount, becauseBuilder, protocolMatches, PROTOCOL)) {
                        return false;
                    }

                    boolean combinedResultAreTrue = combinedResultAreTrue(matchDifferenceCount.getFailures() == 0, request.isNot(), this.httpRequest.isNot(), not);
                    if (!controlPlaneMatcher && combinedResultAreTrue) {
                        // ensure actions have path parameters available to them
                        request.withPathParameters(pathParameters);
                    }
                    return combinedResultAreTrue;
                } else {
                    return combinedResultAreTrue(true, this.httpRequest.isNot(), not);
                }
            }
        }
        return false;
    }

    private boolean failFast(Matcher<?> matcher, MatchDifference context, MatchDifferenceCount matchDifferenceCount, StringBuilder becauseBuilder, boolean fieldMatches, MatchDifference.Field fieldName) {
        // update because builder
        if (!controlPlaneMatcher) {
            becauseBuilder
                .append(NEW_LINE)
                .append(fieldName.getName()).append(fieldMatches ? MATCHED : DID_NOT_MATCH);
            if (context != null && context.getDifferences(fieldName) != null && !context.getDifferences(fieldName).isEmpty()) {
                becauseBuilder
                    .append(COLON_NEW_LINES)
                    .append(Joiner.on(NEW_LINE).join(context.getDifferences(fieldName)));
            }
        }
        if (!fieldMatches) {
            if (!controlPlaneMatcher) {
                if (matchDifferenceCount.getHttpRequest().isNot()) {
                    becauseBuilder
                        .append(REQUEST_NOT_OPERATOR_IS_ENABLED);
                }
                if (this.httpRequest.isNot()) {
                    becauseBuilder
                        .append(EXPECTATION_REQUEST_NOT_OPERATOR_IS_ENABLED);
                }
                if (not) {
                    becauseBuilder
                        .append(EXPECTATION_REQUEST_MATCHER_NOT_OPERATOR_IS_ENABLED);
                }
            }
        }
        // update match difference and potentially fail fast
        if (!fieldMatches) {
            matchDifferenceCount.incrementFailures();
        }
        if (matcher != null && !matcher.isBlank() && configuration.matchersFailFast()) {
            return combinedResultAreTrue(matchDifferenceCount.getFailures() != 0, matchDifferenceCount.getHttpRequest().isNot(), this.httpRequest.isNot(), not);
        }
        return false;
    }

    /**
     * true for odd number of false inputs
     */
    private static boolean combinedResultAreTrue(boolean... inputs) {
        int count = 0;
        for (boolean input : inputs) {
            count += (input ? 1 : 0);
        }
        return count % 2 != 0;
    }

    private boolean bodyMatches(MatchDifference context, HttpRequest request) {
        boolean bodyMatches;
        if (bodyMatcher != null) {
            if (controlPlaneMatcher) {
                if (httpRequest.getBody() != null && String.valueOf(httpRequest.getBody()).equalsIgnoreCase(String.valueOf(request.getBody()))) {
                    bodyMatches = true;
                } else if (bodyMatches(bodyMatcher, context, request)) {
                    // allow match of entries in EchoServer log (i.e. for java client integration tests)
                    bodyMatches = true;
                } else {
                    if (isNotBlank(request.getBodyAsJsonOrXmlString())) {
                        try {
                            BodyDTO bodyDTO = getObjectMapperWithStrictBodyDTODeserializer().readValue(request.getBodyAsJsonOrXmlString(), BodyDTO.class);
                            if (bodyDTO != null) {
                                bodyMatches = bodyMatches(
                                    buildBodyMatcher(bodyDTO.buildObject()),
                                    context,
                                    httpRequest
                                );
                            } else {
                                bodyMatches = false;
                            }
                        } catch (Throwable ignore) {
                            // ignore this exception as this exception would typically get thrown for "normal" HTTP requests (i.e. not clear or retrieve)
                            bodyMatches = false;
                        }
                    } else {
                        bodyMatches = false;
                    }
                }
            } else {
                bodyMatches = bodyMatches(bodyMatcher, context, request);
            }
        } else {
            bodyMatches = true;
        }
        return bodyMatches;
    }

    @SuppressWarnings("unchecked")
    private boolean bodyMatches(BodyMatcher bodyMatcher, MatchDifference context, HttpRequest request) {
        boolean bodyMatches;
        if (httpRequest.getBody().getOptional() != null && httpRequest.getBody().getOptional() && request.getBody() == null) {
            bodyMatches = true;
        } else if (bodyMatcher instanceof BinaryMatcher) {
            bodyMatches = matches(BODY, context, bodyMatcher, request.getBodyAsRawBytes());
        } else {
            if (bodyMatcher instanceof ExactStringMatcher ||
                bodyMatcher instanceof SubStringMatcher ||
                bodyMatcher instanceof RegexStringMatcher) {
                // string body matcher
                bodyMatches = matches(BODY, context, bodyMatcher, string(request.getBodyAsString()));
            } else {
                bodyMatches = matches(BODY, context, bodyMatcher, request.getBodyAsString());
            }
        }
        return bodyMatches;
    }

    private <T> boolean matches(MatchDifference.Field field, MatchDifference context, Matcher<T> matcher, T t) {
        if (context != null) {
            context.currentField(field);
        }
        boolean result = false;

        if (matcher == null) {
            result = true;
        } else if (matcher.matches(context, t)) {
            result = true;
        }

        return result;
    }

    @Override
    public String toString() {
        try {
            return TO_STRING_OBJECT_WRITER
                .writeValueAsString(httpRequest);
        } catch (Exception e) {
            return super.toString();
        }
    }

    @Override
    @JsonIgnore
    public String[] fieldsExcludedFromEqualsAndHashCode() {
        return excludedFields;
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
        if (!super.equals(o)) {
            return false;
        }
        HttpRequestPropertiesMatcher that = (HttpRequestPropertiesMatcher) o;
        return Objects.equals(httpRequest, that.httpRequest);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), httpRequest);
        }
        return hashCode;
    }

    private ObjectMapper getObjectMapperWithStrictBodyDTODeserializer() {
        if (objectMapperWithStrictBodyDTODeserializer == null) {
            objectMapperWithStrictBodyDTODeserializer = ObjectMapperFactory.createObjectMapper(new StrictBodyDTODeserializer());
        }
        return objectMapperWithStrictBodyDTODeserializer;
    }
}
