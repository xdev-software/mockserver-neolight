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

import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import software.xdev.mockserver.model.BinaryProxyListener;
import software.xdev.mockserver.socket.tls.ForwardProxyTLSX509CertificatesTrustManager;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static software.xdev.mockserver.configuration.ConfigurationProperties.fileExists;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Configuration {

    public static Configuration configuration() {
        return new Configuration();
    }

    // logging
    private Level logLevel;
    private Boolean disableSystemOut;
    private Boolean disableLogging;
    private Boolean detailedMatchFailures;
    
    private Integer maxExpectations;
    private Integer maxLogEntries;
    private Integer maxWebSocketExpectations;
    
    // scalability
    private Integer nioEventLoopThreadCount;
    private Integer actionHandlerThreadCount;
    private Integer clientNioEventLoopThreadCount;
    private Integer webSocketClientEventLoopThreadCount;
    private Long maxFutureTimeoutInMillis;
    private Boolean matchersFailFast;

    // socket
    private Long maxSocketTimeoutInMillis;
    private Long socketConnectionTimeoutInMillis;
    private Boolean alwaysCloseSocketConnections;
    private String localBoundIP;

    // http request parsing
    private Integer maxInitialLineLength;
    private Integer maxHeaderSize;
    private Integer maxChunkSize;
    private Boolean useSemicolonAsQueryParameterSeparator;
    private Boolean assumeAllRequestsAreHttp;

    // non http proxying
    private Boolean forwardBinaryRequestsWithoutWaitingForResponse;
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
    private Boolean attemptToProxyIfNoMatchingExpectation;
    private InetSocketAddress forwardHttpProxy;
    private InetSocketAddress forwardHttpsProxy;
    private InetSocketAddress forwardSocksProxy;
    private String forwardProxyAuthenticationUsername;
    private String forwardProxyAuthenticationPassword;
    private String proxyAuthenticationRealm;
    private String proxyAuthenticationUsername;
    private String proxyAuthenticationPassword;
    private String noProxyHosts;

    // liveness
    private String livenessHttpGetPath;

    // control plane authentication
    private Boolean controlPlaneTLSMutualAuthenticationRequired;
    private String controlPlaneTLSMutualAuthenticationCAChain;
    private String controlPlanePrivateKeyPath;
    private String controlPlaneX509CertificatePath;
    private Boolean controlPlaneJWTAuthenticationRequired;
    private String controlPlaneJWTAuthenticationJWKSource;
    private String controlPlaneJWTAuthenticationExpectedAudience;
    private Map<String, String> controlPlaneJWTAuthenticationMatchingClaims;
    private Set<String> controlPlaneJWTAuthenticationRequiredClaims;

    // TLS
    private Boolean proactivelyInitialiseTLS;
    private boolean rebuildTLSContext;
    private boolean rebuildServerTLSContext;
    private String tlsProtocols;

    // inbound - dynamic CA
    private Boolean dynamicallyCreateCertificateAuthorityCertificate;
    private String directoryToSaveDynamicSSLCertificate;

    // inbound - dynamic private key & x509
    private Boolean preventCertificateDynamicUpdate;
    private String sslCertificateDomainName;
    private Set<String> sslSubjectAlternativeNameDomains;
    private Set<String> sslSubjectAlternativeNameIps;

    // inbound - fixed CA
    private String certificateAuthorityPrivateKey;
    private String certificateAuthorityCertificate;

    // inbound - fixed private key & x509
    private String privateKeyPath;
    private String x509CertificatePath;

    // inbound - mTLS
    private Boolean tlsMutualAuthenticationRequired;
    private String tlsMutualAuthenticationCertificateChain;

    // outbound - CA
    private ForwardProxyTLSX509CertificatesTrustManager forwardProxyTLSX509CertificatesTrustManagerType;

    // outbound - fixed CA
    private String forwardProxyTLSCustomTrustX509Certificates;

    // outbound - fixed private key & x509
    private String forwardProxyPrivateKey;
    private String forwardProxyCertificateChain;


    public Level logLevel() {
        if (logLevel == null) {
            return ConfigurationProperties.logLevel();
        }
        return logLevel;
    }

    /**
     * Override the default logging level of INFO
     *
     * @param level the log level, which can be TRACE, DEBUG, INFO, WARN, ERROR, OFF, FINEST, FINE, INFO, WARNING, SEVERE
     */
    public Configuration logLevel(Level level) {
        this.logLevel = level;
        ConfigurationProperties.logLevel(this.logLevel.name());
        return this;
    }

    /**
     * Override the default logging level of INFO
     *
     * @param level the log level, which can be TRACE, DEBUG, INFO, WARN, ERROR, OFF, FINEST, FINE, INFO, WARNING, SEVERE
     */
    public Configuration logLevel(String level) {
        this.logLevel = Level.valueOf(level);
        ConfigurationProperties.logLevel(this.logLevel.name());
        return this;
    }

    public Boolean disableSystemOut() {
        if (disableSystemOut == null) {
            return ConfigurationProperties.disableSystemOut();
        }
        return disableSystemOut;
    }

    /**
     * Disable printing log to system out for JVM, default is enabled
     *
     * @param disableSystemOut printing log to system out for JVM
     */
    public Configuration disableSystemOut(Boolean disableSystemOut) {
        this.disableSystemOut = disableSystemOut;
        return this;
    }

    public Boolean disableLogging() {
        if (disableLogging == null) {
            return ConfigurationProperties.disableLogging();
        }
        return disableLogging;
    }

    /**
     * Disable all logging and processing of log events
     * <p>
     * The default is false
     *
     * @param disableLogging disable all logging
     */
    public Configuration disableLogging(Boolean disableLogging) {
        this.disableLogging = disableLogging;
        return this;
    }

    public Boolean detailedMatchFailures() {
        if (detailedMatchFailures == null) {
            return ConfigurationProperties.detailedMatchFailures();
        }
        return detailedMatchFailures;
    }

    /**
     * If true (the default) the log event recording that a request matcher did not match will include a detailed reason why each non-matching field did not match.
     *
     * @param detailedMatchFailures enabled detailed match failure log events
     */
    public Configuration detailedMatchFailures(Boolean detailedMatchFailures) {
        this.detailedMatchFailures = detailedMatchFailures;
        return this;
    }

    public Integer maxExpectations() {
        if (maxExpectations == null) {
            return ConfigurationProperties.maxExpectations();
        }
        return maxExpectations;
    }

    /**
     * <p>
     * Maximum number of expectations stored in memory.  Expectations are stored in a circular queue so once this limit is reach the oldest and lowest priority expectations are overwritten
     * </p>
     * <p>
     * The default maximum depends on the available memory in the JVM with an upper limit of 5000
     * </p>
     *
     * @param maxExpectations maximum number of expectations to store
     */
    public Configuration maxExpectations(Integer maxExpectations) {
        this.maxExpectations = maxExpectations;
        return this;
    }
    
    public Integer maxLogEntries() {
        if (maxLogEntries == null) {
            return ConfigurationProperties.maxLogEntries();
        }
        return maxLogEntries;
    }
    
    /**
     * <p>
     * Maximum number of log entries stored in memory.  Log entries are stored in a circular queue so once this limit is reach the oldest log entries are overwritten
     * </p>
     * <p>
     * The default maximum depends on the available memory in the JVM with an upper limit of 60000
     * </p>
     *
     * @param maxLogEntries maximum number of expectations to store
     */
    public Configuration maxLogEntries(Integer maxLogEntries) {
        this.maxLogEntries = maxLogEntries;
        return this;
    }
    
    public Integer maxWebSocketExpectations() {
        if (maxWebSocketExpectations == null) {
            return ConfigurationProperties.maxWebSocketExpectations();
        }
        return maxWebSocketExpectations;
    }
    
    /**
     * <p>
     * Maximum number of remote (not the same JVM) method callbacks (i.e. web sockets) registered for expectations.  The web socket client registry entries are stored in a circular queue so once this limit is reach the oldest are overwritten.
     * </p>
     * <p>
     * The default is 1500
     * </p>
     *
     * @param maxWebSocketExpectations maximum number of method callbacks (i.e. web sockets) registered for expectations
     */
    public Configuration maxWebSocketExpectations(Integer maxWebSocketExpectations) {
        this.maxWebSocketExpectations = maxWebSocketExpectations;
        return this;
    }

    public Integer nioEventLoopThreadCount() {
        if (nioEventLoopThreadCount == null) {
            return ConfigurationProperties.nioEventLoopThreadCount();
        }
        return nioEventLoopThreadCount;
    }

    /**
     * <p>Netty worker thread pool size for handling requests and response.  These threads handle deserializing and serialising HTTP requests and responses and some other fast logic, long running tasks are done on the action handler thread pool.</p>
     *
     * @param nioEventLoopThreadCount Netty worker thread pool size
     */
    public Configuration nioEventLoopThreadCount(Integer nioEventLoopThreadCount) {
        this.nioEventLoopThreadCount = nioEventLoopThreadCount;
        return this;
    }

    public Integer actionHandlerThreadCount() {
        if (actionHandlerThreadCount == null) {
            return ConfigurationProperties.actionHandlerThreadCount();
        }
        return actionHandlerThreadCount;
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
    public Configuration actionHandlerThreadCount(Integer actionHandlerThreadCount) {
        this.actionHandlerThreadCount = actionHandlerThreadCount;
        return this;
    }

    public Integer clientNioEventLoopThreadCount() {
        if (clientNioEventLoopThreadCount == null) {
            return ConfigurationProperties.clientNioEventLoopThreadCount();
        }
        return clientNioEventLoopThreadCount;
    }

    /**
     * <p>Client Netty worker thread pool size for handling requests and response.  These threads handle deserializing and serialising HTTP requests and responses and some other fast logic.</p>
     *
     * <p>Default is 5 threads</p>
     *
     * @param clientNioEventLoopThreadCount Client Netty worker thread pool size
     */
    public Configuration clientNioEventLoopThreadCount(Integer clientNioEventLoopThreadCount) {
        this.clientNioEventLoopThreadCount = clientNioEventLoopThreadCount;
        return this;
    }

    public Integer webSocketClientEventLoopThreadCount() {
        if (webSocketClientEventLoopThreadCount == null) {
            return ConfigurationProperties.webSocketClientEventLoopThreadCount();
        }
        return webSocketClientEventLoopThreadCount;
    }

    /**
     * <p>Client Netty worker thread pool size for handling requests and response.  These threads handle deserializing and serialising HTTP requests and responses and some other fast logic.</p>
     *
     * <p>Default is 5 threads</p>
     *
     * @param webSocketClientEventLoopThreadCount Client Netty worker thread pool size
     */
    public Configuration webSocketClientEventLoopThreadCount(Integer webSocketClientEventLoopThreadCount) {
        this.webSocketClientEventLoopThreadCount = webSocketClientEventLoopThreadCount;
        return this;
    }

    public Long maxFutureTimeoutInMillis() {
        if (maxFutureTimeoutInMillis == null) {
            return ConfigurationProperties.maxFutureTimeout();
        }
        return maxFutureTimeoutInMillis;
    }

    /**
     * Maximum time allowed in milliseconds for any future to wait, for example when waiting for a response over a web socket callback.
     * <p>
     * Default is 60,000 ms
     *
     * @param maxFutureTimeoutInMillis maximum time allowed in milliseconds
     */
    public Configuration maxFutureTimeoutInMillis(Long maxFutureTimeoutInMillis) {
        this.maxFutureTimeoutInMillis = maxFutureTimeoutInMillis;
        return this;
    }

    public Boolean matchersFailFast() {
        if (matchersFailFast == null) {
            return ConfigurationProperties.matchersFailFast();
        }
        return matchersFailFast;
    }

    /**
     * If true (the default) request matchers will fail on the first non-matching field, if false request matchers will compare all fields.
     * This is useful to see all mismatching fields in the log event recording that a request matcher did not match.
     *
     * @param matchersFailFast enabled request matchers failing fast
     */
    public Configuration matchersFailFast(Boolean matchersFailFast) {
        this.matchersFailFast = matchersFailFast;
        return this;
    }

    public Long maxSocketTimeoutInMillis() {
        if (maxSocketTimeoutInMillis == null) {
            return ConfigurationProperties.maxSocketTimeout();
        }
        return maxSocketTimeoutInMillis;
    }

    /**
     * Maximum time in milliseconds allowed for a response from a socket
     * <p>
     * Default is 20,000 ms
     *
     * @param maxSocketTimeoutInMillis maximum time in milliseconds allowed
     */
    public Configuration maxSocketTimeoutInMillis(Long maxSocketTimeoutInMillis) {
        this.maxSocketTimeoutInMillis = maxSocketTimeoutInMillis;
        return this;
    }

    public Long socketConnectionTimeoutInMillis() {
        if (socketConnectionTimeoutInMillis == null) {
            return ConfigurationProperties.socketConnectionTimeout();
        }
        return socketConnectionTimeoutInMillis;
    }

    /**
     * Maximum time in milliseconds allowed to connect to a socket
     * <p>
     * Default is 20,000 ms
     *
     * @param socketConnectionTimeoutInMillis maximum time allowed in milliseconds
     */
    public Configuration socketConnectionTimeoutInMillis(Long socketConnectionTimeoutInMillis) {
        this.socketConnectionTimeoutInMillis = socketConnectionTimeoutInMillis;
        return this;
    }

    public Boolean alwaysCloseSocketConnections() {
        if (alwaysCloseSocketConnections == null) {
            return ConfigurationProperties.alwaysCloseSocketConnections();
        }
        return alwaysCloseSocketConnections;
    }

    /**
     * <p>If true socket connections will always be closed after a response is returned, if false connection is only closed if request header indicate connection should be closed.</p>
     * <p>
     * Default is false
     *
     * @param alwaysCloseSocketConnections true socket connections will always be closed after a response is returned
     */
    public Configuration alwaysCloseSocketConnections(Boolean alwaysCloseSocketConnections) {
        this.alwaysCloseSocketConnections = alwaysCloseSocketConnections;
        return this;
    }

    public String localBoundIP() {
        if (localBoundIP == null) {
            return ConfigurationProperties.localBoundIP();
        }
        return localBoundIP;
    }

    /**
     * The local IP address to bind to for accepting new socket connections
     * <p>
     * Default is 0.0.0.0
     *
     * @param localBoundIP local IP address to bind to for accepting new socket connections
     */
    public Configuration localBoundIP(String localBoundIP) {
        this.localBoundIP = localBoundIP;
        return this;
    }

    public Integer maxInitialLineLength() {
        if (maxInitialLineLength == null) {
            return ConfigurationProperties.maxInitialLineLength();
        }
        return maxInitialLineLength;
    }

    /**
     * Maximum size of the first line of an HTTP request
     * <p>
     * The default is Integer.MAX_VALUE
     *
     * @param maxInitialLineLength maximum size of the first line of an HTTP request
     */
    public Configuration maxInitialLineLength(Integer maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
        return this;
    }

    public Integer maxHeaderSize() {
        if (maxHeaderSize == null) {
            return ConfigurationProperties.maxHeaderSize();
        }
        return maxHeaderSize;
    }

    /**
     * Maximum size of HTTP request headers
     * <p>
     * The default is Integer.MAX_VALUE
     *
     * @param maxHeaderSize maximum size of HTTP request headers
     */
    public Configuration maxHeaderSize(Integer maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
        return this;
    }

    public Integer maxChunkSize() {
        if (maxChunkSize == null) {
            return ConfigurationProperties.maxChunkSize();
        }
        return maxChunkSize;
    }

    /**
     * Maximum size of HTTP chunks in request or responses
     * <p>
     * The default is Integer.MAX_VALUE
     *
     * @param maxChunkSize maximum size of HTTP chunks in request or responses
     */
    public Configuration maxChunkSize(Integer maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        return this;
    }

    public Boolean useSemicolonAsQueryParameterSeparator() {
        if (useSemicolonAsQueryParameterSeparator == null) {
            return ConfigurationProperties.useSemicolonAsQueryParameterSeparator();
        }
        return useSemicolonAsQueryParameterSeparator;
    }

    /**
     * If true semicolons are treated as a separator for a query parameter string, if false the semicolon is treated as a normal character that is part of a query parameter value.
     * <p>
     * The default is true
     *
     * @param useSemicolonAsQueryParameterSeparator if true semicolons are treated as a separator for a query parameter string
     */
    public Configuration useSemicolonAsQueryParameterSeparator(Boolean useSemicolonAsQueryParameterSeparator) {
        this.useSemicolonAsQueryParameterSeparator = useSemicolonAsQueryParameterSeparator;
        return this;
    }

    public Boolean assumeAllRequestsAreHttp() {
        if (assumeAllRequestsAreHttp == null) {
            return ConfigurationProperties.assumeAllRequestsAreHttp();
        }
        return assumeAllRequestsAreHttp;
    }

    /**
     * If false requests are assumed as binary if the method isn't one of "GET", "POST", "PUT", "HEAD", "OPTIONS", "PATCH", "DELETE", "TRACE" or "CONNECT"
     * <p>
     * The default is false
     *
     * @param assumeAllRequestsAreHttp if false requests are assumed as binary if the method isn't one of "GET", "POST", "PUT", "HEAD", "OPTIONS", "PATCH", "DELETE", "TRACE" or "CONNECT"
     */
    public Configuration assumeAllRequestsAreHttp(Boolean assumeAllRequestsAreHttp) {
        this.assumeAllRequestsAreHttp = assumeAllRequestsAreHttp;
        return this;
    }

    public Boolean forwardBinaryRequestsWithoutWaitingForResponse() {
        if (forwardBinaryRequestsWithoutWaitingForResponse == null) {
            return ConfigurationProperties.forwardBinaryRequestsWithoutWaitingForResponse();
        }
        return forwardBinaryRequestsWithoutWaitingForResponse;
    }

    /**
     * If true the BinaryProxyListener is called before a response is received from the
     * remote host. This enables the proxying of messages without a response.
     * <p>
     * The default is false
     *
     * @param forwardBinaryRequestsWithoutWaitingForResponse target value
     */
    public Configuration forwardBinaryRequestsWithoutWaitingForResponse(Boolean forwardBinaryRequestsWithoutWaitingForResponse) {
        this.forwardBinaryRequestsWithoutWaitingForResponse = forwardBinaryRequestsWithoutWaitingForResponse;
        return this;
    }

    public BinaryProxyListener binaryProxyListener() {
        return binaryProxyListener;
    }

    /**
     * Set a software.xdev.mockserver.model.BinaryProxyListener called when binary content is proxied
     *
     * @param binaryProxyListener a BinaryProxyListener called when binary content is proxied
     */
    public Configuration binaryProxyListener(BinaryProxyListener binaryProxyListener) {
        this.binaryProxyListener = binaryProxyListener;
        return this;
    }

    public Boolean enableCORSForAPI() {
        if (enableCORSForAPI == null) {
            return ConfigurationProperties.enableCORSForAPI();
        }
        return enableCORSForAPI;
    }

    /**
     * Enable CORS for MockServer REST API so that the API can be used for javascript running in browsers, such as selenium
     * <p>
     * The default is false
     *
     * @param enableCORSForAPI CORS for MockServer REST API
     */
    public Configuration enableCORSForAPI(Boolean enableCORSForAPI) {
        this.enableCORSForAPI = enableCORSForAPI;
        return this;
    }

    public Boolean enableCORSForAllResponses() {
        if (enableCORSForAllResponses == null) {
            return ConfigurationProperties.enableCORSForAllResponses();
        }
        return enableCORSForAllResponses;
    }

    /**
     * Enable CORS for all responses from MockServer, including the REST API and expectation responses
     * <p>
     * The default is false
     *
     * @param enableCORSForAllResponses CORS for all responses from MockServer
     */
    public Configuration enableCORSForAllResponses(Boolean enableCORSForAllResponses) {
        this.enableCORSForAllResponses = enableCORSForAllResponses;
        return this;
    }

    public String corsAllowOrigin() {
        if (corsAllowOrigin == null) {
            return ConfigurationProperties.corsAllowOrigin();
        }
        return corsAllowOrigin;
    }

    /**
     * <p>the value used for CORS in the access-control-allow-origin header.</p>
     * <p>The default is ""</p>
     *
     * @param corsAllowOrigin the value used for CORS in the access-control-allow-methods header
     */
    public Configuration corsAllowOrigin(String corsAllowOrigin) {
        this.corsAllowOrigin = corsAllowOrigin;
        return this;
    }

    public String corsAllowMethods() {
        if (corsAllowMethods == null) {
            return ConfigurationProperties.corsAllowMethods();
        }
        return corsAllowMethods;
    }

    /**
     * <p>the value used for CORS in the access-control-allow-methods header.</p>
     * <p>The default is ""</p>
     *
     * @param corsAllowMethods the value used for CORS in the access-control-allow-methods header
     */
    public Configuration corsAllowMethods(String corsAllowMethods) {
        this.corsAllowMethods = corsAllowMethods;
        return this;
    }

    public String corsAllowHeaders() {
        if (corsAllowHeaders == null) {
            return ConfigurationProperties.corsAllowHeaders();
        }
        return corsAllowHeaders;
    }

    /**
     * <p>the value used for CORS in the access-control-allow-headers and access-control-expose-headers headers.</p>
     * <p>In addition to this default value any headers specified in the request header access-control-request-headers also get added to access-control-allow-headers and access-control-expose-headers headers in a CORS response.</p>
     * <p>The default is ""</p>
     *
     * @param corsAllowHeaders the value used for CORS in the access-control-allow-headers and access-control-expose-headers headers
     */
    public Configuration corsAllowHeaders(String corsAllowHeaders) {
        this.corsAllowHeaders = corsAllowHeaders;
        return this;
    }

    public Boolean corsAllowCredentials() {
        if (corsAllowCredentials == null) {
            return ConfigurationProperties.corsAllowCredentials();
        }
        return corsAllowCredentials;
    }

    /**
     * The value used for CORS in the access-control-allow-credentials header.
     * <p>
     * The default is false
     *
     * @param corsAllowCredentials the value used for CORS in the access-control-allow-credentials header
     */
    public Configuration corsAllowCredentials(Boolean corsAllowCredentials) {
        this.corsAllowCredentials = corsAllowCredentials;
        return this;
    }

    public Integer corsMaxAgeInSeconds() {
        if (corsMaxAgeInSeconds == null) {
            return ConfigurationProperties.corsMaxAgeInSeconds();
        }
        return corsMaxAgeInSeconds;
    }

    /**
     * The value used for CORS in the access-control-max-age header.
     * <p>
     * The default is 0
     *
     * @param corsMaxAgeInSeconds the value used for CORS in the access-control-max-age header.
     */
    public Configuration corsMaxAgeInSeconds(Integer corsMaxAgeInSeconds) {
        this.corsMaxAgeInSeconds = corsMaxAgeInSeconds;
        return this;
    }

    public Integer maximumNumberOfRequestToReturnInVerificationFailure() {
        if (maximumNumberOfRequestToReturnInVerificationFailure == null) {
            return ConfigurationProperties.maximumNumberOfRequestToReturnInVerificationFailure();
        }
        return maximumNumberOfRequestToReturnInVerificationFailure;
    }

    /**
     * The maximum number of requests to return in verification failure result, if more expectations are found the failure result does not list them separately
     *
     * @param maximumNumberOfRequestToReturnInVerificationFailure maximum number of expectations to return in verification failure result
     */
    public Configuration maximumNumberOfRequestToReturnInVerificationFailure(Integer maximumNumberOfRequestToReturnInVerificationFailure) {
        this.maximumNumberOfRequestToReturnInVerificationFailure = maximumNumberOfRequestToReturnInVerificationFailure;
        return this;
    }

    public Boolean attemptToProxyIfNoMatchingExpectation() {
        if (attemptToProxyIfNoMatchingExpectation == null) {
            return ConfigurationProperties.attemptToProxyIfNoMatchingExpectation();
        }
        return attemptToProxyIfNoMatchingExpectation;
    }

    /**
     * If true (the default) when no matching expectation is found, and the host header of the request does not match MockServer's host, then MockServer attempts to proxy the request if that fails then a 404 is returned.
     * If false when no matching expectation is found, and MockServer is not being used as a proxy, then MockServer always returns a 404 immediately.
     *
     * @param attemptToProxyIfNoMatchingExpectation enables automatically attempted proxying of request that don't match an expectation and look like they should be proxied
     */
    public Configuration attemptToProxyIfNoMatchingExpectation(Boolean attemptToProxyIfNoMatchingExpectation) {
        this.attemptToProxyIfNoMatchingExpectation = attemptToProxyIfNoMatchingExpectation;
        return this;
    }

    public InetSocketAddress forwardHttpProxy() {
        if (forwardHttpProxy == null) {
            return ConfigurationProperties.forwardHttpProxy();
        }
        return forwardHttpProxy;
    }

    /**
     * Use HTTP proxy (i.e. via Host header) for all outbound / forwarded requests
     * <p>
     * The default is null
     *
     * @param forwardHttpProxy host and port for HTTP proxy (i.e. via Host header) for all outbound / forwarded requests
     */
    public Configuration forwardHttpProxy(InetSocketAddress forwardHttpProxy) {
        this.forwardHttpProxy = forwardHttpProxy;
        return this;
    }

    public InetSocketAddress forwardHttpsProxy() {
        if (forwardHttpsProxy == null) {
            return ConfigurationProperties.forwardHttpsProxy();
        }
        return forwardHttpsProxy;
    }

    /**
     * Use HTTPS proxy (i.e. HTTP CONNECT) for all outbound / forwarded requests, supports TLS tunnelling of HTTPS requests
     * <p>
     * The default is null
     *
     * @param forwardHttpsProxy host and port for HTTPS proxy (i.e. HTTP CONNECT) for all outbound / forwarded requests
     */
    public Configuration forwardHttpsProxy(InetSocketAddress forwardHttpsProxy) {
        this.forwardHttpsProxy = forwardHttpsProxy;
        return this;
    }

    public InetSocketAddress forwardSocksProxy() {
        if (forwardSocksProxy == null) {
            return ConfigurationProperties.forwardSocksProxy();
        }
        return forwardSocksProxy;
    }

    /**
     * Use SOCKS proxy for all outbound / forwarded requests, support TLS tunnelling of TCP connections
     * <p>
     * The default is null
     *
     * @param forwardSocksProxy host and port for SOCKS proxy for all outbound / forwarded requests
     */
    public Configuration forwardSocksProxy(InetSocketAddress forwardSocksProxy) {
        this.forwardSocksProxy = forwardSocksProxy;
        return this;
    }

    public String forwardProxyAuthenticationUsername() {
        if (forwardProxyAuthenticationUsername == null) {
            return ConfigurationProperties.forwardProxyAuthenticationUsername();
        }
        return forwardProxyAuthenticationUsername;
    }

    /**
     * <p>Username for proxy authentication when using HTTPS proxy (i.e. HTTP CONNECT) for all outbound / forwarded requests</p>
     * <p><strong>Note:</strong> <a target="_blank" href="https://www.oracle.com/java/technologies/javase/8u111-relnotes.html">8u111 Update Release Notes</a> state that the Basic authentication scheme has been deactivated when setting up an HTTPS tunnel.  To resolve this clear or set to an empty string the following system properties: <code class="inline code">jdk.http.auth.tunneling.disabledSchemes</code> and <code class="inline code">jdk.http.auth.proxying.disabledSchemes</code>.</p>
     * <p>
     * The default is null
     *
     * @param forwardProxyAuthenticationUsername username for proxy authentication
     */
    public Configuration forwardProxyAuthenticationUsername(String forwardProxyAuthenticationUsername) {
        this.forwardProxyAuthenticationUsername = forwardProxyAuthenticationUsername;
        return this;
    }

    public String forwardProxyAuthenticationPassword() {
        if (forwardProxyAuthenticationPassword == null) {
            return ConfigurationProperties.forwardProxyAuthenticationPassword();
        }
        return forwardProxyAuthenticationPassword;
    }

    /**
     * <p>Password for proxy authentication when using HTTPS proxy (i.e. HTTP CONNECT) for all outbound / forwarded requests</p>
     * <p><strong>Note:</strong> <a target="_blank" href="https://www.oracle.com/java/technologies/javase/8u111-relnotes.html">8u111 Update Release Notes</a> state that the Basic authentication scheme has been deactivated when setting up an HTTPS tunnel.  To resolve this clear or set to an empty string the following system properties: <code class="inline code">jdk.http.auth.tunneling.disabledSchemes</code> and <code class="inline code">jdk.http.auth.proxying.disabledSchemes</code>.</p>
     * <p>
     * The default is null
     *
     * @param forwardProxyAuthenticationPassword password for proxy authentication
     */
    public Configuration forwardProxyAuthenticationPassword(String forwardProxyAuthenticationPassword) {
        this.forwardProxyAuthenticationPassword = forwardProxyAuthenticationPassword;
        return this;
    }

    public String proxyAuthenticationRealm() {
        if (proxyAuthenticationRealm == null) {
            return ConfigurationProperties.proxyAuthenticationRealm();
        }
        return proxyAuthenticationRealm;
    }

    /**
     * The authentication realm for proxy authentication to MockServer
     *
     * @param proxyAuthenticationRealm the authentication realm for proxy authentication
     */
    public Configuration proxyAuthenticationRealm(String proxyAuthenticationRealm) {
        this.proxyAuthenticationRealm = proxyAuthenticationRealm;
        return this;
    }

    public String proxyAuthenticationUsername() {
        if (proxyAuthenticationUsername == null) {
            return ConfigurationProperties.proxyAuthenticationUsername();
        }
        return proxyAuthenticationUsername;
    }

    /**
     * <p>The required username for proxy authentication to MockServer</p>
     * <p><strong>Note:</strong> <a target="_blank" href="https://www.oracle.com/java/technologies/javase/8u111-relnotes.html">8u111 Update Release Notes</a> state that the Basic authentication scheme has been deactivated when setting up an HTTPS tunnel.  To resolve this clear or set to an empty string the following system properties: <code class="inline code">jdk.http.auth.tunneling.disabledSchemes</code> and <code class="inline code">jdk.http.auth.proxying.disabledSchemes</code>.</p>
     * <p>
     * The default is ""
     *
     * @param proxyAuthenticationUsername required username for proxy authentication to MockServer
     */
    public Configuration proxyAuthenticationUsername(String proxyAuthenticationUsername) {
        this.proxyAuthenticationUsername = proxyAuthenticationUsername;
        return this;
    }

    public String proxyAuthenticationPassword() {
        if (proxyAuthenticationPassword == null) {
            return ConfigurationProperties.proxyAuthenticationPassword();
        }
        return proxyAuthenticationPassword;
    }

    /**
     * <p>The required password for proxy authentication to MockServer</p>
     * <p><strong>Note:</strong> <a target="_blank" href="https://www.oracle.com/java/technologies/javase/8u111-relnotes.html">8u111 Update Release Notes</a> state that the Basic authentication scheme has been deactivated when setting up an HTTPS tunnel.  To resolve this clear or set to an empty string the following system properties: <code class="inline code">jdk.http.auth.tunneling.disabledSchemes</code> and <code class="inline code">jdk.http.auth.proxying.disabledSchemes</code>.</p>
     * <p>
     * The default is ""
     *
     * @param proxyAuthenticationPassword required password for proxy authentication to MockServer
     */
    public Configuration proxyAuthenticationPassword(String proxyAuthenticationPassword) {
        this.proxyAuthenticationPassword = proxyAuthenticationPassword;
        return this;
    }

    public String noProxyHosts() {
        if (noProxyHosts == null) {
            return ConfigurationProperties.noProxyHosts();
        }
        return noProxyHosts;
    }

    /**
     * <p>The list of hostnames to not use the configured proxy. Several values may be present, seperated by comma (,)</p>
     * The default is ""
     *
     * @param noProxyHosts Comma-seperated list of hosts to not be proxied.
     */
    public Configuration noProxyHosts(String noProxyHosts) {
        this.noProxyHosts = noProxyHosts;
        return this;
    }

    public String livenessHttpGetPath() {
        if (livenessHttpGetPath == null) {
            return ConfigurationProperties.livenessHttpGetPath();
        }
        return livenessHttpGetPath;
    }

    /**
     * Path to support HTTP GET requests for status response (also available on PUT /mockserver/status).
     * <p>
     * If this value is not modified then only PUT /mockserver/status but is a none blank value is provided for this value then GET requests to this path will return the 200 Ok status response showing the MockServer version and bound ports.
     * <p>
     * A GET request to this path will be matched before any expectation matching or proxying of requests.
     * <p>
     * The default is ""
     *
     * @param livenessHttpGetPath path to support HTTP GET requests for status response
     */
    public Configuration livenessHttpGetPath(String livenessHttpGetPath) {
        this.livenessHttpGetPath = livenessHttpGetPath;
        return this;
    }

    public Boolean controlPlaneTLSMutualAuthenticationRequired() {
        if (controlPlaneTLSMutualAuthenticationRequired == null) {
            return ConfigurationProperties.controlPlaneTLSMutualAuthenticationRequired();
        }
        return controlPlaneTLSMutualAuthenticationRequired;
    }

    /**
     * Require mTLS (also called client authentication and two-way TLS) for all control plane requests
     *
     * @param controlPlaneTLSMutualAuthenticationRequired TLS mutual authentication for all control plane requests
     */
    public Configuration controlPlaneTLSMutualAuthenticationRequired(Boolean controlPlaneTLSMutualAuthenticationRequired) {
        this.controlPlaneTLSMutualAuthenticationRequired = controlPlaneTLSMutualAuthenticationRequired;
        return this;
    }

    public String controlPlaneTLSMutualAuthenticationCAChain() {
        if (controlPlaneTLSMutualAuthenticationCAChain == null) {
            return ConfigurationProperties.controlPlaneTLSMutualAuthenticationCAChain();
        }
        return controlPlaneTLSMutualAuthenticationCAChain;
    }

    /**
     * File system path or classpath location of custom mTLS (TLS client authentication) X.509 Certificate Chain for control plane mTLS authentication
     * <p>
     * The X.509 Certificate Chain is for trusting (i.e. signature verification of) Client X.509 Certificates, the certificate chain must be a X509 PEM file.
     * <p>
     * This certificate chain will be used for to performs mTLS (client authentication) for inbound TLS connections if controlPlaneTLSMutualAuthenticationRequired is enabled
     *
     * @param controlPlaneTLSMutualAuthenticationCAChain File system path or classpath location of custom mTLS (TLS client authentication) X.509 Certificate Chain for Trusting (i.e. signature verification of) Client X.509 Certificates
     */
    public Configuration controlPlaneTLSMutualAuthenticationCAChain(String controlPlaneTLSMutualAuthenticationCAChain) {
        fileExists(controlPlaneTLSMutualAuthenticationCAChain);
        this.controlPlaneTLSMutualAuthenticationCAChain = controlPlaneTLSMutualAuthenticationCAChain;
        return this;
    }

    public String controlPlanePrivateKeyPath() {
        if (controlPlanePrivateKeyPath == null) {
            return ConfigurationProperties.controlPlanePrivateKeyPath();
        }
        return controlPlanePrivateKeyPath;
    }

    /**
     * File system path or classpath location of a fixed custom private key for control plane connections using mTLS for authentication.
     * <p>
     * The private key must be a PKCS#8 or PKCS#1 PEM file and must be the private key corresponding to the controlPlaneX509CertificatePath X509 (public key) configuration.
     * The controlPlaneTLSMutualAuthenticationCAChain configuration must be the Certificate Authority for the corresponding X509 certificate (i.e. able to valid its signature).
     * <p>
     * To convert a PKCS#1 (i.e. default for Bouncy Castle) to a PKCS#8 the following command can be used: openssl pkcs8 -topk8 -inform PEM -in private_key_PKCS_1.pem -out private_key_PKCS_8.pem -nocrypt
     * <p>
     * This configuration will be ignored unless x509CertificatePath is also set.
     *
     * @param controlPlanePrivateKeyPath location of the PKCS#8 PEM file containing the private key
     */
    public Configuration controlPlanePrivateKeyPath(String controlPlanePrivateKeyPath) {
        fileExists(controlPlanePrivateKeyPath);
        this.controlPlanePrivateKeyPath = controlPlanePrivateKeyPath;
        return this;
    }

    public String controlPlaneX509CertificatePath() {
        if (controlPlaneX509CertificatePath == null) {
            return ConfigurationProperties.controlPlaneX509CertificatePath();
        }
        return controlPlaneX509CertificatePath;
    }

    /**
     * File system path or classpath location of a fixed custom X.509 Certificate for control plane connections using mTLS for authentication.
     * <p>
     * The certificate must be a X509 PEM file and must be the public key corresponding to the controlPlanePrivateKeyPath private key configuration.
     * The controlPlaneTLSMutualAuthenticationCAChain configuration must be the Certificate Authority for this certificate (i.e. able to valid its signature).
     * <p>
     * This configuration will be ignored unless privateKeyPath is also set.
     *
     * @param controlPlaneX509CertificatePath location of the PEM file containing the X509 certificate
     */
    public Configuration controlPlaneX509CertificatePath(String controlPlaneX509CertificatePath) {
        fileExists(controlPlaneX509CertificatePath);
        this.controlPlaneX509CertificatePath = controlPlaneX509CertificatePath;
        return this;
    }

    public Boolean controlPlaneJWTAuthenticationRequired() {
        if (controlPlaneJWTAuthenticationRequired == null) {
            return ConfigurationProperties.controlPlaneJWTAuthenticationRequired();
        }
        return controlPlaneJWTAuthenticationRequired;
    }

    /**
     * <p>
     * Require JWT authentication for all control plane requests
     * </p>
     *
     * @param controlPlaneJWTAuthenticationRequired TLS mutual authentication for all control plane requests
     */
    public Configuration controlPlaneJWTAuthenticationRequired(Boolean controlPlaneJWTAuthenticationRequired) {
        this.controlPlaneJWTAuthenticationRequired = controlPlaneJWTAuthenticationRequired;
        return this;
    }

    public String controlPlaneJWTAuthenticationJWKSource() {
        if (controlPlaneJWTAuthenticationJWKSource == null) {
            return ConfigurationProperties.controlPlaneJWTAuthenticationJWKSource();
        }
        return controlPlaneJWTAuthenticationJWKSource;
    }

    /**
     * <p>
     * JWK source used when JWT authentication is enabled for control plane requests
     * </p>
     * <p>
     * JWK source can be a file system path, classpath location or a URL
     * </p>
     * <p>
     * See: https://openid.net/specs/draft-jones-json-web-key-03.html
     * </p>
     *
     * @param controlPlaneJWTAuthenticationJWKSource file system path, classpath location or a URL of JWK source
     */
    public Configuration controlPlaneJWTAuthenticationJWKSource(String controlPlaneJWTAuthenticationJWKSource) {
        this.controlPlaneJWTAuthenticationJWKSource = controlPlaneJWTAuthenticationJWKSource;
        return this;
    }

    public String controlPlaneJWTAuthenticationExpectedAudience() {
        if (controlPlaneJWTAuthenticationExpectedAudience == null) {
            return ConfigurationProperties.controlPlaneJWTAuthenticationExpectedAudience();
        }
        return controlPlaneJWTAuthenticationExpectedAudience;
    }

    /**
     * <p>
     * Audience claim (i.e. aud) required when JWT authentication is enabled for control plane requests
     * </p>
     *
     * @param controlPlaneJWTAuthenticationExpectedAudience required value for audience claim (i.e. aud)
     */
    public Configuration controlPlaneJWTAuthenticationExpectedAudience(String controlPlaneJWTAuthenticationExpectedAudience) {
        this.controlPlaneJWTAuthenticationExpectedAudience = controlPlaneJWTAuthenticationExpectedAudience;
        return this;
    }

    public Map<String, String> controlPlaneJWTAuthenticationMatchingClaims() {
        if (controlPlaneJWTAuthenticationMatchingClaims == null) {
            return ConfigurationProperties.controlPlaneJWTAuthenticationMatchingClaims();
        }
        return controlPlaneJWTAuthenticationMatchingClaims;
    }

    /**
     * <p>
     * Matching claims expected when JWT authentication is enabled for control plane requests
     * </p>
     * <p>
     * Value should be string with comma separated key=value items, for example: scope=internal public,sub=some_subject
     * </p>
     *
     * @param controlPlaneJWTAuthenticationMatchingClaims required values for claims
     */
    public Configuration controlPlaneJWTAuthenticationMatchingClaims(Map<String, String> controlPlaneJWTAuthenticationMatchingClaims) {
        this.controlPlaneJWTAuthenticationMatchingClaims = controlPlaneJWTAuthenticationMatchingClaims;
        return this;
    }

    public Set<String> controlPlaneJWTAuthenticationRequiredClaims() {
        if (controlPlaneJWTAuthenticationRequiredClaims == null) {
            return ConfigurationProperties.controlPlaneJWTAuthenticationRequiredClaims();
        }
        return controlPlaneJWTAuthenticationRequiredClaims;
    }

    /**
     * <p>
     * Required claims that should exist (i.e. with any value) when JWT authentication is enabled for control plane requests
     * </p>
     * <p>
     * Value should be string with comma separated values, for example: scope,sub
     * </p>
     *
     * @param controlPlaneJWTAuthenticationRequiredClaims required claims
     */
    public Configuration controlPlaneJWTAuthenticationRequiredClaims(Set<String> controlPlaneJWTAuthenticationRequiredClaims) {
        this.controlPlaneJWTAuthenticationRequiredClaims = controlPlaneJWTAuthenticationRequiredClaims;
        return this;
    }

    public Boolean proactivelyInitialiseTLS() {
        if (proactivelyInitialiseTLS == null) {
            return ConfigurationProperties.proactivelyInitialiseTLS();
        }
        return proactivelyInitialiseTLS;
    }

    /**
     * <p>Proactively initialise TLS during start to ensure that if dynamicallyCreateCertificateAuthorityCertificate is enabled the Certificate Authority X.509 Certificate and Private Key will be created during start up and not when the first TLS connection is received.</p>
     * <p>This setting will also ensure any configured private key and X.509 will be loaded during start up and not when the first TLS connection is received to give immediate feedback on any related TLS configuration errors.</p>
     *
     * @param proactivelyInitialiseTLS proactively initialise TLS at startup
     */
    public Configuration proactivelyInitialiseTLS(Boolean proactivelyInitialiseTLS) {
        this.proactivelyInitialiseTLS = proactivelyInitialiseTLS;
        return this;
    }

    public boolean rebuildTLSContext() {
        return rebuildTLSContext;
    }

    public Configuration rebuildTLSContext(boolean rebuildTLSContext) {
        this.rebuildTLSContext = rebuildTLSContext;
        return this;
    }

    public boolean rebuildServerTLSContext() {
        return rebuildServerTLSContext;
    }

    public Configuration rebuildServerTLSContext(boolean rebuildServerTLSContext) {
        this.rebuildServerTLSContext = rebuildServerTLSContext;
        return this;
    }

    public String tlsProtocols() {
        if (tlsProtocols == null) {
            return ConfigurationProperties.tlsProtocols();
        }
        return tlsProtocols;
    }

    /**
     * Comma seperated list of TLS protocols, by default TLSv1,TLSv1.1,TLSv1.2
     *
     * @param tlsProtocols comma seperated list of TLS protocols
     */
    public Configuration tlsProtocols(String tlsProtocols) {
        this.tlsProtocols = tlsProtocols;
        return this;
    }

    public Boolean dynamicallyCreateCertificateAuthorityCertificate() {
        if (dynamicallyCreateCertificateAuthorityCertificate == null) {
            return ConfigurationProperties.dynamicallyCreateCertificateAuthorityCertificate();
        }
        return dynamicallyCreateCertificateAuthorityCertificate;
    }

    /**
     * Enable dynamic creation of Certificate Authority X509 certificate and private key.
     * <p>
     * Enable this property to increase the security of trusting the MockServer Certificate Authority X509 by ensuring a local dynamic value is used instead of the public value in the MockServer git repo.
     * <p>
     * These PEM files will be created and saved in the directory specified with configuration property directoryToSaveDynamicSSLCertificate.
     *
     * @param dynamicallyCreateCertificateAuthorityCertificate dynamic creation of Certificate Authority X509 certificate and private key.
     */
    public Configuration dynamicallyCreateCertificateAuthorityCertificate(Boolean dynamicallyCreateCertificateAuthorityCertificate) {
        this.dynamicallyCreateCertificateAuthorityCertificate = dynamicallyCreateCertificateAuthorityCertificate;
        return this;
    }

    public String directoryToSaveDynamicSSLCertificate() {
        if (directoryToSaveDynamicSSLCertificate == null) {
            return ConfigurationProperties.directoryToSaveDynamicSSLCertificate();
        }
        return directoryToSaveDynamicSSLCertificate;
    }

    /**
     * Directory used to save the dynamically generated Certificate Authority X.509 Certificate and Private Key.
     *
     * @param directoryToSaveDynamicSSLCertificate directory to save Certificate Authority X.509 Certificate and Private Key
     */
    public Configuration directoryToSaveDynamicSSLCertificate(String directoryToSaveDynamicSSLCertificate) {
        this.directoryToSaveDynamicSSLCertificate = directoryToSaveDynamicSSLCertificate;
        return this;
    }

    public Boolean preventCertificateDynamicUpdate() {
        if (preventCertificateDynamicUpdate == null) {
            return ConfigurationProperties.preventCertificateDynamicUpdate();
        }
        return preventCertificateDynamicUpdate;
    }

    /**
     * Prevent certificates from dynamically updating when domain list changes
     *
     * @param preventCertificateDynamicUpdate prevent certificates from dynamically updating when domain list changes
     */
    public Configuration preventCertificateDynamicUpdate(Boolean preventCertificateDynamicUpdate) {
        this.preventCertificateDynamicUpdate = preventCertificateDynamicUpdate;
        return this;
    }

    public String sslCertificateDomainName() {
        if (sslCertificateDomainName == null) {
            return ConfigurationProperties.sslCertificateDomainName();
        }
        return sslCertificateDomainName;
    }

    /**
     * The domain name for auto-generate TLS certificates
     * <p>
     * The default is "localhost"
     *
     * @param sslCertificateDomainName domain name for auto-generate TLS certificates
     */
    public Configuration sslCertificateDomainName(String sslCertificateDomainName) {
        this.sslCertificateDomainName = sslCertificateDomainName;
        return this;
    }

    public Set<String> sslSubjectAlternativeNameDomains() {
        if (sslSubjectAlternativeNameDomains == null) {
            return ConfigurationProperties.sslSubjectAlternativeNameDomains();
        }
        return sslSubjectAlternativeNameDomains;
    }

    /**
     * The Subject Alternative Name (SAN) domain names for auto-generate TLS certificates
     * <p>
     * The default is "localhost"
     *
     * @param sslSubjectAlternativeNameDomains Subject Alternative Name (SAN) domain names for auto-generate TLS certificates
     */
    public Configuration sslSubjectAlternativeNameDomains(String... sslSubjectAlternativeNameDomains) {
        this.sslSubjectAlternativeNameDomains = Sets.newConcurrentHashSet(Arrays.asList(sslSubjectAlternativeNameDomains));
        return this;
    }

    /**
     * The Subject Alternative Name (SAN) domain names for auto-generate TLS certificates
     * <p>
     * The default is "localhost"
     *
     * @param sslSubjectAlternativeNameDomains Subject Alternative Name (SAN) domain names for auto-generate TLS certificates
     */
    public Configuration sslSubjectAlternativeNameDomains(Set<String> sslSubjectAlternativeNameDomains) {
        this.sslSubjectAlternativeNameDomains = sslSubjectAlternativeNameDomains;
        return this;
    }

    public Set<String> sslSubjectAlternativeNameIps() {
        if (sslSubjectAlternativeNameIps == null) {
            return ConfigurationProperties.sslSubjectAlternativeNameIps();
        }
        return sslSubjectAlternativeNameIps;
    }

    /**
     * <p>The Subject Alternative Name (SAN) IP addresses for auto-generate TLS certificates</p>
     *
     * <p>The default is 127.0.0.1, 0.0.0.0</p>
     *
     * @param sslSubjectAlternativeNameIps Subject Alternative Name (SAN) IP addresses for auto-generate TLS certificates
     */
    public Configuration sslSubjectAlternativeNameIps(String... sslSubjectAlternativeNameIps) {
        sslSubjectAlternativeNameIps(Sets.newConcurrentHashSet(Arrays.asList(sslSubjectAlternativeNameIps)));
        return this;
    }

    /**
     * <p>The Subject Alternative Name (SAN) IP addresses for auto-generate TLS certificates</p>
     *
     * <p>The default is 127.0.0.1, 0.0.0.0</p>
     *
     * @param sslSubjectAlternativeNameIps Subject Alternative Name (SAN) IP addresses for auto-generate TLS certificates
     */
    public Configuration sslSubjectAlternativeNameIps(Set<String> sslSubjectAlternativeNameIps) {
        this.sslSubjectAlternativeNameIps = sslSubjectAlternativeNameIps;
        return this;
    }

    public String certificateAuthorityPrivateKey() {
        if (certificateAuthorityPrivateKey == null) {
            return ConfigurationProperties.certificateAuthorityPrivateKey();
        }
        return certificateAuthorityPrivateKey;
    }

    /**
     * File system path or classpath location of custom Private Key for Certificate Authority for TLS, the private key must be a PKCS#8 or PKCS#1 PEM file and must match the certificateAuthorityCertificate
     * To convert a PKCS#1 (i.e. default for Bouncy Castle) to a PKCS#8 the following command can be used: openssl pkcs8 -topk8 -inform PEM -in private_key_PKCS_1.pem -out private_key_PKCS_8.pem -nocrypt
     *
     * @param certificateAuthorityPrivateKey location of the PEM file containing the certificate authority private key
     */
    public Configuration certificateAuthorityPrivateKey(String certificateAuthorityPrivateKey) {
        this.certificateAuthorityPrivateKey = certificateAuthorityPrivateKey;
        return this;
    }

    public String certificateAuthorityCertificate() {
        if (certificateAuthorityCertificate == null) {
            return ConfigurationProperties.certificateAuthorityCertificate();
        }
        return certificateAuthorityCertificate;
    }

    /**
     * File system path or classpath location of custom X.509 Certificate for Certificate Authority for TLS, the certificate must be a X509 PEM file and must match the certificateAuthorityPrivateKey
     *
     * @param certificateAuthorityCertificate location of the PEM file containing the certificate authority X509 certificate
     */
    public Configuration certificateAuthorityCertificate(String certificateAuthorityCertificate) {
        this.certificateAuthorityCertificate = certificateAuthorityCertificate;
        return this;
    }

    public String privateKeyPath() {
        if (privateKeyPath == null) {
            return ConfigurationProperties.privateKeyPath();
        }
        return privateKeyPath;
    }

    /**
     * File system path or classpath location of a fixed custom private key for TLS connections into MockServer.
     * <p>
     * The private key must be a PKCS#8 or PKCS#1 PEM file and must be the private key corresponding to the x509CertificatePath X509 (public key) configuration.
     * The certificateAuthorityCertificate configuration must be the Certificate Authority for the corresponding X509 certificate (i.e. able to valid its signature), see: x509CertificatePath.
     * <p>
     * To convert a PKCS#1 (i.e. default for Bouncy Castle) to a PKCS#8 the following command can be used: openssl pkcs8 -topk8 -inform PEM -in private_key_PKCS_1.pem -out private_key_PKCS_8.pem -nocrypt
     * <p>
     * This configuration will be ignored unless x509CertificatePath is also set.
     *
     * @param privateKeyPath location of the PKCS#8 PEM file containing the private key
     */
    public Configuration privateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
        return this;
    }

    public String x509CertificatePath() {
        if (x509CertificatePath == null) {
            return ConfigurationProperties.x509CertificatePath();
        }
        return x509CertificatePath;
    }

    /**
     * File system path or classpath location of a fixed custom X.509 Certificate for TLS connections into MockServer.
     * <p>
     * The certificate must be a X509 PEM file and must be the public key corresponding to the privateKeyPath private key configuration.
     * The certificateAuthorityCertificate configuration must be the Certificate Authority for this certificate (i.e. able to valid its signature).
     * <p>
     * This configuration will be ignored unless privateKeyPath is also set.
     *
     * @param x509CertificatePath location of the PEM file containing the X509 certificate
     */
    public Configuration x509CertificatePath(String x509CertificatePath) {
        this.x509CertificatePath = x509CertificatePath;
        return this;
    }

    public Boolean tlsMutualAuthenticationRequired() {
        if (tlsMutualAuthenticationRequired == null) {
            return ConfigurationProperties.tlsMutualAuthenticationRequired();
        }
        return tlsMutualAuthenticationRequired;
    }

    /**
     * Require mTLS (also called client authentication and two-way TLS) for all TLS connections / HTTPS requests to MockServer
     *
     * @param tlsMutualAuthenticationRequired TLS mutual authentication
     */
    public Configuration tlsMutualAuthenticationRequired(Boolean tlsMutualAuthenticationRequired) {
        this.tlsMutualAuthenticationRequired = tlsMutualAuthenticationRequired;
        return this;
    }

    public String tlsMutualAuthenticationCertificateChain() {
        if (tlsMutualAuthenticationCertificateChain == null) {
            return ConfigurationProperties.tlsMutualAuthenticationCertificateChain();
        }
        return tlsMutualAuthenticationCertificateChain;
    }

    /**
     * File system path or classpath location of custom mTLS (TLS client authentication) X.509 Certificate Chain for trusting (i.e. signature verification of) Client X.509 Certificates, the certificate chain must be a X509 PEM file.
     * <p>
     * This certificate chain will be used if MockServer performs mTLS (client authentication) for inbound TLS connections because tlsMutualAuthenticationRequired is enabled
     *
     * @param tlsMutualAuthenticationCertificateChain File system path or classpath location of custom mTLS (TLS client authentication) X.509 Certificate Chain for Trusting (i.e. signature verification of) Client X.509 Certificates
     */
    public Configuration tlsMutualAuthenticationCertificateChain(String tlsMutualAuthenticationCertificateChain) {
        fileExists(tlsMutualAuthenticationCertificateChain);
        this.tlsMutualAuthenticationCertificateChain = tlsMutualAuthenticationCertificateChain;
        return this;
    }

    public ForwardProxyTLSX509CertificatesTrustManager forwardProxyTLSX509CertificatesTrustManagerType() {
        if (forwardProxyTLSX509CertificatesTrustManagerType == null) {
            return ConfigurationProperties.forwardProxyTLSX509CertificatesTrustManagerType();
        }
        return forwardProxyTLSX509CertificatesTrustManagerType;
    }

    /**
     * Configure trusted set of certificates for forwarded or proxied requests.
     * <p>
     * MockServer will only be able to establish a TLS connection to endpoints that have a trusted X509 certificate according to the trust manager type, as follows:
     * <p>
     * <p>
     * ALL - Insecure will trust all X509 certificates and not perform host name verification.
     * JVM - Will trust all X509 certificates trust by the JVM.
     * CUSTOM - Will trust all X509 certificates specified in forwardProxyTLSCustomTrustX509Certificates configuration value.
     *
     * @param forwardProxyTLSX509CertificatesTrustManagerType trusted set of certificates for forwarded or proxied requests, allowed values: ALL, JVM, CUSTOM.
     */
    public Configuration forwardProxyTLSX509CertificatesTrustManagerType(ForwardProxyTLSX509CertificatesTrustManager forwardProxyTLSX509CertificatesTrustManagerType) {
        this.forwardProxyTLSX509CertificatesTrustManagerType = forwardProxyTLSX509CertificatesTrustManagerType;
        return this;
    }

    public String forwardProxyTLSCustomTrustX509Certificates() {
        if (forwardProxyTLSCustomTrustX509Certificates == null) {
            return ConfigurationProperties.forwardProxyTLSCustomTrustX509Certificates();
        }
        return forwardProxyTLSCustomTrustX509Certificates;
    }

    /**
     * File system path or classpath location of custom file for trusted X509 Certificate Authority roots for forwarded or proxied requests, the certificate chain must be a X509 PEM file.
     * <p>
     * MockServer will only be able to establish a TLS connection to endpoints that have an X509 certificate chain that is signed by one of the provided custom
     * certificates, i.e. where a path can be established from the endpoints X509 certificate to one or more of the custom X509 certificates provided.
     *
     * @param forwardProxyTLSCustomTrustX509Certificates custom set of trusted X509 certificate authority roots for forwarded or proxied requests in PEM format.
     */
    public Configuration forwardProxyTLSCustomTrustX509Certificates(String forwardProxyTLSCustomTrustX509Certificates) {
        fileExists(forwardProxyTLSCustomTrustX509Certificates);
        this.forwardProxyTLSCustomTrustX509Certificates = forwardProxyTLSCustomTrustX509Certificates;
        return this;
    }

    public String forwardProxyPrivateKey() {
        if (forwardProxyPrivateKey == null) {
            return ConfigurationProperties.forwardProxyPrivateKey();
        }
        return forwardProxyPrivateKey;
    }

    /**
     * File system path or classpath location of custom Private Key for proxied TLS connections out of MockServer, the private key must be a PKCS#8 or PKCS#1 PEM file
     * <p>
     * To convert a PKCS#1 (i.e. default for Bouncy Castle) to a PKCS#8 the following command can be used: openssl pkcs8 -topk8 -inform PEM -in private_key_PKCS_1.pem -out private_key_PKCS_8.pem -nocrypt
     * <p>
     * This private key will be used if MockServer needs to perform mTLS (client authentication) for outbound TLS connections.
     *
     * @param forwardProxyPrivateKey location of the PEM file containing the private key
     */
    public Configuration forwardProxyPrivateKey(String forwardProxyPrivateKey) {
        fileExists(forwardProxyPrivateKey);
        this.forwardProxyPrivateKey = forwardProxyPrivateKey;
        return this;
    }

    public String forwardProxyCertificateChain() {
        if (forwardProxyCertificateChain == null) {
            return ConfigurationProperties.forwardProxyCertificateChain();
        }
        return forwardProxyCertificateChain;
    }

    /**
     * File system path or classpath location of custom mTLS (TLS client authentication) X.509 Certificate Chain for Trusting (i.e. signature verification of) Client X.509 Certificates, the certificate chain must be a X509 PEM file.
     * <p>
     * This certificate chain will be used if MockServer needs to perform mTLS (client authentication) for outbound TLS connections.
     *
     * @param forwardProxyCertificateChain location of the PEM file containing the certificate chain
     */
    public Configuration forwardProxyCertificateChain(String forwardProxyCertificateChain) {
        fileExists(forwardProxyCertificateChain);
        this.forwardProxyCertificateChain = forwardProxyCertificateChain;
        return this;
    }

    public void addSubjectAlternativeName(String host) {
        if (isNotBlank(host)) {
            String hostWithoutPort = substringBefore(host, ":");
            if (isNotBlank(hostWithoutPort)) {
                if (InetAddresses.isInetAddress(hostWithoutPort)) {
                    addSslSubjectAlternativeNameIps(hostWithoutPort);
                } else {
                    addSslSubjectAlternativeNameDomains(hostWithoutPort);
                }
            }
        }
    }

    public void addSslSubjectAlternativeNameIps(String... additionalSubjectAlternativeNameIps) {
        boolean subjectAlternativeIpsModified = false;
        Set<String> sslSubjectAlternativeNameIps = sslSubjectAlternativeNameIps();
        for (String subjectAlternativeIp : additionalSubjectAlternativeNameIps) {
            if (sslSubjectAlternativeNameIps.add(subjectAlternativeIp.trim())) {
                subjectAlternativeIpsModified = true;
            }
        }
        if (subjectAlternativeIpsModified) {
            rebuildServerTLSContext(true);
            sslSubjectAlternativeNameIps(sslSubjectAlternativeNameIps);
        }
    }

    public void clearSslSubjectAlternativeNameIps() {
        sslSubjectAlternativeNameIps.clear();
        rebuildServerTLSContext(true);
    }

    public void addSslSubjectAlternativeNameDomains(String... additionalSubjectAlternativeNameDomains) {
        boolean subjectAlternativeDomainsModified = false;
        Set<String> sslSubjectAlternativeNameDomains = sslSubjectAlternativeNameDomains();
        for (String subjectAlternativeDomain : additionalSubjectAlternativeNameDomains) {
            if (sslSubjectAlternativeNameDomains.add(subjectAlternativeDomain.trim())) {
                subjectAlternativeDomainsModified = true;
            }
        }
        if (subjectAlternativeDomainsModified) {
            rebuildServerTLSContext(true);
            sslSubjectAlternativeNameDomains(sslSubjectAlternativeNameDomains);
        }
    }

    public void clearSslSubjectAlternativeNameDomains() {
        sslSubjectAlternativeNameDomains.clear();
        rebuildServerTLSContext(true);
    }
    
    public int ringBufferSize() {
        return nextPowerOfTwo(maxLogEntries());
    }
    
    private int nextPowerOfTwo(int value) {
        for (int i = 0; i < 16; i++) {
            double powOfTwo = Math.pow(2, i);
            if (powOfTwo > value) {
                return (int) powOfTwo;
            }
        }
        return (int) Math.pow(2, 16);
    }
}
