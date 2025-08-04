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

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.matchers.MatchDifference.Field.BODY;
import static software.xdev.mockserver.matchers.MatchDifference.Field.COOKIES;
import static software.xdev.mockserver.matchers.MatchDifference.Field.HEADERS;
import static software.xdev.mockserver.matchers.MatchDifference.Field.KEEP_ALIVE;
import static software.xdev.mockserver.matchers.MatchDifference.Field.METHOD;
import static software.xdev.mockserver.matchers.MatchDifference.Field.PATH;
import static software.xdev.mockserver.matchers.MatchDifference.Field.PATH_PARAMETERS;
import static software.xdev.mockserver.matchers.MatchDifference.Field.PROTOCOL;
import static software.xdev.mockserver.matchers.MatchDifference.Field.QUERY_PARAMETERS;
import static software.xdev.mockserver.model.NottableString.string;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import software.xdev.mockserver.codec.ExpandedParameterDecoder;
import software.xdev.mockserver.codec.PathParametersDecoder;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.model.BinaryBody;
import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.NottableString;
import software.xdev.mockserver.model.ParameterBody;
import software.xdev.mockserver.model.Parameters;
import software.xdev.mockserver.model.Protocol;
import software.xdev.mockserver.model.RegexBody;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.model.StringBody;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.serialization.deserializers.body.StrictBodyDTODeserializer;
import software.xdev.mockserver.serialization.model.BodyDTO;
import software.xdev.mockserver.util.StringUtils;


