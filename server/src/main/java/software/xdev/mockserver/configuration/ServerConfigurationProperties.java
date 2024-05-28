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

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.logging.MockServerLoggerConfiguration.configureLogger;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;


@SuppressWarnings("checkstyle:MagicNumber")
public class ServerConfigurationProperties extends ConfigurationProperties
{
	private static final Logger LOG = LoggerFactory.getLogger(ServerConfigurationProperties.class);
	
	private static final String DEFAULT_LOG_LEVEL = "INFO";
	
	// logging
	private static final String MOCKSERVER_LOG_LEVEL = "mockserver.logLevel";
	private static final String MOCKSERVER_DISABLE_SYSTEM_OUT = "mockserver.disableSystemOut";
	private static final String MOCKSERVER_DISABLE_LOGGING = "mockserver.disableLogging";
	private static final String MOCKSERVER_DETAILED_MATCH_FAILURES = "mockserver.detailedMatchFailures";
	
	// memory usage
	private static final String MOCKSERVER_MAX_EXPECTATIONS = "mockserver.maxExpectations";
	private static final String MOCKSERVER_MAX_LOG_ENTRIES = "mockserver.maxLogEntries";
	
	// scalability
	private static final String MOCKSERVER_NIO_EVENT_LOOP_THREAD_COUNT = "mockserver.nioEventLoopThreadCount";
	private static final String MOCKSERVER_ACTION_HANDLER_THREAD_COUNT = "mockserver.actionHandlerThreadCount";
	private static final String MOCKSERVER_MATCHERS_FAIL_FAST = "mockserver.matchersFailFast";
	
	// socket
	private static final String MOCKSERVER_ALWAYS_CLOSE_SOCKET_CONNECTIONS = "mockserver.alwaysCloseSocketConnections";
	private static final String MOCKSERVER_LOCAL_BOUND_IP = "mockserver.localBoundIP";
	
	// http request parsing
	private static final String MOCKSERVER_MAX_INITIAL_LINE_LENGTH = "mockserver.maxInitialLineLength";
	private static final String MOCKSERVER_MAX_HEADER_SIZE = "mockserver.maxHeaderSize";
	private static final String MOCKSERVER_MAX_CHUNK_SIZE = "mockserver.maxChunkSize";
	private static final String MOCKSERVER_USE_SEMICOLON_AS_QUERY_PARAMETER_SEPARATOR =
		"mockserver.useSemicolonAsQueryParameterSeparator";
	private static final String MOCKSERVER_ASSUME_ALL_REQUESTS_ARE_HTTP = "mockserver.assumeAllRequestsAreHttp";
	
	// CORS
	private static final String MOCKSERVER_ENABLE_CORS_FOR_API = "mockserver.enableCORSForAPI";
	private static final String MOCKSERVER_ENABLE_CORS_FOR_ALL_RESPONSES = "mockserver.enableCORSForAllResponses";
	private static final String MOCKSERVER_CORS_ALLOW_ORIGIN = "mockserver.corsAllowOrigin";
	private static final String MOCKSERVER_CORS_ALLOW_METHODS = "mockserver.corsAllowMethods";
	private static final String MOCKSERVER_CORS_ALLOW_HEADERS = "mockserver.corsAllowHeaders";
	private static final String MOCKSERVER_CORS_ALLOW_CREDENTIALS = "mockserver.corsAllowCredentials";
	private static final String MOCKSERVER_CORS_MAX_AGE_IN_SECONDS = "mockserver.corsMaxAgeInSeconds";
	
	// verification
	private static final String MOCKSERVER_MAXIMUM_NUMBER_OF_REQUESTS_TO_RETURN_IN_VERIFICATION_FAILURE =
		"mockserver.maximumNumberOfRequestToReturnInVerificationFailure";
	
