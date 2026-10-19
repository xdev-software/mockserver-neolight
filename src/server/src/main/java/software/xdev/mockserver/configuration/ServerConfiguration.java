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
package software.xdev.mockserver.configuration;

import software.xdev.mockserver.model.BinaryProxyListener;


@SuppressWarnings({"UnusedReturnValue", "unused"})
public class ServerConfiguration extends Configuration
{
	public static ServerConfiguration configuration()
	{
		return new ServerConfiguration();
	}
	
	// logging
	private Boolean detailedMatchFailures;
	
	private Integer maxExpectations;
	private Integer maxLogEntries;
	
	// scalability
	private Integer nioEventLoopThreadCount;
	private Integer actionHandlerThreadCount;
	private Boolean matchersFailFast;
	
	// socket
	private Boolean alwaysCloseSocketConnections;
	private String localBoundIP;
	
	// http request parsing
	private Integer maxInitialLineLength;
	private Integer maxHeaderSize;
	private Integer maxChunkSize;
	private Boolean useSemicolonAsQueryParameterSeparator;
	private Boolean assumeAllRequestsAreHttp;
	
	// non http proxying
	private BinaryProxyListener binaryProxyListener;
	
	// CORS
	private Boolean enableCORSForAPI;
	private Boolean enableCORSForAllResponses;
	private String corsAllowOrigin;
	private String corsAllowMethods;
	private String corsAllowHeaders;
	private Boolean corsAllowCredentials;
	private Integer corsMaxAgeInSeconds;
	
	// verification
	private Integer maximumNumberOfRequestToReturnInVerificationFailure;
	
	// proxy
	private String proxyAuthenticationRealm;
	private String proxyAuthenticationUsername;
	private String proxyAuthenticationPassword;
	private String noProxyHosts;
	
	// liveness
	private String livenessHttpGetPath;
	
	public Boolean detailedMatchFailures()
	{
		if(this.detailedMatchFailures == null)
		{
			return ServerConfigurationProperties.detailedMatchFailures();
		}
		return this.detailedMatchFailures;
	}
	
	/**
	 * If true (the default) the log event recording that a request matcher did not match will include a detailed
	 * reason
	 * why each non-matching field did not match.
	 *
	 * @param detailedMatchFailures enabled detailed match failure log events
	 */
	public ServerConfiguration detailedMatchFailures(final Boolean detailedMatchFailures)
	{
		this.detailedMatchFailures = detailedMatchFailures;
		return this;
	}
	
	public Integer maxExpectations()
	{
		if(this.maxExpectations == null)
		{
			return ServerConfigurationProperties.maxExpectations();
		}
		return this.maxExpectations;
	}
	
	/**
	 * <p>
	 * Maximum number of expectations stored in memory.  Expectations are stored in a circular queue so once this limit
	 * is reach the oldest and lowest priority expectations are overwritten
	 * </p>
	 * <p>
	 * The default maximum depends on the available memory in the JVM with an upper limit of 5000
	 * </p>
	 *
	 * @param maxExpectations maximum number of expectations to store
	 */
	public ServerConfiguration maxExpectations(final Integer maxExpectations)
	{
		this.maxExpectations = maxExpectations;
		return this;
	}
	
	public Integer maxLogEntries()
	{
		if(this.maxLogEntries == null)
		{
			return ServerConfigurationProperties.maxLogEntries();
		}
		return this.maxLogEntries;
	}
	
	/**
	 * <p>
	 * Maximum number of log entries stored in memory.  Log entries are stored in a circular queue so once this
	 * limit is
	 * reach the oldest log entries are overwritten
	 * </p>
	 * <p>
	 * The default maximum depends on the available memory in the JVM with an upper limit of 60000
	 * </p>
	 *
	 * @param maxLogEntries maximum number of expectations to store
	 */
	public ServerConfiguration maxLogEntries(final Integer maxLogEntries)
	{
		this.maxLogEntries = maxLogEntries;
		return this;
	}
	
	public Integer nioEventLoopThreadCount()
	{
		if(this.nioEventLoopThreadCount == null)
		{
			return ServerConfigurationProperties.nioEventLoopThreadCount();
		}
		return this.nioEventLoopThreadCount;
	}
	
	/**
	 * <p>Netty worker thread pool size for handling requests and response.  These threads handle deserializing and
	 * serialising HTTP requests and responses and some other fast logic, long running tasks are done on the action
	 * handler thread pool.</p>
	 *
	 * @param nioEventLoopThreadCount Netty worker thread pool size
	 */
	public ServerConfiguration nioEventLoopThreadCount(final Integer nioEventLoopThreadCount)
	{
		this.nioEventLoopThreadCount = nioEventLoopThreadCount;
		return this;
	}
	