@SuppressWarnings({"rawtypes", "PMD.GodClass"})
public class HttpRequestPropertiesMatcher extends AbstractHttpRequestMatcher
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpRequestPropertiesMatcher.class);
	
	private static final String COMMA = ",";
	private static final String REQUEST_NOT_OPERATOR_IS_ENABLED =
		COMMA + NEW_LINE + "request 'not' operator is enabled";
	private static final String EXPECTATION_REQUEST_NOT_OPERATOR_IS_ENABLED =
		COMMA + NEW_LINE + "expectation's request 'not' operator is enabled";
	private static final String EXPECTATION_REQUEST_MATCHER_NOT_OPERATOR_IS_ENABLED =
		COMMA + NEW_LINE + "expectation's request matcher 'not' operator is enabled";
	private static final PathParametersDecoder PATH_PARAMETERS_DECODER = new PathParametersDecoder();
	private static final ObjectWriter TO_STRING_OBJECT_WRITER = ObjectMapperFactory.createObjectMapper(true, false);
	private final ExpandedParameterDecoder expandedParameterDecoder;
	private int hashCode;
	private HttpRequest httpRequest;
	private List<HttpRequest> httpRequests;
	private RegexStringMatcher methodMatcher;
	private RegexStringMatcher pathMatcher;
	private MultiValueMapMatcher pathParameterMatcher;
	private MultiValueMapMatcher queryStringParameterMatcher;
	private BodyMatcher bodyMatcher;
	private MultiValueMapMatcher headerMatcher;
	private HashMapMatcher cookieMatcher;
	private BooleanMatcher keepAliveMatcher;
	private ExactStringMatcher protocolMatcher;
	private ObjectMapper objectMapperWithStrictBodyDTODeserializer;
	
	public HttpRequestPropertiesMatcher(final ServerConfiguration configuration)
	{
		super(configuration);
		this.expandedParameterDecoder = new ExpandedParameterDecoder(configuration);
	}
	
	public HttpRequest getHttpRequest()
	{
		return this.httpRequest;
	}
	
	@Override
	public List<HttpRequest> getHttpRequests()
	{
		return this.httpRequests;
	}
	
	@Override
	public boolean apply(final RequestDefinition requestDefinition)
	{
		final HttpRequest httpRequest = requestDefinition instanceof final HttpRequest r ? r : null;
		if(this.httpRequest == null || !this.httpRequest.equals(httpRequest))
		{
			this.hashCode = 0;
			this.httpRequest = httpRequest;
			this.httpRequests = Collections.singletonList(this.httpRequest);
			if(httpRequest != null)
			{
				this.withMethod(httpRequest.getMethod());
				this.withPath(httpRequest);
				this.withPathParameters(httpRequest.getPathParameters());
				this.withQueryStringParameters(httpRequest.getQueryStringParameters());
				this.withBody(httpRequest.getBody());
				this.withHeaders(httpRequest.getHeaders());
				this.withCookies(httpRequest.getCookies());
				this.withKeepAlive(httpRequest.isKeepAlive());
				this.withProtocol(httpRequest.getProtocol());
			}
			return true;
		}
		return false;
	}
	
	public HttpRequestPropertiesMatcher withControlPlaneMatcher(final boolean controlPlaneMatcher)
	{
		this.controlPlaneMatcher = controlPlaneMatcher;
		return this;
	}
	
	private void withMethod(final NottableString method)
	{
		this.methodMatcher = new RegexStringMatcher(method, this.controlPlaneMatcher);
	}
	
	private void withPath(final HttpRequest httpRequest)
	{
		this.pathMatcher = new RegexStringMatcher(
			PATH_PARAMETERS_DECODER.normalisePathWithParametersForMatching(httpRequest),
			this.controlPlaneMatcher);
	}
	
	private void withPathParameters(final Parameters parameters)
	{
		this.pathParameterMatcher = new MultiValueMapMatcher(parameters, this.controlPlaneMatcher);
	}
	
	private void withQueryStringParameters(final Parameters parameters)
	{
		this.queryStringParameterMatcher = new MultiValueMapMatcher(parameters, this.controlPlaneMatcher);
	}
	
	private void withBody(final Body body)
	{
		this.bodyMatcher = this.buildBodyMatcher(body);
	}
	
	private BodyMatcher buildBodyMatcher(final Body body)
	{
		if(body == null)
		{
			return null;
		}
		
		final BodyMatcher matcher = this.determineBodyMatcherByType(body);
		return body.isNot() ? notMatcher(matcher) : matcher;
	}
	
	private BodyMatcher determineBodyMatcherByType(final Body body)
	{
		return switch(body.getType())
		{
			case STRING:
				final StringBody stringBody = (StringBody)body;
				final NottableString string = string(stringBody.getValue());
				yield stringBody.isSubString() ? new SubStringMatcher(string) : new ExactStringMatcher(string);
			case REGEX:
				final RegexBody regexBody = (RegexBody)body;
				yield new RegexStringMatcher(string(regexBody.getValue()), this.controlPlaneMatcher);
			case PARAMETERS:
				final ParameterBody parameterBody = (ParameterBody)body;
				yield new ParameterStringMatcher(
					this.configuration,
					parameterBody.getValue(),
					this.controlPlaneMatcher);
			case BINARY:
				final BinaryBody binaryBody = (BinaryBody)body;
				yield new BinaryMatcher(binaryBody.getValue());
		};
	}
	
	private void withHeaders(final Headers headers)
	{
		this.headerMatcher = new MultiValueMapMatcher(headers, this.controlPlaneMatcher);
	}
	
	private void withCookies(final Cookies cookies)
	{
		this.cookieMatcher = new HashMapMatcher(cookies, this.controlPlaneMatcher);
	}
	
	private void withKeepAlive(final Boolean keepAlive)
	{
		this.keepAliveMatcher = new BooleanMatcher(keepAlive);
	}
	
	private void withProtocol(final Protocol protocol)
	{
		this.protocolMatcher = new ExactStringMatcher(protocol != null ? string(protocol.name()) : null);
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	public boolean matches(final MatchDifference context, final RequestDefinition requestDefinition)
	{
		if(requestDefinition instanceof final HttpRequest request)
		{
			final StringBuilder becauseBuilder = new StringBuilder();
			final boolean overallMatch = this.matches(context, request, becauseBuilder);
			if(!this.controlPlaneMatcher)
			{
				if(overallMatch)
				{
					if(LOG.isInfoEnabled())
					{
						LOG.info(
							this.expectation == null ? REQUEST_DID_MATCH : EXPECTATION_DID_MATCH,
							request,
							this.expectation == null ? this : this.expectation.clone());
					}
				}
				else
				{
					becauseBuilder.replace(0, 1, "");
					final String because = becauseBuilder.toString();
					if(LOG.isInfoEnabled())
					{
						LOG.info(
							this.expectation == null
								? this.didNotMatchRequestBecause
								: !becauseBuilder.isEmpty()
								? this.didNotMatchExpectationBecause
								: this.didNotMatchExpectationWithoutBecause,
							request,
							this.expectation == null ? this : this.expectation.clone(),
							because);
					}
				}
			}
			return overallMatch;
		}
		else
		{
			return requestDefinition == null;
		}
	}
	
	@SuppressWarnings({
		"checkstyle:MethodLength",
		"PMD.CognitiveComplexity",
		"PMD.NPathComplexity",
		"PMD.CyclomaticComplexity",
		"PMD.NcssCount"})
	private boolean matches(
		final MatchDifference context,
		final HttpRequest request,
		final StringBuilder becauseBuilder)
	{
		if(this.isActive())
		{
			if(request == this.httpRequest)
			{
				return true;
			}
			else if(this.httpRequest == null)
			{
				return true;
			}
			else
			{
				final MatchDifferenceCount matchDifferenceCount = new MatchDifferenceCount(request);
				if(request != null)
				{
					final boolean methodMatches = StringUtils.isBlank(request.getMethod().getValue()) || this.matches(
						METHOD,
						context,
						this.methodMatcher,
						request.getMethod());
					if(this.failFast(
						this.methodMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						methodMatches,
						METHOD))
					{
						return false;
					}
					
					boolean pathMatches = StringUtils.isBlank(request.getPath().getValue()) || this.matches(
						PATH,
						context,
						this.pathMatcher,
						this.controlPlaneMatcher
							? PATH_PARAMETERS_DECODER.normalisePathWithParametersForMatching(request)
							: request.getPath());
					Parameters pathParameters = null;
					try
					{
						pathParameters = PATH_PARAMETERS_DECODER.extractPathParameters(this.httpRequest, request);
					}
					catch(final IllegalArgumentException iae)
					{
						if(!this.httpRequest.getPath().isBlank())
						{
							if(context != null)
							{
								context.currentField(PATH);
								context.addDifference(iae.getMessage());
							}
							pathMatches = false;
						}
					}
					if(this.failFast(
						this.pathMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						pathMatches,
						PATH))
					{
						return false;
					}
					
					final boolean bodyMatches = this.bodyMatches(context, request);
					if(this.failFast(
						this.bodyMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						bodyMatches,
						BODY))
					{
						return false;
					}
					
					final boolean headersMatch =
						this.matches(HEADERS, context, this.headerMatcher, request.getHeaders());
					if(this.failFast(
						this.headerMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						headersMatch,
						HEADERS))
					{
						return false;
					}
					
					final boolean cookiesMatch =
						this.matches(COOKIES, context, this.cookieMatcher, request.getCookies());
					if(this.failFast(
						this.cookieMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						cookiesMatch,
						COOKIES))
					{
						return false;
					}
					
					boolean pathParametersMatches = true;
					if(!this.httpRequest.getPath().isBlank())
					{
						if(!this.controlPlaneMatcher)
						{
							this.expandedParameterDecoder.splitParameters(
								this.httpRequest.getPathParameters(),
								pathParameters);
						}
						MultiValueMapMatcher pathParameterMatcher = this.pathParameterMatcher;
						if(this.controlPlaneMatcher)
						{
							Parameters controlPlaneParameters;
							try
							{
								controlPlaneParameters =
									PATH_PARAMETERS_DECODER.extractPathParameters(request, this.httpRequest);
							}
							catch(final IllegalArgumentException iae)
							{
								controlPlaneParameters = new Parameters();
							}
							pathParameterMatcher =
								new MultiValueMapMatcher(controlPlaneParameters, this.controlPlaneMatcher);
						}
						pathParametersMatches = this.matches(PATH_PARAMETERS, context, pathParameterMatcher,
							pathParameters);
					}
					if(this.failFast(
						this.pathParameterMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						pathParametersMatches,
						PATH_PARAMETERS))
					{
						return false;
					}
					
					if(!this.controlPlaneMatcher)
					{
						this.expandedParameterDecoder.splitParameters(
							this.httpRequest.getQueryStringParameters(),
							request.getQueryStringParameters());
					}
					final boolean queryStringParametersMatches = this.matches(
						QUERY_PARAMETERS,
						context,
						this.queryStringParameterMatcher,
						request.getQueryStringParameters());
					if(this.failFast(
						this.queryStringParameterMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						queryStringParametersMatches,
						QUERY_PARAMETERS))
					{
						return false;
					}
					
					final boolean keepAliveMatches = this.matches(KEEP_ALIVE, context,
						this.keepAliveMatcher, request.isKeepAlive());
					if(this.failFast(
						this.keepAliveMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						keepAliveMatches,
						KEEP_ALIVE))
					{
						return false;
					}
					
					final boolean protocolMatches = this.matches(
						PROTOCOL,
						context,
						this.protocolMatcher,
						request.getProtocol() != null ? string(request.getProtocol().name()) : null);
					if(this.failFast(
						this.protocolMatcher,
						context,
						matchDifferenceCount,
						becauseBuilder,
						protocolMatches,
						PROTOCOL))
					{
						return false;
					}
					
					final boolean combinedResultAreTrue = combinedResultAreTrue(
						matchDifferenceCount.getFailures() == 0,
						request.isNot(),
						this.httpRequest.isNot(),
						this.not);
					if(!this.controlPlaneMatcher && combinedResultAreTrue)
					{
						// ensure actions have path parameters available to them
						request.withPathParameters(pathParameters);
					}
					return combinedResultAreTrue;
				}
				else
				{
					return combinedResultAreTrue(true, this.httpRequest.isNot(), this.not);
				}
			}
		}
		return false;
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
	private boolean failFast(
		final Matcher<?> matcher,
		final MatchDifference context,
		final MatchDifferenceCount matchDifferenceCount,
		final StringBuilder becauseBuilder,
		final boolean fieldMatches,
		final MatchDifference.Field fieldName)
	{
		// update because builder
		if(!this.controlPlaneMatcher)
		{
			becauseBuilder
				.append(NEW_LINE)
				.append(fieldName.getName()).append(fieldMatches ? MATCHED : DID_NOT_MATCH);
			if(context != null && context.getDifferences(fieldName) != null && !context.getDifferences(fieldName)
				.isEmpty())
			{
				becauseBuilder
					.append(COLON_NEW_LINES)
					.append(String.join(NEW_LINE, context.getDifferences(fieldName)));
			}
		}
		if(!fieldMatches
			&& !this.controlPlaneMatcher)
		{
			if(matchDifferenceCount.getHttpRequest().isNot())
			{
				becauseBuilder
					.append(REQUEST_NOT_OPERATOR_IS_ENABLED);
			}
			if(this.httpRequest.isNot())
			{
				becauseBuilder
					.append(EXPECTATION_REQUEST_NOT_OPERATOR_IS_ENABLED);
			}
			if(this.not)
			{
				becauseBuilder
					.append(EXPECTATION_REQUEST_MATCHER_NOT_OPERATOR_IS_ENABLED);
			}
		}
		
		// update match difference and potentially fail fast
		if(!fieldMatches)
		{
			matchDifferenceCount.incrementFailures();
		}
		if(matcher != null && !matcher.isBlank() && this.configuration.matchersFailFast())
		{
			return combinedResultAreTrue(
				matchDifferenceCount.getFailures() != 0,
				matchDifferenceCount.getHttpRequest().isNot(),
				this.httpRequest.isNot(),
				this.not);
		}
		return false;
	}
	
	/**
	 * true for odd number of false inputs
	 */
	private static boolean combinedResultAreTrue(final boolean... inputs)
	{
		int count = 0;
		for(final boolean input : inputs)
		{
			count += input ? 1 : 0;
		}
		return count % 2 != 0;
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	private boolean bodyMatches(final MatchDifference context, final HttpRequest request)
	{
		boolean bodyMatches;
		if(this.bodyMatcher != null)
		{
			if(this.controlPlaneMatcher)
			{
				if(this.httpRequest.getBody() != null && String.valueOf(this.httpRequest.getBody())
					.equalsIgnoreCase(String.valueOf(request.getBody())))
				{
					bodyMatches = true;
				}
				else if(this.bodyMatches(this.bodyMatcher, context, request))
				{
					// allow match of entries in EchoServer log (i.e. for java client integration tests)
					bodyMatches = true;
				}
				else
				{
					if(isNotBlank(request.getBodyAsJsonOrXmlString()))
					{
						try
						{
							final BodyDTO bodyDTO = this.getObjectMapperWithStrictBodyDTODeserializer().readValue(
								request.getBodyAsJsonOrXmlString(),
								BodyDTO.class);
							if(bodyDTO != null)
							{
								bodyMatches = this.bodyMatches(
									this.buildBodyMatcher(bodyDTO.buildObject()),
									context,
									this.httpRequest
								);
							}
							else
							{
								bodyMatches = false;
							}
						}
						catch(final Exception ignore)
						{
							// ignore this exception as this exception would typically get thrown for "normal" HTTP
							// requests (i.e. not clear or retrieve)
							bodyMatches = false;
						}
					}
					else
					{
						bodyMatches = false;
					}
				}
			}
			else
			{
				bodyMatches = this.bodyMatches(this.bodyMatcher, context, request);
			}
		}
		else
		{
			bodyMatches = true;
		}
		return bodyMatches;
	}
	
	@SuppressWarnings("unchecked")
	private boolean bodyMatches(
		final BodyMatcher bodyMatcher, final MatchDifference context,
		final HttpRequest request)
	{
		final boolean bodyMatches;
		if(this.httpRequest.getBody().getOptional() != null && this.httpRequest.getBody().getOptional()
			&& request.getBody() == null)
		{
			bodyMatches = true;
		}
		else if(bodyMatcher instanceof BinaryMatcher)
		{
			bodyMatches = this.matches(BODY, context, bodyMatcher, request.getBodyAsRawBytes());
		}
		else
		{
			if(bodyMatcher instanceof ExactStringMatcher
				|| bodyMatcher instanceof SubStringMatcher
				|| bodyMatcher instanceof RegexStringMatcher)
			{
				// string body matcher
				bodyMatches = this.matches(BODY, context, bodyMatcher, string(request.getBodyAsString()));
			}
			else
			{
				bodyMatches = this.matches(BODY, context, bodyMatcher, request.getBodyAsString());
			}
		}
		return bodyMatches;
	}
	
	private <T> boolean matches(
		final MatchDifference.Field field,
		final MatchDifference context,
		final Matcher<T> matcher,
		final T t)
	{
		if(context != null)
		{
			context.currentField(field);
		}
		return matcher == null || matcher.matches(context, t);
	}
	
	@Override
	public String toString()
	{
		try
		{
			return TO_STRING_OBJECT_WRITER.writeValueAsString(this.httpRequest);
		}
		catch(final Exception e)
		{
			return super.toString();
		}
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		if(this.hashCode() != o.hashCode())
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		final HttpRequestPropertiesMatcher that = (HttpRequestPropertiesMatcher)o;
		return Objects.equals(this.httpRequest, that.httpRequest);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(super.hashCode(), this.httpRequest);
		}
		return this.hashCode;
	}
	
	private ObjectMapper getObjectMapperWithStrictBodyDTODeserializer()
	{
		if(this.objectMapperWithStrictBodyDTODeserializer == null)
		{
			this.objectMapperWithStrictBodyDTODeserializer =
				ObjectMapperFactory.createObjectMapper(new StrictBodyDTODeserializer());
		}
		return this.objectMapperWithStrictBodyDTODeserializer;
	}
}
