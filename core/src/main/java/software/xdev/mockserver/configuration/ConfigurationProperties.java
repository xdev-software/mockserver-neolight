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

import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.util.StringUtils;


@SuppressWarnings({"checkstyle:MagicNumber", "PMD.UseUtilityClass", "PMD.AvoidSystemSetterCall"})
public class ConfigurationProperties
{
	protected static final Logger LOG = LoggerFactory.getLogger(ConfigurationProperties.class);
	
	protected static final Pattern UNESCAPE_QUOTES_PATTERN = Pattern.compile("(^\")|(\"$)");
	
	protected static final String MOCKSERVER_MAX_WEB_SOCKET_EXPECTATIONS = "mockserver.maxWebSocketExpectations";
	
	// scalability
	protected static final String MOCKSERVER_CLIENT_NIO_EVENT_LOOP_THREAD_COUNT =
		"mockserver.clientNioEventLoopThreadCount";
	protected static final String MOCKSERVER_WEB_SOCKET_CLIENT_EVENT_LOOP_THREAD_COUNT =
		"mockserver.webSocketClientEventLoopThreadCount";
	protected static final String MOCKSERVER_MAX_FUTURE_TIMEOUT = "mockserver.maxFutureTimeout";
	
	// socket
	protected static final String MOCKSERVER_MAX_SOCKET_TIMEOUT = "mockserver.maxSocketTimeout";
	protected static final String MOCKSERVER_SOCKET_CONNECTION_TIMEOUT = "mockserver.socketConnectionTimeout";
	
	// non http proxying
	private static final String MOCKSERVER_FORWARD_BINARY_REQUESTS_WITHOUT_WAITING_FOR_RESPONSE =
		"mockserver.forwardBinaryRequestsWithoutWaitingForResponse";
	
	// proxy
	protected static final String MOCKSERVER_ATTEMPT_TO_PROXY_IF_NO_MATCHING_EXPECTATION =
		"mockserver.attemptToProxyIfNoMatchingExpectation";
	protected static final String MOCKSERVER_FORWARD_HTTP_PROXY = "mockserver.forwardHttpProxy";
	protected static final String MOCKSERVER_FORWARD_SOCKS_PROXY = "mockserver.forwardSocksProxy";
	protected static final String MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_USERNAME =
		"mockserver.forwardProxyAuthenticationUsername";
	protected static final String MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_PASSWORD =
		"mockserver.forwardProxyAuthenticationPassword";
	
	// properties file
	@SuppressWarnings("checkstyle:VisibilityModifier")
	public static Properties properties = new Properties();
	
	protected ConfigurationProperties()
	{
	}
	
	public static int maxWebSocketExpectations()
	{
		return readIntegerProperty(
			MOCKSERVER_MAX_WEB_SOCKET_EXPECTATIONS,
			"MOCKSERVER_MAX_WEB_SOCKET_EXPECTATIONS",
			1500);
	}
	
	/**
	 * <p>
	 * Maximum number of remote (not the same JVM) method callbacks (i.e. web sockets) registered for expectations. The
	 * web socket client registry entries are stored in a circular queue so once this limit is reach the oldest are
	 * overwritten.
	 * </p>
	 * <p>
	 * The default is 1500
	 * </p>
	 *
	 * @param count maximum number of method callbacks (i.e. web sockets) registered for expectations
	 */
	public static void maxWebSocketExpectations(final int count)
	{
		setProperty(MOCKSERVER_MAX_WEB_SOCKET_EXPECTATIONS, String.valueOf(count));
	}
	
	public static int clientNioEventLoopThreadCount()
	{
		return readIntegerProperty(
			MOCKSERVER_CLIENT_NIO_EVENT_LOOP_THREAD_COUNT,
			"MOCKSERVER_CLIENT_NIO_EVENT_LOOP_THREAD_COUNT",
			5);
	}
	