	public Integer actionHandlerThreadCount()
	{
		if(this.actionHandlerThreadCount == null)
		{
			return ServerConfigurationProperties.actionHandlerThreadCount();
		}
		return this.actionHandlerThreadCount;
	}
	
	/**
	 * <p>Number of threads for the action handler thread pool</p>
	 * <p>These threads are used for handling actions such as:</p>
	 *     <ul>
	 *         <li>serialising and writing expectation or proxied responses</li>
	 *         <li>handling response delays in a non-blocking way (i.e. using a scheduler)</li>
	 *         <li>executing class callbacks</li>
	 *         <li>handling method / closure callbacks (using web sockets)</li>
	 *     </ul>
	 * <p>
	 * <p>Default is maximum of 5 or available processors count</p>
	 *
	 * @param actionHandlerThreadCount Netty worker thread pool size
	 */
	public ServerConfiguration actionHandlerThreadCount(final Integer actionHandlerThreadCount)
	{
		this.actionHandlerThreadCount = actionHandlerThreadCount;
		return this;
	}
	
	public Boolean matchersFailFast()
	{
		if(this.matchersFailFast == null)
		{
			return ServerConfigurationProperties.matchersFailFast();
		}
		return this.matchersFailFast;
	}
	
	/**
	 * If true (the default) request matchers will fail on the first non-matching field, if false request matchers will
	 * compare all fields. This is useful to see all mismatching fields in the log event recording that a request
	 * matcher did not match.
	 *
	 * @param matchersFailFast enabled request matchers failing fast
	 */
	public ServerConfiguration matchersFailFast(final Boolean matchersFailFast)
	{
		this.matchersFailFast = matchersFailFast;
		return this;
	}
	
	public Boolean alwaysCloseSocketConnections()
	{
		if(this.alwaysCloseSocketConnections == null)
		{
			return ServerConfigurationProperties.alwaysCloseSocketConnections();
		}
		return this.alwaysCloseSocketConnections;
	}
	
	/**
	 * <p>If true socket connections will always be closed after a response is returned, if false connection is only
	 * closed if request header indicate connection should be closed.</p>
	 * <p>
	 * Default is false
	 *
	 * @param alwaysCloseSocketConnections true socket connections will always be closed after a response is returned
	 */
	public ServerConfiguration alwaysCloseSocketConnections(final Boolean alwaysCloseSocketConnections)
	{
		this.alwaysCloseSocketConnections = alwaysCloseSocketConnections;
		return this;
	}
	
	public String localBoundIP()
	{
		if(this.localBoundIP == null)
		{
			return ServerConfigurationProperties.localBoundIP();
		}
		return this.localBoundIP;
	}
	
	/**
	 * The local IP address to bind to for accepting new socket connections
	 * <p>
	 * Default is 0.0.0.0
	 *
	 * @param localBoundIP local IP address to bind to for accepting new socket connections
	 */
	public ServerConfiguration localBoundIP(final String localBoundIP)
	{
		this.localBoundIP = localBoundIP;
		return this;
	}
	
	public Integer maxInitialLineLength()
	{
		if(this.maxInitialLineLength == null)
		{
			return ServerConfigurationProperties.maxInitialLineLength();
		}
		return this.maxInitialLineLength;
	}
	
	/**
	 * Maximum size of the first line of an HTTP request
	 * <p>
	 * The default is Integer.MAX_VALUE
	 *
	 * @param maxInitialLineLength maximum size of the first line of an HTTP request
	 */
	public ServerConfiguration maxInitialLineLength(final Integer maxInitialLineLength)
	{
		this.maxInitialLineLength = maxInitialLineLength;
		return this;
	}
	
	public Integer maxHeaderSize()
	{
		if(this.maxHeaderSize == null)
		{
			return ServerConfigurationProperties.maxHeaderSize();
		}
		return this.maxHeaderSize;
	}
	
	/**
	 * Maximum size of HTTP request headers
	 * <p>
	 * The default is Integer.MAX_VALUE
	 *
	 * @param maxHeaderSize maximum size of HTTP request headers
	 */
	public ServerConfiguration maxHeaderSize(final Integer maxHeaderSize)
	{
		this.maxHeaderSize = maxHeaderSize;
		return this;
	}
	
	public Integer maxChunkSize()
	{
		if(this.maxChunkSize == null)
		{
			return ServerConfigurationProperties.maxChunkSize();
		}
		return this.maxChunkSize;
	}
	
