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

import java.net.InetSocketAddress;


@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Configuration
{
	public static Configuration configuration()
	{
		return new Configuration();
	}
	
	protected Integer maxWebSocketExpectations;
	
	// scalability
	protected Integer clientNioEventLoopThreadCount;
	protected Integer webSocketClientEventLoopThreadCount;
	protected Long maxFutureTimeoutInMillis;
	
	// socket
	protected Long maxSocketTimeoutInMillis;
	protected Long socketConnectionTimeoutInMillis;
	
	// non http proxing
	private Boolean forwardBinaryRequestsWithoutWaitingForResponse;
	
	// proxy
	protected Boolean attemptToProxyIfNoMatchingExpectation;
	protected InetSocketAddress forwardHttpProxy;
	protected InetSocketAddress forwardSocksProxy;
	protected String forwardProxyAuthenticationUsername;
	protected String forwardProxyAuthenticationPassword;
	
	public Integer maxWebSocketExpectations()
	{
		if(this.maxWebSocketExpectations == null)
		{
			return ConfigurationProperties.maxWebSocketExpectations();
		}
		return this.maxWebSocketExpectations;
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
	 * @param maxWebSocketExpectations maximum number of method callbacks (i.e. web sockets) registered for
	 *                                 expectations
	 */
	public Configuration maxWebSocketExpectations(final Integer maxWebSocketExpectations)
	{
		this.maxWebSocketExpectations = maxWebSocketExpectations;
		return this;
	}
	
	public Integer clientNioEventLoopThreadCount()
	{
		if(this.clientNioEventLoopThreadCount == null)
		{
			return ConfigurationProperties.clientNioEventLoopThreadCount();
		}
		return this.clientNioEventLoopThreadCount;
	}
	
	/**
	 * <p>Client Netty worker thread pool size for handling requests and response.  These threads handle deserializing
	 * and serialising HTTP requests and responses and some other fast logic.</p>
	 *
	 * <p>Default is 5 threads</p>
	 *
	 * @param clientNioEventLoopThreadCount Client Netty worker thread pool size
	 */
	public Configuration clientNioEventLoopThreadCount(final Integer clientNioEventLoopThreadCount)
	{
		this.clientNioEventLoopThreadCount = clientNioEventLoopThreadCount;
		return this;
	}
	
	public Integer webSocketClientEventLoopThreadCount()
	{
		if(this.webSocketClientEventLoopThreadCount == null)
		{
			return ConfigurationProperties.webSocketClientEventLoopThreadCount();
		}
		return this.webSocketClientEventLoopThreadCount;
	}
	
	/**
	 * <p>Client Netty worker thread pool size for handling requests and response.  These threads handle deserializing
	 * and serialising HTTP requests and responses and some other fast logic.</p>
	 *
	 * <p>Default is 5 threads</p>
	 *
	 * @param webSocketClientEventLoopThreadCount Client Netty worker thread pool size
	 */
	public Configuration webSocketClientEventLoopThreadCount(final Integer webSocketClientEventLoopThreadCount)
	{
		this.webSocketClientEventLoopThreadCount = webSocketClientEventLoopThreadCount;
		return this;
	}
	
	public Long maxFutureTimeoutInMillis()
	{
		if(this.maxFutureTimeoutInMillis == null)
		{
			return ConfigurationProperties.maxFutureTimeout();
		}
		return this.maxFutureTimeoutInMillis;
	}
	
	/**
	 * Maximum time allowed in milliseconds for any future to wait, for example when waiting for a response over a web
	 * socket callback.
	 * <p>
	 * Default is 60,000 ms
	 *
	 * @param maxFutureTimeoutInMillis maximum time allowed in milliseconds
	 */
	public Configuration maxFutureTimeoutInMillis(final Long maxFutureTimeoutInMillis)
	{
		this.maxFutureTimeoutInMillis = maxFutureTimeoutInMillis;
		return this;
	}
	
	public Long maxSocketTimeoutInMillis()
	{
		if(this.maxSocketTimeoutInMillis == null)
		{
			return ConfigurationProperties.maxSocketTimeout();
		}
		return this.maxSocketTimeoutInMillis;
	}
	
	/**
	 * Maximum time in milliseconds allowed for a response from a socket
	 * <p>
	 * Default is 20,000 ms
	 *
	 * @param maxSocketTimeoutInMillis maximum time in milliseconds allowed
	 */
	public Configuration maxSocketTimeoutInMillis(final Long maxSocketTimeoutInMillis)
	{
		this.maxSocketTimeoutInMillis = maxSocketTimeoutInMillis;
		return this;
	}
	
	public Long socketConnectionTimeoutInMillis()
	{
		if(this.socketConnectionTimeoutInMillis == null)
		{
			return ConfigurationProperties.socketConnectionTimeout();
		}
		return this.socketConnectionTimeoutInMillis;
	}
	
	/**
	 * Maximum time in milliseconds allowed to connect to a socket
	 * <p>
	 * Default is 20,000 ms
	 *
	 * @param socketConnectionTimeoutInMillis maximum time allowed in milliseconds
	 */
	public Configuration socketConnectionTimeoutInMillis(final Long socketConnectionTimeoutInMillis)
	{
		this.socketConnectionTimeoutInMillis = socketConnectionTimeoutInMillis;
		return this;
	}
	
	public Boolean forwardBinaryRequestsWithoutWaitingForResponse()
	{
		if(this.forwardBinaryRequestsWithoutWaitingForResponse == null)
		{
			return ConfigurationProperties.forwardBinaryRequestsWithoutWaitingForResponse();
		}
		return this.forwardBinaryRequestsWithoutWaitingForResponse;
	}
	
	/**
	 * If true the BinaryProxyListener is called before a response is received from the remote host. This enables the
	 * proxying of messages without a response.
	 * <p>
	 * The default is false
	 *
	 * @param forwardBinaryRequestsWithoutWaitingForResponse target value
	 */
	public Configuration forwardBinaryRequestsWithoutWaitingForResponse(
		final Boolean forwardBinaryRequestsWithoutWaitingForResponse)
	{
		this.forwardBinaryRequestsWithoutWaitingForResponse = forwardBinaryRequestsWithoutWaitingForResponse;
		return this;
	}
	
	public Boolean attemptToProxyIfNoMatchingExpectation()
	{
		if(this.attemptToProxyIfNoMatchingExpectation == null)
		{
			return ConfigurationProperties.attemptToProxyIfNoMatchingExpectation();
		}
		return this.attemptToProxyIfNoMatchingExpectation;
	}
	
	/**
	 * If true (the default) when no matching expectation is found, and the host header of the request does not match
	 * MockServer's host, then MockServer attempts to proxy the request if that fails then a 404 is returned. If false
	 * when no matching expectation is found, and MockServer is not being used as a proxy, then MockServer always
	 * returns a 404 immediately.
	 *
	 * @param attemptToProxyIfNoMatchingExpectation enables automatically attempted proxying of request that don't
	 *                                                 match
	 *                                              an expectation and look like they should be proxied
	 */
	public Configuration attemptToProxyIfNoMatchingExpectation(final Boolean attemptToProxyIfNoMatchingExpectation)
	{
		this.attemptToProxyIfNoMatchingExpectation = attemptToProxyIfNoMatchingExpectation;
		return this;
	}
	
	public InetSocketAddress forwardHttpProxy()
	{
		if(this.forwardHttpProxy == null)
		{
			return ConfigurationProperties.forwardHttpProxy();
		}
		return this.forwardHttpProxy;
	}
	
	/**
	 * Use HTTP proxy (i.e. via Host header) for all outbound / forwarded requests
	 * <p>
	 * The default is null
	 *
	 * @param forwardHttpProxy host and port for HTTP proxy (i.e. via Host header) for all outbound / forwarded
	 *                         requests
	 */
	public Configuration forwardHttpProxy(final InetSocketAddress forwardHttpProxy)
	{
		this.forwardHttpProxy = forwardHttpProxy;
		return this;
	}
	
	public InetSocketAddress forwardSocksProxy()
	{
		if(this.forwardSocksProxy == null)
		{
			return ConfigurationProperties.forwardSocksProxy();
		}
		return this.forwardSocksProxy;
	}
	
	/**
	 * Use SOCKS proxy for all outbound / forwarded requests, support TLS tunnelling of TCP connections
	 * <p>
	 * The default is null
	 *
	 * @param forwardSocksProxy host and port for SOCKS proxy for all outbound / forwarded requests
	 */
	public Configuration forwardSocksProxy(final InetSocketAddress forwardSocksProxy)
	{
		this.forwardSocksProxy = forwardSocksProxy;
		return this;
	}
	
	public String forwardProxyAuthenticationUsername()
	{
		if(this.forwardProxyAuthenticationUsername == null)
		{
			return ConfigurationProperties.forwardProxyAuthenticationUsername();
		}
		return this.forwardProxyAuthenticationUsername;
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
	public Configuration forwardProxyAuthenticationUsername(final String forwardProxyAuthenticationUsername)
	{
		this.forwardProxyAuthenticationUsername = forwardProxyAuthenticationUsername;
		return this;
	}
	
	public String forwardProxyAuthenticationPassword()
	{
		if(this.forwardProxyAuthenticationPassword == null)
		{
			return ConfigurationProperties.forwardProxyAuthenticationPassword();
		}
		return this.forwardProxyAuthenticationPassword;
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
	public Configuration forwardProxyAuthenticationPassword(final String forwardProxyAuthenticationPassword)
	{
		this.forwardProxyAuthenticationPassword = forwardProxyAuthenticationPassword;
		return this;
	}
}