	// proxy
	private static final String MOCKSERVER_PROXY_SERVER_REALM = "mockserver.proxyAuthenticationRealm";
	private static final String MOCKSERVER_PROXY_AUTHENTICATION_USERNAME = "mockserver.proxyAuthenticationUsername";
	private static final String MOCKSERVER_PROXY_AUTHENTICATION_PASSWORD = "mockserver.proxyAuthenticationPassword";
	private static final String MOCKSERVER_NO_PROXY_HOSTS = "mockserver.noProxyHosts";
	
	// liveness
	private static final String MOCKSERVER_LIVENESS_HTTP_GET_PATH = "mockserver.livenessHttpGetPath";
	
	// properties file
	private static final String MOCKSERVER_PROPERTY_FILE = "mockserver.propertyFile";
	
	static
	{
		properties = readPropertyFile();
	}
	
	private static Map<String, String> slf4jOrJavaLoggerToJavaLoggerLevelMapping;
	
	private static Map<String, String> slf4jOrJavaLoggerToSLF4JLevelMapping;
	
	private static Map<String, String> getSLF4JOrJavaLoggerToJavaLoggerLevelMapping()
	{
		if(slf4jOrJavaLoggerToJavaLoggerLevelMapping == null)
		{
			slf4jOrJavaLoggerToJavaLoggerLevelMapping = Map.ofEntries(
				Map.entry("TRACE", "FINEST"),
				Map.entry("DEBUG", "FINE"),
				Map.entry("INFO", "INFO"),
				Map.entry("WARN", "WARNING"),
				Map.entry("ERROR", "SEVERE"),
				Map.entry("FINEST", "FINEST"),
				Map.entry("FINE", "FINE"),
				Map.entry("WARNING", "WARNING"),
				Map.entry("SEVERE", "SEVERE"),
				Map.entry("OFF", "OFF")
			);
		}
		return slf4jOrJavaLoggerToJavaLoggerLevelMapping;
	}
	
	private static Map<String, String> getSLF4JOrJavaLoggerToSLF4JLevelMapping()
	{
		if(slf4jOrJavaLoggerToSLF4JLevelMapping == null)
		{
			slf4jOrJavaLoggerToSLF4JLevelMapping = Map.ofEntries(
				Map.entry("FINEST", "TRACE"),
				Map.entry("FINE", "DEBUG"),
				Map.entry("INFO", "INFO"),
				Map.entry("WARNING", "WARN"),
				Map.entry("SEVERE", "ERROR"),
				Map.entry("TRACE", "TRACE"),
				Map.entry("DEBUG", "DEBUG"),
				Map.entry("WARN", "WARN"),
				Map.entry("ERROR", "ERROR"),
				Map.entry("OFF", "ERROR")
			);
		}
		return slf4jOrJavaLoggerToSLF4JLevelMapping;
	}
	
	private static String propertyFile()
	{
		if(isNotBlank(System.getProperty(MOCKSERVER_PROPERTY_FILE)) && System.getProperty(MOCKSERVER_PROPERTY_FILE)
			.equals("/config/mockserver.properties"))
		{
			return isBlank(System.getenv("MOCKSERVER_PROPERTY_FILE"))
				? System.getProperty(MOCKSERVER_PROPERTY_FILE)
				: System.getenv("MOCKSERVER_PROPERTY_FILE");
		}
		else
		{
			return System.getProperty(
				MOCKSERVER_PROPERTY_FILE,
				isBlank(System.getenv("MOCKSERVER_PROPERTY_FILE"))
					? "mockserver.properties"
					: System.getenv("MOCKSERVER_PROPERTY_FILE"));
		}
	}
	
	// logging
	
	public static Level logLevel()
	{
		final String logLevel = readPropertyHierarchically(
			properties,
			MOCKSERVER_LOG_LEVEL,
			"MOCKSERVER_LOG_LEVEL",
			DEFAULT_LOG_LEVEL).toUpperCase();
		if(isNotBlank(logLevel))
		{
			if(getSLF4JOrJavaLoggerToSLF4JLevelMapping().get(logLevel).equals("OFF"))
			{
				return null;
			}
			else
			{
				return Level.valueOf(getSLF4JOrJavaLoggerToSLF4JLevelMapping().get(logLevel));
			}
		}
		else
		{
			return Level.INFO;
		}
	}
	