	/**
	 * Maximum size of HTTP chunks in request or responses
	 * <p>
	 * The default is Integer.MAX_VALUE
	 *
	 * @param maxChunkSize maximum size of HTTP chunks in request or responses
	 */
	public ServerConfiguration maxChunkSize(final Integer maxChunkSize)
	{
		this.maxChunkSize = maxChunkSize;
		return this;
	}
	
	public Boolean useSemicolonAsQueryParameterSeparator()
	{
		if(this.useSemicolonAsQueryParameterSeparator == null)
		{
			return ServerConfigurationProperties.useSemicolonAsQueryParameterSeparator();
		}
		return this.useSemicolonAsQueryParameterSeparator;
	}
	
	/**
	 * If true semicolons are treated as a separator for a query parameter string, if false the semicolon is treated as
	 * a normal character that is part of a query parameter value.
	 * <p>
	 * The default is true
	 *
	 * @param useSemicolonAsQueryParameterSeparator if true semicolons are treated as a separator for a query parameter
	 *                                              string
	 */
	public ServerConfiguration useSemicolonAsQueryParameterSeparator(
		final Boolean useSemicolonAsQueryParameterSeparator)
	{
		this.useSemicolonAsQueryParameterSeparator = useSemicolonAsQueryParameterSeparator;
		return this;
	}
	
	public Boolean assumeAllRequestsAreHttp()
	{
		if(this.assumeAllRequestsAreHttp == null)
		{
			return ServerConfigurationProperties.assumeAllRequestsAreHttp();
		}
		return this.assumeAllRequestsAreHttp;
	}
	
	/**
	 * If false requests are assumed as binary if the method isn't one of "GET", "POST", "PUT", "HEAD", "OPTIONS",
	 * "PATCH", "DELETE", "TRACE" or "CONNECT"
	 * <p>
	 * The default is false
	 *
	 * @param assumeAllRequestsAreHttp if false requests are assumed as binary if the method isn't one of "GET",
	 *                                    "POST",
	 *                                 "PUT", "HEAD", "OPTIONS", "PATCH", "DELETE", "TRACE" or "CONNECT"
	 */
	public ServerConfiguration assumeAllRequestsAreHttp(final Boolean assumeAllRequestsAreHttp)
	{
		this.assumeAllRequestsAreHttp = assumeAllRequestsAreHttp;
		return this;
	}
	
	public BinaryProxyListener binaryProxyListener()
	{
		return this.binaryProxyListener;
	}
	
	/**
	 * Set a software.xdev.mockserver.model.BinaryProxyListener called when binary content is proxied
	 *
	 * @param binaryProxyListener a BinaryProxyListener called when binary content is proxied
	 */
	public ServerConfiguration binaryProxyListener(final BinaryProxyListener binaryProxyListener)
	{
		this.binaryProxyListener = binaryProxyListener;
		return this;
	}
	
	public Boolean enableCORSForAPI()
	{
		if(this.enableCORSForAPI == null)
		{
			return ServerConfigurationProperties.enableCORSForAPI();
		}
		return this.enableCORSForAPI;
	}
	
	/**
	 * Enable CORS for MockServer REST API so that the API can be used for javascript running in browsers, such as
	 * selenium
	 * <p>
	 * The default is false
	 *
	 * @param enableCORSForAPI CORS for MockServer REST API
	 */
	public ServerConfiguration enableCORSForAPI(final Boolean enableCORSForAPI)
	{
		this.enableCORSForAPI = enableCORSForAPI;
		return this;
	}
	
	public Boolean enableCORSForAllResponses()
	{
		if(this.enableCORSForAllResponses == null)
		{
			return ServerConfigurationProperties.enableCORSForAllResponses();
		}
		return this.enableCORSForAllResponses;
	}
	
	/**
	 * Enable CORS for all responses from MockServer, including the REST API and expectation responses
	 * <p>
	 * The default is false
	 *
	 * @param enableCORSForAllResponses CORS for all responses from MockServer
	 */
	public ServerConfiguration enableCORSForAllResponses(final Boolean enableCORSForAllResponses)
	{
		this.enableCORSForAllResponses = enableCORSForAllResponses;
		return this;
	}
	
	public String corsAllowOrigin()
	{
		if(this.corsAllowOrigin == null)
		{
			return ServerConfigurationProperties.corsAllowOrigin();
		}
		return this.corsAllowOrigin;
	}
	