	/**
	 * <p>Client Netty worker thread pool size for handling requests and response.  These threads handle deserializing
	 * and serialising HTTP requests and responses and some other fast logic.</p>
	 *
	 * <p>Default is 5 threads</p>
	 *
	 * @param count Client Netty worker thread pool size
	 */
	public static void clientNioEventLoopThreadCount(final int count)
	{
		setProperty(MOCKSERVER_CLIENT_NIO_EVENT_LOOP_THREAD_COUNT, String.valueOf(count));
	}
	
	public static int webSocketClientEventLoopThreadCount()
	{
		return readIntegerProperty(
			MOCKSERVER_WEB_SOCKET_CLIENT_EVENT_LOOP_THREAD_COUNT,
			"MOCKSERVER_WEB_SOCKET_CLIENT_EVENT_LOOP_THREAD_COUNT",
			5);
	}
	
	/**
	 * <p>Web socket thread pool size for expectations with remote (not the same JVM) method callbacks (i.e. web
	 * sockets).</p>
	 * <p>
	 * Default is 5 threads
	 *
	 * @param count web socket worker thread pool size
	 */
	public static void webSocketClientEventLoopThreadCount(final int count)
	{
		setProperty(MOCKSERVER_WEB_SOCKET_CLIENT_EVENT_LOOP_THREAD_COUNT, String.valueOf(count));
	}
	
	public static long maxFutureTimeout()
	{
		return readLongProperty(
			MOCKSERVER_MAX_FUTURE_TIMEOUT,
			"MOCKSERVER_MAX_FUTURE_TIMEOUT",
			TimeUnit.SECONDS.toMillis(90));
	}
	
	/**
	 * Maximum time allowed in milliseconds for any future to wait, for example when waiting for a response over a web
	 * socket callback.
	 * <p>
	 * Default is 60,000 ms
	 *
	 * @param milliseconds maximum time allowed in milliseconds
	 */
	public static void maxFutureTimeout(final long milliseconds)
	{
		setProperty(MOCKSERVER_MAX_FUTURE_TIMEOUT, String.valueOf(milliseconds));
	}
	
	// socket
	
	public static long maxSocketTimeout()
	{
		return readLongProperty(
			MOCKSERVER_MAX_SOCKET_TIMEOUT,
			"MOCKSERVER_MAX_SOCKET_TIMEOUT",
			TimeUnit.SECONDS.toMillis(20));
	}
	
	/**
	 * Maximum time in milliseconds allowed for a response from a socket
	 * <p>
	 * Default is 20,000 ms
	 *
	 * @param milliseconds maximum time in milliseconds allowed
	 */
	public static void maxSocketTimeout(final long milliseconds)
	{
		setProperty(MOCKSERVER_MAX_SOCKET_TIMEOUT, String.valueOf(milliseconds));
	}
	
	public static long socketConnectionTimeout()
	{
		return readLongProperty(
			MOCKSERVER_SOCKET_CONNECTION_TIMEOUT,
			"MOCKSERVER_SOCKET_CONNECTION_TIMEOUT",
			TimeUnit.SECONDS.toMillis(20));
	}
	
	/**
	 * Maximum time in milliseconds allowed to connect to a socket
	 * <p>
	 * Default is 20,000 ms
	 *
	 * @param milliseconds maximum time allowed in milliseconds
	 */
	public static void socketConnectionTimeout(final long milliseconds)
	{
		setProperty(MOCKSERVER_SOCKET_CONNECTION_TIMEOUT, String.valueOf(milliseconds));
	}
	
	/**
	 * If true the BinaryRequestProxyingHandler.binaryExchangeCallback is called before a response is received from the
	 * remote host. This enables the proxying of messages without a response.
	 * <p>
	 * The default is false
	 *
	 * @param forwardBinaryRequestsAsynchronously target value
	 */
	public static void forwardBinaryRequestsWithoutWaitingForResponse(final boolean forwardBinaryRequestsAsynchronously)
	{
		setProperty(
			MOCKSERVER_FORWARD_BINARY_REQUESTS_WITHOUT_WAITING_FOR_RESPONSE,
			String.valueOf(forwardBinaryRequestsAsynchronously));
	}
	