	public static String javaLoggerLogLevel()
	{
		final String logLevel = readPropertyHierarchically(
			properties,
			MOCKSERVER_LOG_LEVEL,
			"MOCKSERVER_LOG_LEVEL",
			DEFAULT_LOG_LEVEL).toUpperCase();
		if(isNotBlank(logLevel))
		{
			if(getSLF4JOrJavaLoggerToJavaLoggerLevelMapping().get(logLevel).equals("OFF"))
			{
				return "OFF";
			}
			else
			{
				return getSLF4JOrJavaLoggerToJavaLoggerLevelMapping().get(logLevel);
			}
		}
		else
		{
			return "INFO";
		}
	}
	
	/**
	 * Override the default logging level of INFO
	 *
	 * @param level the log level, which can be TRACE, DEBUG, INFO, WARN, ERROR, OFF, FINEST, FINE, INFO, WARNING,
	 *              SEVERE
	 */
	public static void logLevel(final String level)
	{
		if(isNotBlank(level))
		{
			if(!getSLF4JOrJavaLoggerToSLF4JLevelMapping().containsKey(level))
			{
				throw new IllegalArgumentException("log level \"" + level
					+ "\" is not legal it must be one of SL4J levels: \"TRACE\", \"DEBUG\", \"INFO\", \"WARN\", "
					+ "\"ERROR\", \"OFF\", or the Java Logger levels: \"FINEST\", \"FINE\", \"INFO\", \"WARNING\", "
					+ "\"SEVERE\", \"OFF\"");
			}
			setProperty(MOCKSERVER_LOG_LEVEL, level);
		}
		configureLogger();
	}
	
	public static void temporaryLogLevel(final String level, final Runnable runnable)
	{
		final Level originalLogLevel = logLevel();
		try
		{
			logLevel(level);
			runnable.run();
		}
		finally
		{
			if(originalLogLevel != null)
			{
				logLevel(originalLogLevel.name());
			}
		}
	}
	