	/**
	 * <p>the value used for CORS in the access-control-allow-origin header.</p>
	 * <p>The default is ""</p>
	 *
	 * @param corsAllowOrigin the value used for CORS in the access-control-allow-methods header
	 */
	public ServerConfiguration corsAllowOrigin(final String corsAllowOrigin)
	{
		this.corsAllowOrigin = corsAllowOrigin;
		return this;
	}
	
	public String corsAllowMethods()
	{
		if(this.corsAllowMethods == null)
		{
			return ServerConfigurationProperties.corsAllowMethods();
		}
		return this.corsAllowMethods;
	}
	
	/**
	 * <p>the value used for CORS in the access-control-allow-methods header.</p>
	 * <p>The default is ""</p>
	 *
	 * @param corsAllowMethods the value used for CORS in the access-control-allow-methods header
	 */
	public ServerConfiguration corsAllowMethods(final String corsAllowMethods)
	{
		this.corsAllowMethods = corsAllowMethods;
		return this;
	}
	
	public String corsAllowHeaders()
	{
		if(this.corsAllowHeaders == null)
		{
			return ServerConfigurationProperties.corsAllowHeaders();
		}
		return this.corsAllowHeaders;
	}
	
	/**
	 * <p>the value used for CORS in the access-control-allow-headers and access-control-expose-headers headers.</p>
	 * <p>In addition to this default value any headers specified in the request header access-control-request-headers
	 * also get added to access-control-allow-headers and access-control-expose-headers headers in a CORS response.</p>
	 * <p>The default is ""</p>
	 *
	 * @param corsAllowHeaders the value used for CORS in the access-control-allow-headers and
	 *                         access-control-expose-headers headers
	 */
	public ServerConfiguration corsAllowHeaders(final String corsAllowHeaders)
	{
		this.corsAllowHeaders = corsAllowHeaders;
		return this;
	}
	
	public Boolean corsAllowCredentials()
	{
		if(this.corsAllowCredentials == null)
		{
			return ServerConfigurationProperties.corsAllowCredentials();
		}
		return this.corsAllowCredentials;
	}
	
	/**
	 * The value used for CORS in the access-control-allow-credentials header.
	 * <p>
	 * The default is false
	 *
	 * @param corsAllowCredentials the value used for CORS in the access-control-allow-credentials header
	 */
	public ServerConfiguration corsAllowCredentials(final Boolean corsAllowCredentials)
	{
		this.corsAllowCredentials = corsAllowCredentials;
		return this;
	}
	
	public Integer corsMaxAgeInSeconds()
	{
		if(this.corsMaxAgeInSeconds == null)
		{
			return ServerConfigurationProperties.corsMaxAgeInSeconds();
		}
		return this.corsMaxAgeInSeconds;
	}
	
	/**
	 * The value used for CORS in the access-control-max-age header.
	 * <p>
	 * The default is 0
	 *
	 * @param corsMaxAgeInSeconds the value used for CORS in the access-control-max-age header.
	 */
	public ServerConfiguration corsMaxAgeInSeconds(final Integer corsMaxAgeInSeconds)
	{
		this.corsMaxAgeInSeconds = corsMaxAgeInSeconds;
		return this;
	}
	
	public Integer maximumNumberOfRequestToReturnInVerificationFailure()
	{
		if(this.maximumNumberOfRequestToReturnInVerificationFailure == null)
		{
			return ServerConfigurationProperties.maximumNumberOfRequestToReturnInVerificationFailure();
		}
		return this.maximumNumberOfRequestToReturnInVerificationFailure;
	}
	
	/**
	 * The maximum number of requests to return in verification failure result, if more expectations are found the
	 * failure result does not list them separately
	 *
	 * @param maximumNumberOfRequestToReturnInVerificationFailure maximum number of expectations to return in
	 *                                                            verification failure result
	 */
	public ServerConfiguration maximumNumberOfRequestToReturnInVerificationFailure(
		final Integer maximumNumberOfRequestToReturnInVerificationFailure)
	{
		this.maximumNumberOfRequestToReturnInVerificationFailure = maximumNumberOfRequestToReturnInVerificationFailure;
		return this;
	}
	
	public String proxyAuthenticationRealm()
	{
		if(this.proxyAuthenticationRealm == null)
		{
			return ServerConfigurationProperties.proxyAuthenticationRealm();
		}
		return this.proxyAuthenticationRealm;
	}
	
	/**
	 * The authentication realm for proxy authentication to MockServer
	 *
	 * @param proxyAuthenticationRealm the authentication realm for proxy authentication
	 */
	public ServerConfiguration proxyAuthenticationRealm(final String proxyAuthenticationRealm)
	{
		this.proxyAuthenticationRealm = proxyAuthenticationRealm;
		return this;
	}
	