	public static boolean forwardBinaryRequestsWithoutWaitingForResponse()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_FORWARD_BINARY_REQUESTS_WITHOUT_WAITING_FOR_RESPONSE,
			"MOCKSERVER_FORWARD_BINARY_REQUESTS_WITHOUT_WAITING_FOR_RESPONSE",
			"false"));
	}
	
	// proxy
	
	public static boolean attemptToProxyIfNoMatchingExpectation()
	{
		return Boolean.parseBoolean(readPropertyHierarchically(
			properties,
			MOCKSERVER_ATTEMPT_TO_PROXY_IF_NO_MATCHING_EXPECTATION,
			"MOCKSERVER_ATTEMPT_TO_PROXY_IF_NO_MATCHING_EXPECTATION",
			"true"));
	}
	
	/**
	 * If true (the default) when no matching expectation is found, and the host header of the request does not match
	 * MockServer's host, then MockServer attempts to proxy the request if that fails then a 404 is returned. If false
	 * when no matching expectation is found, and MockServer is not being used as a proxy, then MockServer always
	 * returns a 404 immediately.
	 *
	 * @param enable enables automatically attempted proxying of request that don't match an expectation and look like
	 *               they should be proxied
	 */
	public static void attemptToProxyIfNoMatchingExpectation(final boolean enable)
	{
		setProperty(MOCKSERVER_ATTEMPT_TO_PROXY_IF_NO_MATCHING_EXPECTATION, String.valueOf(enable));
	}
	
	public static InetSocketAddress forwardHttpProxy()
	{
		return readInetSocketAddressProperty(MOCKSERVER_FORWARD_HTTP_PROXY, "MOCKSERVER_FORWARD_HTTP_PROXY");
	}
	
	/**
	 * Use HTTP proxy (i.e. via Host header) for all outbound / forwarded requests
	 * <p>
	 * The default is null
	 *
	 * @param hostAndPort host and port for HTTP proxy (i.e. via Host header) for all outbound / forwarded requests
	 */
	public static void forwardHttpProxy(final String hostAndPort)
	{
		validateHostAndPortAndSetProperty(hostAndPort, MOCKSERVER_FORWARD_HTTP_PROXY);
	}
	
	/**
	 * Use HTTP proxy (i.e. via Host header) for all outbound / forwarded requests
	 * <p>
	 * The default is null
	 *
	 * @param hostAndPort host and port for HTTP proxy (i.e. via Host header) for all outbound / forwarded requests
	 */
	public static void forwardHttpProxy(final InetSocketAddress hostAndPort)
	{
		validateHostAndPortAndSetProperty(hostAndPort.toString(), MOCKSERVER_FORWARD_HTTP_PROXY);
	}
	
	public static InetSocketAddress forwardSocksProxy()
	{
		return readInetSocketAddressProperty(MOCKSERVER_FORWARD_SOCKS_PROXY, "MOCKSERVER_FORWARD_SOCKS_PROXY");
	}
	
	/**
	 * Use SOCKS proxy for all outbound / forwarded requests, support TLS tunnelling of TCP connections
	 * <p>
	 * The default is null
	 *
	 * @param hostAndPort host and port for SOCKS proxy for all outbound / forwarded requests
	 */
	public static void forwardSocksProxy(final String hostAndPort)
	{
		validateHostAndPortAndSetProperty(hostAndPort, MOCKSERVER_FORWARD_SOCKS_PROXY);
	}
	
	/**
	 * Use SOCKS proxy for all outbound / forwarded requests, support TLS tunnelling of TCP connections
	 * <p>
	 * The default is null
	 *
	 * @param hostAndPort host and port for SOCKS proxy for all outbound / forwarded requests
	 */
	public static void forwardSocksProxy(final InetSocketAddress hostAndPort)
	{
		validateHostAndPortAndSetProperty(hostAndPort.toString(), MOCKSERVER_FORWARD_SOCKS_PROXY);
	}
	
	public static String forwardProxyAuthenticationUsername()
	{
		return readPropertyHierarchically(
			properties,
			MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_USERNAME,
			"MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_USERNAME",
			"");
	}
	
	/**
	 * <p>Username for proxy authentication when using HTTPS proxy (i.e. HTTP CONNECT) for all outbound / forwarded
	 * requests</p>
	 * <p><strong>Note:</strong> <a target="_blank"
	 * href="https://www.oracle.com/java/technologies/javase/8u111-relnotes.html">8u111 Update Release Notes</a> state
	 * that the Basic authentication scheme has been deactivated when setting up an HTTPS tunnel.  To resolve this
	 * clear
	 * or set to an empty string the following system properties: <code class="inline
	 * code">jdk.http.auth.tunneling.disabledSchemes</code> and <code class="inline
	 * code">jdk.http.auth.proxying.disabledSchemes</code>.</p>
	 * <p>
	 * The default is null
	 *
	 * @param forwardProxyAuthenticationUsername username for proxy authentication
	 */
	public static void forwardProxyAuthenticationUsername(final String forwardProxyAuthenticationUsername)
	{
		if(forwardProxyAuthenticationUsername != null)
		{
			setProperty(MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_USERNAME, forwardProxyAuthenticationUsername);
		}
		else
		{
			clearProperty(MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_USERNAME);
		}
	}
	
	public static String forwardProxyAuthenticationPassword()
	{
		return readPropertyHierarchically(
			properties,
			MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_PASSWORD,
			"MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_PASSWORD",
			"");
	}
	
	/**
	 * <p>Password for proxy authentication when using HTTPS proxy (i.e. HTTP CONNECT) for all outbound / forwarded
	 * requests</p>
	 * <p><strong>Note:</strong> <a target="_blank"
	 * href="https://www.oracle.com/java/technologies/javase/8u111-relnotes.html">8u111 Update Release Notes</a> state
	 * that the Basic authentication scheme has been deactivated when setting up an HTTPS tunnel.  To resolve this
	 * clear
	 * or set to an empty string the following system properties: <code class="inline
	 * code">jdk.http.auth.tunneling.disabledSchemes</code> and <code class="inline
	 * code">jdk.http.auth.proxying.disabledSchemes</code>.</p>
	 * <p>
	 * The default is null
	 *
	 * @param forwardProxyAuthenticationPassword password for proxy authentication
	 */
	public static void forwardProxyAuthenticationPassword(final String forwardProxyAuthenticationPassword)
	{
		if(forwardProxyAuthenticationPassword != null)
		{
			setProperty(MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_PASSWORD, forwardProxyAuthenticationPassword);
		}
		else
		{
			clearProperty(MOCKSERVER_FORWARD_PROXY_AUTHENTICATION_PASSWORD);
		}
	}
	
	@SuppressWarnings("checkstyle:FinalParameters")
	protected static void validateHostAndPortAndSetProperty(String hostAndPort, final String mockserverSocksProxy)
	{
		if(isBlank(hostAndPort))
		{
			clearProperty(mockserverSocksProxy);
			return;
		}
		if(hostAndPort.startsWith("/"))
		{
			hostAndPort = StringUtils.substringAfter(hostAndPort, "/");
		}
		final String errorMessage = "Invalid property value \"" + hostAndPort + "\" for \"" + mockserverSocksProxy
			+ "\" must include <host>:<port> for example \"127.0.0.1:1090\" or \"localhost:1090\"";
		try
		{
			final URI uri = new URI("https://" + hostAndPort);
			if(uri.getHost() == null || uri.getPort() == -1)
			{
				throw new IllegalArgumentException(errorMessage);
			}
			else
			{
				setProperty(mockserverSocksProxy, hostAndPort);
			}
		}
		catch(final URISyntaxException ex)
		{
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}
	
	protected static InetSocketAddress readInetSocketAddressProperty(
		final String key,
		final String environmentVariableKey)
	{
		InetSocketAddress inetSocketAddress = null;
		final String proxy = readPropertyHierarchically(properties, key, environmentVariableKey, "");
		if(proxy != null && proxy.contains(":"))
		{
			final String[] proxyParts = proxy.split(":");
			if(proxyParts.length > 1)
			{
				try
				{
					inetSocketAddress = new InetSocketAddress(proxyParts[0], Integer.parseInt(proxyParts[1]));
				}
				catch(final NumberFormatException nfe)
				{
					LOG.error("NumberFormatException converting value \"{}\" into an integer", proxyParts[1], nfe);
				}
			}
		}
		return inetSocketAddress;
	}
	
	protected static Integer readIntegerProperty(
		final String key,
		final String environmentVariableKey,
		final int defaultValue)
	{
		try
		{
			return Integer.parseInt(readPropertyHierarchically(
				properties,
				key,
				environmentVariableKey,
				String.valueOf(defaultValue)));
		}
		catch(final NumberFormatException nfe)
		{
			LOG.error(
				"NumberFormatException converting {} with value [{}]",
				key,
				readPropertyHierarchically(properties, key, environmentVariableKey, String.valueOf(defaultValue)),
				nfe);
			return defaultValue;
		}
	}
	
	protected static Long readLongProperty(
		final String key,
		final String environmentVariableKey,
		final long defaultValue)
	{
		try
		{
			return Long.parseLong(readPropertyHierarchically(
				properties,
				key,
				environmentVariableKey,
				String.valueOf(defaultValue)));
		}
		catch(final NumberFormatException nfe)
		{
			LOG.error(
				"NumberFormatException converting {} with value [{}]",
				key,
				readPropertyHierarchically(properties, key, environmentVariableKey, String.valueOf(defaultValue)),
				nfe);
			return defaultValue;
		}
	}
	
	protected static Map<String, String> propertyCache;
	
	protected static Map<String, String> getPropertyCache()
	{
		if(propertyCache == null)
		{
			propertyCache = new ConcurrentHashMap<>();
		}
		return propertyCache;
	}
	
	protected static void setProperty(final String systemPropertyKey, final String value)
	{
		getPropertyCache().put(systemPropertyKey, value);
		System.setProperty(systemPropertyKey, value);
	}
	
	protected static void clearProperty(final String systemPropertyKey)
	{
		getPropertyCache().remove(systemPropertyKey);
		System.clearProperty(systemPropertyKey);
	}
	
	protected static String readPropertyHierarchically(
		final Properties properties,
		final String systemPropertyKey,
		final String environmentVariableKey,
		final String defaultValue)
	{
		final String cachedPropertyValue = getPropertyCache().get(systemPropertyKey);
		if(cachedPropertyValue != null)
		{
			return cachedPropertyValue;
		}
		
		if(isBlank(environmentVariableKey))
		{
			throw new IllegalArgumentException("environment property name cannot be null for " + systemPropertyKey);
		}
		
		final String defaultOrEnvironmentVariable = isNotBlank(System.getenv(environmentVariableKey))
			? System.getenv(environmentVariableKey)
			: defaultValue;
		String propertyValue = System.getProperty(
			systemPropertyKey,
			properties != null
				? properties.getProperty(systemPropertyKey, defaultOrEnvironmentVariable)
				: defaultOrEnvironmentVariable);
		if(propertyValue != null && propertyValue.startsWith("\"") && propertyValue.endsWith("\""))
		{
			propertyValue = UNESCAPE_QUOTES_PATTERN.matcher(propertyValue).replaceAll("");
		}
		getPropertyCache().put(systemPropertyKey, propertyValue);
		return propertyValue;
	}
}