	public static boolean disableSystemOut()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_DISABLE_SYSTEM_OUT,
			"MOCKSERVER_DISABLE_SYSTEM_OUT",
			"" + false));
	}
	
	/**
	 * Disable printing log to system out for JVM, default is enabled
	 *
	 * @param disable printing log to system out for JVM
	 */
	public static void disableSystemOut(final boolean disable)
	{
		setProperty(MOCKSERVER_DISABLE_SYSTEM_OUT, "" + disable);
		configureLogger();
	}
	
	public static boolean disableLogging()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_DISABLE_LOGGING,
			"MOCKSERVER_DISABLE_LOGGING",
			"" + false));
	}
	
	/**
	 * Disable all logging and processing of log events
	 * <p>
	 * The default is false
	 *
	 * @param disable disable all logging
	 */
	public static void disableLogging(final boolean disable)
	{
		setProperty(MOCKSERVER_DISABLE_LOGGING, "" + disable);
		configureLogger();
	}
	
	public static boolean detailedMatchFailures()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_DETAILED_MATCH_FAILURES,
			"MOCKSERVER_DETAILED_MATCH_FAILURES",
			"" + true));
	}
	
	/**
	 * If true (the default) the log event recording that a request matcher did not match will include a detailed
	 * reason
	 * why each non matching field did not match.
	 *
	 * @param enable enabled detailed match failure log events
	 */
	public static void detailedMatchFailures(final boolean enable)
	{
		setProperty(MOCKSERVER_DETAILED_MATCH_FAILURES, "" + enable);
	}
	
	public static int maxExpectations()
	{
		return readIntegerProperty(MOCKSERVER_MAX_EXPECTATIONS, "MOCKSERVER_MAX_EXPECTATIONS", 5000);
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
	 * @param count maximum number of expectations to store
	 */
	public static void maxExpectations(final int count)
	{
		setProperty(MOCKSERVER_MAX_EXPECTATIONS, "" + count);
	}
	
	public static int maxLogEntries()
	{
		return readIntegerProperty(MOCKSERVER_MAX_LOG_ENTRIES, "MOCKSERVER_MAX_LOG_ENTRIES", 60000);
	}
	
	/**
	 * <p>
	 * Maximum number of log entries stored in memory.  Log entries are stored in a circular queue so once this
	 * limit is
	 * reach the oldest log entries are overwritten.
	 * </p>
	 * <p>
	 * The default maximum depends on the available memory in the JVM with an upper limit of 60000, but can be
	 * overridden using defaultMaxLogEntries
	 * </p>
	 *
	 * @param count maximum number of expectations to store
	 */
	public static void maxLogEntries(final int count)
	{
		setProperty(MOCKSERVER_MAX_LOG_ENTRIES, "" + count);
	}
	
	// scalability
	
	public static int nioEventLoopThreadCount()
	{
		return readIntegerProperty(MOCKSERVER_NIO_EVENT_LOOP_THREAD_COUNT, "MOCKSERVER_NIO_EVENT_LOOP_THREAD_COUNT",
			5);
	}
	
	/**
	 * <p>Netty worker thread pool size for handling requests and response.  These threads are used for fast
	 * non-blocking activities such as, reading and de-serialise all requests and responses.</p>
	 *
	 * @param count Netty worker thread pool size
	 */
	public static void nioEventLoopThreadCount(final int count)
	{
		setProperty(MOCKSERVER_NIO_EVENT_LOOP_THREAD_COUNT, "" + count);
	}
	
	public static int actionHandlerThreadCount()
	{
		return readIntegerProperty(
			MOCKSERVER_ACTION_HANDLER_THREAD_COUNT,
			"MOCKSERVER_ACTION_HANDLER_THREAD_COUNT",
			Math.max(5, Runtime.getRuntime().availableProcessors()));
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
	 * @param count Netty worker thread pool size
	 */
	public static void actionHandlerThreadCount(final int count)
	{
		setProperty(MOCKSERVER_ACTION_HANDLER_THREAD_COUNT, "" + count);
	}
	
	public static boolean matchersFailFast()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_MATCHERS_FAIL_FAST,
			"MOCKSERVER_MATCHERS_FAIL_FAST",
			"" + true));
	}
	
	/**
	 * If true (the default) request matchers will fail on the first non-matching field, if false request matchers will
	 * compare all fields. This is useful to see all mismatching fields in the log event recording that a request
	 * matcher did not match.
	 *
	 * @param enable enabled request matchers failing fast
	 */
	public static void matchersFailFast(final boolean enable)
	{
		setProperty(MOCKSERVER_MATCHERS_FAIL_FAST, "" + enable);
	}
	
	// socket
	
	/**
	 * <p>If true socket connections will always be closed after a response is returned, if false connection is only
	 * closed if request header indicate connection should be closed.</p>
	 * <p>
	 * Default is false
	 *
	 * @param alwaysClose true socket connections will always be closed after a response is returned
	 */
	public static void alwaysCloseSocketConnections(final boolean alwaysClose)
	{
		setProperty(MOCKSERVER_ALWAYS_CLOSE_SOCKET_CONNECTIONS, "" + alwaysClose);
	}
	
	public static boolean alwaysCloseSocketConnections()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_ALWAYS_CLOSE_SOCKET_CONNECTIONS,
			"MOCKSERVER_ALWAYS_CLOSE_SOCKET_CONNECTIONS",
			"false"));
	}
	
	public static String localBoundIP()
	{
		return readPropertyHierarchically(properties, MOCKSERVER_LOCAL_BOUND_IP, "MOCKSERVER_LOCAL_BOUND_IP", "");
	}
	
	/**
	 * The local IP address to bind to for accepting new socket connections
	 * <p>
	 * Default is 0.0.0.0
	 *
	 * @param localBoundIP local IP address to bind to for accepting new socket connections
	 */
	public static void localBoundIP(final String localBoundIP)
	{
		if(isNotBlank(localBoundIP))
		{
			setProperty(MOCKSERVER_LOCAL_BOUND_IP, localBoundIP);
		}
	}
	
	// http request parsing
	
	public static int maxInitialLineLength()
	{
		return readIntegerProperty(
			MOCKSERVER_MAX_INITIAL_LINE_LENGTH,
			"MOCKSERVER_MAX_INITIAL_LINE_LENGTH",
			Integer.MAX_VALUE);
	}
	
	/**
	 * Maximum size of the first line of an HTTP request
	 * <p>
	 * The default is Integer.MAX_VALUE
	 *
	 * @param length maximum size of the first line of an HTTP request
	 */
	public static void maxInitialLineLength(final int length)
	{
		setProperty(MOCKSERVER_MAX_INITIAL_LINE_LENGTH, "" + length);
	}
	
	public static int maxHeaderSize()
	{
		return readIntegerProperty(MOCKSERVER_MAX_HEADER_SIZE, "MOCKSERVER_MAX_HEADER_SIZE", Integer.MAX_VALUE);
	}
	
	/**
	 * Maximum size of HTTP request headers
	 * <p>
	 * The default is Integer.MAX_VALUE
	 *
	 * @param size maximum size of HTTP request headers
	 */
	public static void maxHeaderSize(final int size)
	{
		setProperty(MOCKSERVER_MAX_HEADER_SIZE, "" + size);
	}
	
	public static int maxChunkSize()
	{
		return readIntegerProperty(MOCKSERVER_MAX_CHUNK_SIZE, "MOCKSERVER_MAX_CHUNK_SIZE", Integer.MAX_VALUE);
	}
	
	/**
	 * Maximum size of HTTP chunks in request or responses
	 * <p>
	 * The default is Integer.MAX_VALUE
	 *
	 * @param size maximum size of HTTP chunks in request or responses
	 */
	public static void maxChunkSize(final int size)
	{
		setProperty(MOCKSERVER_MAX_CHUNK_SIZE, "" + size);
	}
	
	/**
	 * If true semicolons are treated as a separator for a query parameter string, if false the semicolon is treated as
	 * a normal character that is part of a query parameter value.
	 * <p>
	 * The default is true
	 *
	 * @param useAsQueryParameterSeparator true semicolons are treated as a separator for a query parameter string
	 */
	public static void useSemicolonAsQueryParameterSeparator(final boolean useAsQueryParameterSeparator)
	{
		setProperty(MOCKSERVER_USE_SEMICOLON_AS_QUERY_PARAMETER_SEPARATOR, "" + useAsQueryParameterSeparator);
	}
	
	public static boolean useSemicolonAsQueryParameterSeparator()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_USE_SEMICOLON_AS_QUERY_PARAMETER_SEPARATOR,
			"MOCKSERVER_USE_SEMICOLON_AS_QUERY_PARAMETER_SEPARATOR",
			"true"));
	}
	
	/**
	 * If true requests are assumed as binary if the method isn't one of "GET", "POST", "PUT", "HEAD", "OPTIONS",
	 * "PATCH", "DELETE", "TRACE" or "CONNECT"
	 * <p>
	 * The default is true
	 *
	 * @param assumeAllRequestsAreHttp if true requests are assumed as binary if the method isn't one of "GET", "POST",
	 *                                 "PUT", "HEAD", "OPTIONS", "PATCH", "DELETE", "TRACE" or "CONNECT"
	 */
	public static void assumeAllRequestsAreHttp(final boolean assumeAllRequestsAreHttp)
	{
		setProperty(MOCKSERVER_ASSUME_ALL_REQUESTS_ARE_HTTP, "" + assumeAllRequestsAreHttp);
	}
	
	public static boolean assumeAllRequestsAreHttp()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_ASSUME_ALL_REQUESTS_ARE_HTTP,
			"MOCKSERVER_ASSUME_ALL_REQUESTS_ARE_HTTP",
			"false"));
	}
	
	// CORS
	
	public static boolean enableCORSForAPI()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_ENABLE_CORS_FOR_API,
			"MOCKSERVER_ENABLE_CORS_FOR_API",
			"false"));
	}
	
	/**
	 * Enable CORS for MockServer REST API so that the API can be used for javascript running in browsers, such as
	 * selenium
	 * <p>
	 * The default is false
	 *
	 * @param enable CORS for MockServer REST API
	 */
	public static void enableCORSForAPI(final boolean enable)
	{
		setProperty(MOCKSERVER_ENABLE_CORS_FOR_API, "" + enable);
	}
	
	public static boolean enableCORSForAllResponses()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_ENABLE_CORS_FOR_ALL_RESPONSES,
			"MOCKSERVER_ENABLE_CORS_FOR_ALL_RESPONSES",
			"false"));
	}
	
	/**
	 * Enable CORS for all responses from MockServer, including the REST API and expectation responses
	 * <p>
	 * The default is false
	 *
	 * @param enable CORS for all responses from MockServer
	 */
	public static void enableCORSForAllResponses(final boolean enable)
	{
		setProperty(MOCKSERVER_ENABLE_CORS_FOR_ALL_RESPONSES, "" + enable);
	}
	
	public static String corsAllowOrigin()
	{
		return readPropertyHierarchically(properties, MOCKSERVER_CORS_ALLOW_ORIGIN, "MOCKSERVER_CORS_ALLOW_ORIGIN",
			"");
	}
	
	/**
	 * <p>the value used for CORS in the access-control-allow-origin header.</p>
	 * <p>The default is ""</p>
	 *
	 * @param corsAllowOrigin the value used for CORS in the access-control-allow-methods header
	 */
	public static void corsAllowOrigin(final String corsAllowOrigin)
	{
		setProperty(MOCKSERVER_CORS_ALLOW_ORIGIN, corsAllowOrigin);
	}
	
	public static String corsAllowMethods()
	{
		return readPropertyHierarchically(
			properties,
			MOCKSERVER_CORS_ALLOW_METHODS,
			"MOCKSERVER_CORS_ALLOW_METHODS",
			"");
	}
	
	/**
	 * <p>The value used for CORS in the access-control-allow-methods header.</p>
	 * <p>The default is "CONNECT, DELETE, GET, HEAD, OPTIONS, POST, PUT, PATCH, TRACE"</p>
	 *
	 * @param corsAllowMethods the value used for CORS in the access-control-allow-methods header
	 */
	public static void corsAllowMethods(final String corsAllowMethods)
	{
		setProperty(MOCKSERVER_CORS_ALLOW_METHODS, corsAllowMethods);
	}
	
	public static String corsAllowHeaders()
	{
		return readPropertyHierarchically(
			properties,
			MOCKSERVER_CORS_ALLOW_HEADERS,
			"MOCKSERVER_CORS_ALLOW_HEADERS",
			"");
	}
	
	/**
	 * <p>the value used for CORS in the access-control-allow-headers and access-control-expose-headers headers.</p>
	 * <p>In addition to this default value any headers specified in the request header access-control-request-headers
	 * also get added to access-control-allow-headers and access-control-expose-headers headers in a CORS response.</p>
	 * <p>The default is "Allow, Content-Encoding, Content-Length, Content-Type, ETag, Expires, Last-Modified,
	 * Location, Server, Vary, Authorization"</p>
	 *
	 * @param corsAllowHeaders the value used for CORS in the access-control-allow-headers and
	 *                         access-control-expose-headers headers
	 */
	public static void corsAllowHeaders(final String corsAllowHeaders)
	{
		setProperty(MOCKSERVER_CORS_ALLOW_HEADERS, corsAllowHeaders);
	}
	
	public static boolean corsAllowCredentials()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_CORS_ALLOW_CREDENTIALS,
			"MOCKSERVER_CORS_ALLOW_CREDENTIALS",
			"false"));
	}
	
	/**
	 * The value used for CORS in the access-control-allow-credentials header.
	 * <p>
	 * The default is true
	 *
	 * @param allow the value used for CORS in the access-control-allow-credentials header
	 */
	public static void corsAllowCredentials(final boolean allow)
	{
		setProperty(MOCKSERVER_CORS_ALLOW_CREDENTIALS, "" + allow);
	}
	
	public static int corsMaxAgeInSeconds()
	{
		return readIntegerProperty(MOCKSERVER_CORS_MAX_AGE_IN_SECONDS, "MOCKSERVER_CORS_MAX_AGE_IN_SECONDS", 0);
	}
	
	/**
	 * The value used for CORS in the access-control-max-age header.
	 * <p>
	 * The default is 300
	 *
	 * @param ageInSeconds the value used for CORS in the access-control-max-age header.
	 */
	public static void corsMaxAgeInSeconds(final int ageInSeconds)
	{
		setProperty(MOCKSERVER_CORS_MAX_AGE_IN_SECONDS, "" + ageInSeconds);
	}
	
	// verification
	
	public static Integer maximumNumberOfRequestToReturnInVerificationFailure()
	{
		return readIntegerProperty(
			MOCKSERVER_MAXIMUM_NUMBER_OF_REQUESTS_TO_RETURN_IN_VERIFICATION_FAILURE,
			"MOCKSERVER_MAXIMUM_NUMBER_OF_REQUESTS_TO_RETURN_IN_VERIFICATION_FAILURE",
			10);
	}
	
	/**
	 * The maximum number of requests to return in verification failure result, if more expectations are found the
	 * failure result does not list them separately
	 *
	 * @param maximumNumberOfRequestToReturnInVerification maximum number of expectations to return in verification
	 *                                                     failure result
	 */
	public static void maximumNumberOfRequestToReturnInVerificationFailure(
		final Integer maximumNumberOfRequestToReturnInVerification)
	{
		setProperty(
			MOCKSERVER_MAXIMUM_NUMBER_OF_REQUESTS_TO_RETURN_IN_VERIFICATION_FAILURE,
			"" + maximumNumberOfRequestToReturnInVerification);
	}
	
	// proxy
	public static String proxyAuthenticationRealm()
	{
		return readPropertyHierarchically(
			properties,
			MOCKSERVER_PROXY_SERVER_REALM,
			"MOCKSERVER_PROXY_SERVER_REALM",
			"MockServer HTTP Proxy");
	}
	
	/**
	 * The authentication realm for proxy authentication to MockServer
	 *
	 * @param proxyAuthenticationRealm the authentication realm for proxy authentication
	 */
	public static void proxyAuthenticationRealm(final String proxyAuthenticationRealm)
	{
		setProperty(MOCKSERVER_PROXY_SERVER_REALM, proxyAuthenticationRealm);
	}
	
	public static String proxyAuthenticationUsername()
	{
		return readPropertyHierarchically(
			properties,
			MOCKSERVER_PROXY_AUTHENTICATION_USERNAME,
			"MOCKSERVER_PROXY_AUTHENTICATION_USERNAME",
			"");
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
	public static void proxyAuthenticationUsername(final String proxyAuthenticationUsername)
	{
		setProperty(MOCKSERVER_PROXY_AUTHENTICATION_USERNAME, proxyAuthenticationUsername);
	}
	
	public static String proxyAuthenticationPassword()
	{
		return readPropertyHierarchically(
			properties,
			MOCKSERVER_PROXY_AUTHENTICATION_PASSWORD,
			"MOCKSERVER_PROXY_AUTHENTICATION_PASSWORD",
			"");
	}
	
	/**
	 * <p>The list of hostnames to not use the configured proxy. Several values may be present, seperated by comma
	 * (,)</p> The default is ""
	 *
	 * @param noProxyHosts Comma-seperated list of hosts to not be proxied.
	 */
	public static void noProxyHosts(final String noProxyHosts)
	{
		setProperty(MOCKSERVER_NO_PROXY_HOSTS, noProxyHosts);
	}
	
	public static String noProxyHosts()
	{
		return readPropertyHierarchically(properties, MOCKSERVER_NO_PROXY_HOSTS, "MOCKSERVER_NO_PROXY_HOSTS", "");
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
	public static void proxyAuthenticationPassword(final String proxyAuthenticationPassword)
	{
		setProperty(MOCKSERVER_PROXY_AUTHENTICATION_PASSWORD, proxyAuthenticationPassword);
	}
	
	// liveness
	
	public static String livenessHttpGetPath()
	{
		return readPropertyHierarchically(
			properties,
			MOCKSERVER_LIVENESS_HTTP_GET_PATH,
			"MOCKSERVER_LIVENESS_HTTP_GET_PATH",
			"");
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
	 * @param livenessPath path to support HTTP GET requests for status response
	 */
	public static void livenessHttpGetPath(final String livenessPath)
	{
		setProperty(MOCKSERVER_LIVENESS_HTTP_GET_PATH, livenessPath);
	}
	
	@SuppressWarnings("ConstantConditions")
	private static Properties readPropertyFile()
	{
		final Properties properties = new Properties();
		
		try(final InputStream inputStream = ServerConfigurationProperties.class.getClassLoader()
			.getResourceAsStream(propertyFile()))
		{
			if(inputStream != null)
			{
				try
				{
					properties.load(inputStream);
				}
				catch(final IOException e)
				{
					LOG.error("Exception loading property file [{}]", propertyFile(), e);
				}
			}
			else
			{
				if(LOG.isDebugEnabled())
				{
					LOG.debug("Property file not found on classpath using path [{}]", propertyFile());
				}
				try(final FileInputStream fis = new FileInputStream(propertyFile()))
				{
					properties.load(fis);
				}
				catch(final FileNotFoundException e)
				{
					if(LOG.isDebugEnabled())
					{
						LOG.debug("Property file not found using path [{}]", propertyFile(), e);
					}
				}
				catch(final IOException e)
				{
					LOG.error("Exception loading property file [{}]", propertyFile(), e);
				}
			}
		}
		catch(final IOException ioe)
		{
			// ignore
		}
		
		if(!properties.isEmpty())
		{
			final Enumeration<?> propertyNames = properties.propertyNames();
			
			final StringBuilder propertiesLogDump = new StringBuilder();
			propertiesLogDump.append("Reading properties from property file [")
				.append(propertyFile())
				.append("]:")
				.append(NEW_LINE);
			while(propertyNames.hasMoreElements())
			{
				final String propertyName = String.valueOf(propertyNames.nextElement());
				propertiesLogDump.append("  ")
					.append(propertyName)
					.append(" = ")
					.append(properties.getProperty(propertyName))
					.append(NEW_LINE);
			}
			
			final Level logLevel =
				Level.valueOf(getSLF4JOrJavaLoggerToSLF4JLevelMapping().get(readPropertyHierarchically(
					properties,
					MOCKSERVER_LOG_LEVEL,
					"MOCKSERVER_LOG_LEVEL",
					DEFAULT_LOG_LEVEL).toUpperCase()));
			if(LOG.isEnabledForLevel(logLevel))
			{
				LOG.info(propertiesLogDump.toString());
			}
		}
		
		return properties;
	}
}