	public String proxyAuthenticationUsername()
	{
		if(this.proxyAuthenticationUsername == null)
		{
			return ServerConfigurationProperties.proxyAuthenticationUsername();
		}
		return this.proxyAuthenticationUsername;
	}
	
	/**
	 * <p>The required username for proxy authentication to MockServer</p>
	 * <p><strong>Note:</strong> <a target="_blank"
	 * href="https://www.oracle.com/java/technologies/javase/8u111-relnotes.html">8u111 Update Release Notes</a> state
	 * that the Basic authentication scheme has been deactivated when setting up an HTTPS tunnel.  To resolve this
	 * clear
	 * or set to an empty string the following system properties: <code class="inline
	 * code">jdk.http.auth.tunneling.disabledSchemes</code> and <code class="inline
	 * code">jdk.http.auth.proxying.disabledSchemes</code>.</p>
	 * <p>
	 * The default is ""
	 *
	 * @param proxyAuthenticationUsername required username for proxy authentication to MockServer
	 */
	public ServerConfiguration proxyAuthenticationUsername(final String proxyAuthenticationUsername)
	{
		this.proxyAuthenticationUsername = proxyAuthenticationUsername;
		return this;
	}
	
	public String proxyAuthenticationPassword()
	{
		if(this.proxyAuthenticationPassword == null)
		{
			return ServerConfigurationProperties.proxyAuthenticationPassword();
		}
		return this.proxyAuthenticationPassword;
	}
	
	/**
	 * <p>The required password for proxy authentication to MockServer</p>
	 * <p><strong>Note:</strong> <a target="_blank"
	 * href="https://www.oracle.com/java/technologies/javase/8u111-relnotes.html">8u111 Update Release Notes</a> state
	 * that the Basic authentication scheme has been deactivated when setting up an HTTPS tunnel.  To resolve this
	 * clear
	 * or set to an empty string the following system properties: <code class="inline
	 * code">jdk.http.auth.tunneling.disabledSchemes</code> and <code class="inline
	 * code">jdk.http.auth.proxying.disabledSchemes</code>.</p>
	 * <p>
	 * The default is ""
	 *
	 * @param proxyAuthenticationPassword required password for proxy authentication to MockServer
	 */
	public ServerConfiguration proxyAuthenticationPassword(final String proxyAuthenticationPassword)
	{
		this.proxyAuthenticationPassword = proxyAuthenticationPassword;
		return this;
	}
	
	public String noProxyHosts()
	{
		if(this.noProxyHosts == null)
		{
			return ServerConfigurationProperties.noProxyHosts();
		}
		return this.noProxyHosts;
	}
	
	/**
	 * <p>The list of hostnames to not use the configured proxy. Several values may be present, seperated by comma
	 * (,)</p> The default is ""
	 *
	 * @param noProxyHosts Comma-seperated list of hosts to not be proxied.
	 */
	public ServerConfiguration noProxyHosts(final String noProxyHosts)
	{
		this.noProxyHosts = noProxyHosts;
		return this;
	}
	
	public String livenessHttpGetPath()
	{
		if(this.livenessHttpGetPath == null)
		{
			return ServerConfigurationProperties.livenessHttpGetPath();
		}
		return this.livenessHttpGetPath;
	}
	
	/**
	 * Path to support HTTP GET requests for status response (also available on PUT /mockserver/status).
	 * <p>
	 * If this value is not modified then only PUT /mockserver/status but is a none blank value is provided for this
	 * value then GET requests to this path will return the 200 Ok status response showing the MockServer version and
	 * bound ports.
	 * <p>
	 * A GET request to this path will be matched before any expectation matching or proxying of requests.
	 * <p>
	 * The default is ""
	 *
	 * @param livenessHttpGetPath path to support HTTP GET requests for status response
	 */
	public ServerConfiguration livenessHttpGetPath(final String livenessHttpGetPath)
	{
		this.livenessHttpGetPath = livenessHttpGetPath;
		return this;
	}
	
	public int ringBufferSize()
	{
		return this.nextPowerOfTwo(this.maxLogEntries());
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	private int nextPowerOfTwo(final int value)
	{
		for(int i = 0; i < 16; i++)
		{
			final double powOfTwo = Math.pow(2, i);
			if(powOfTwo > value)
			{
				return (int)powOfTwo;
			}
		}
		return (int)Math.pow(2, 16);
	}
}
