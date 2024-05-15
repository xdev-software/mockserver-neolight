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
package software.xdev.mockserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.model.Header.header;
import static software.xdev.mockserver.model.NottableSchemaString.schemaString;
import static software.xdev.mockserver.model.NottableString.string;
import static software.xdev.mockserver.model.SocketAddress.Scheme.HTTP;
import static software.xdev.mockserver.model.SocketAddress.Scheme.HTTPS;

@SuppressWarnings({"rawtypes", "UnusedReturnValue"})
public class HttpRequest extends RequestDefinition implements HttpMessage<HttpRequest, Body> {
    private int hashCode;
    private NottableString method = string("");
    private NottableString path = string("");
    private Parameters pathParameters;
    private Parameters queryStringParameters;
    private Body body = null;
    private Headers headers;
    private Cookies cookies;
    private Boolean keepAlive = null;
    private Boolean secure = null;
    private Protocol protocol = null;
    private Integer streamId = null;
    private List<X509Certificate> clientCertificateChain;
    private SocketAddress socketAddress;
    private String localAddress;
    private String remoteAddress;

    public static HttpRequest request() {
        return new HttpRequest();
    }

    public static HttpRequest request(String path) {
        return new HttpRequest().withPath(path);
    }

    public Boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * Match on whether the request was made using an HTTP persistent connection, also called HTTP keep-alive, or HTTP connection reuse
     *
     * @param isKeepAlive true if the request was made with an HTTP persistent connection
     */
    public HttpRequest withKeepAlive(Boolean isKeepAlive) {
        this.keepAlive = isKeepAlive;
        this.hashCode = 0;
        return this;
    }

    public Boolean isSecure() {
        if (socketAddress != null && socketAddress.getScheme() != null) {
            if (socketAddress.getScheme() == SocketAddress.Scheme.HTTPS) {
                secure = true;
                this.hashCode = 0;
            }
        }
        return secure;
    }

    /**
     * Match on whether the request was made over TLS or SSL (i.e. HTTPS)
     *
     * @param isSecure true if the request was made with TLS or SSL
     */
    public HttpRequest withSecure(Boolean isSecure) {
        this.secure = isSecure;
        if (socketAddress != null && socketAddress.getScheme() != null) {
            if (socketAddress.getScheme() == SocketAddress.Scheme.HTTPS) {
                secure = true;
            }
        }
        this.hashCode = 0;
        return this;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Match on whether the request was made over HTTP or HTTP2
     *
     * @param protocol used to indicate HTTP or HTTP2
     */
    public HttpRequest withProtocol(Protocol protocol) {
        this.protocol = protocol;
        this.hashCode = 0;
        return this;
    }

    public Integer getStreamId() {
        return streamId;
    }

    /**
     * HTTP2 stream id request was received on
     *
     * @param streamId HTTP2 stream id request was received on
     */
    public HttpRequest withStreamId(Integer streamId) {
        this.streamId = streamId;
        this.hashCode = 0;
        return this;
    }

    public List<X509Certificate> getClientCertificateChain() {
        return clientCertificateChain;
    }

    public HttpRequest withClientCertificateChain(List<X509Certificate> clientCertificateChain) {
        this.clientCertificateChain = clientCertificateChain;
        this.hashCode = 0;
        return this;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    /**
     * Specify remote address if the remote address can't be derived from the host header,
     * if no value is specified the host header will be used to determine remote address
     *
     * @param socketAddress the remote address to send request to
     */
    public HttpRequest withSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
        if (socketAddress != null && socketAddress.getScheme() != null) {
            if (socketAddress.getScheme() == SocketAddress.Scheme.HTTPS) {
                secure = true;
            }
        }
        this.hashCode = 0;
        return this;
    }

    /**
     * Specify remote address if the remote address can't be derived from the host header,
     * if no value is specified the host header will be used to determine remote address
     *
     * @param host   the remote host or ip to send request to
     * @param port   the remote port to send request to
     * @param scheme the scheme to use for remote socket
     */
    public HttpRequest withSocketAddress(String host, Integer port, SocketAddress.Scheme scheme) {
        this.socketAddress = new SocketAddress()
            .withHost(host)
            .withPort(port)
            .withScheme(scheme);
        this.hashCode = 0;
        return this;
    }

    /**
     * Specify remote address by attempting to derive it from the host header and / or the specified port
     *
     * @param host the remote host or ip to send request to
     * @param port the remote port to send request to
     */
    public HttpRequest withSocketAddress(String host, Integer port) {
        withSocketAddress(secure, host, port);
        this.hashCode = 0;
        return this;
    }

    /**
     * Specify remote address by attempting to derive it from the host header
     */
    public HttpRequest withSocketAddressFromHostHeader() {
        withSocketAddress(secure, getFirstHeader("host"), null);
        this.hashCode = 0;
        return this;
    }

    /**
     * Specify remote address by attempting to derive it from the host header and / or the specified port
     *
     * @param isSecure true if the request was made with TLS or SSL
     * @param host     the remote host or ip to send request to
     * @param port     the remote port to send request to
     */
    public HttpRequest withSocketAddress(Boolean isSecure, String host, Integer port) {
        if (isNotBlank(host)) {
            String[] hostParts = host.split(":");
            boolean secure = Boolean.TRUE.equals(isSecure);
            if (hostParts.length > 1) {
                withSocketAddress(hostParts[0], port != null ? port : Integer.parseInt(hostParts[1]), secure ? HTTPS : HTTP);
            } else if (secure) {
                withSocketAddress(host, port != null ? port : 443, HTTPS);
            } else {
                withSocketAddress(host, port != null ? port : 80, HTTP);
            }
        }
        this.hashCode = 0;
        return this;
    }

    public HttpRequest withLocalAddress(String localAddress) {
        this.localAddress = localAddress;
        this.hashCode = 0;
        return this;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public HttpRequest withRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.hashCode = 0;
        return this;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * The HTTP method to match on such as "GET" or "POST"
     *
     * @param method the HTTP method such as "GET" or "POST"
     */
    public HttpRequest withMethod(String method) {
        return withMethod(string(method));
    }

    /**
     * The HTTP method to match on as a JSON Schema for example:
     * <pre>
     * {
     *     "type": "string",
     *     "minLength": 2,
     *     "maxLength": 3
     * }
     *
     * or
     *
     * {
     *     "type": "string",
     *     "pattern": "^P.{2,3}$"
     * }
     *
     * or
     *
     * {
     *     "type": "string",
     *     "format": "ipv4"
     * }
     * </pre>
     * <p>
     * For full details of JSON Schema see, https://json-schema.org/understanding-json-schema/reference/string.html
     *
     * @param method the HTTP method to match on as a JSON Schema
     */
    public HttpRequest withMethodSchema(String method) {
        withMethod(schemaString(method));
        return this;
    }

    /**
     * The HTTP method all method except a specific value using the "not" operator,
     * for example this allows operations such as not("GET")
     *
     * @param method the HTTP method to not match on not("GET") or not("POST")
     */
    public HttpRequest withMethod(NottableString method) {
        this.method = method;
        this.hashCode = 0;
        return this;
    }

    public NottableString getMethod() {
        return method;
    }

    public String getMethod(String defaultValue) {
        if (isBlank(method.getValue())) {
            return defaultValue;
        } else {
            return method.getValue();
        }
    }

    /**
     * The path to match on such as "/some_mocked_path" any servlet context path is ignored for matching and should not be specified here
     * regex values are also supported such as ".*_path", see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
     * for full details of the supported regex syntax
     *
     * @param path the path such as "/some_mocked_path" or a regex
     */
    public HttpRequest withPath(String path) {
        withPath(string(path));
        return this;
    }

    /**
     * The path to not match on for example not("/some_mocked_path") with match any path not equal to "/some_mocked_path",
     * the servlet context path is ignored for matching and should not be specified hereregex values are also supported
     * such as not(".*_path"), see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html for full details
     * of the supported regex syntax
     *
     * @param path the path to not match on such as not("/some_mocked_path") or not(".*_path")
     */
    public HttpRequest withPath(NottableString path) {
        this.path = path;
        this.hashCode = 0;
        return this;
    }

    /**
     * The path to match on as a JSON Schema for example:
     * <pre>
     * {
     *     "type": "string",
     *     "minLength": 2,
     *     "maxLength": 3
     * }
     *
     * or
     *
     * {
     *     "type": "string",
     *     "pattern": "^simp.{2}$"
     * }
     *
     * or
     *
     * {
     *     "type": "string",
     *     "format": "ipv4"
     * }
     * </pre>
     * <p>
     * For full details of JSON Schema see, https://json-schema.org/understanding-json-schema/reference/string.html
     *
     * @param path the path to match on as a JSON Schema
     */
    public HttpRequest withPathSchema(String path) {
        withPath(schemaString(path));
        return this;
    }

    public NottableString getPath() {
        return path;
    }

    public boolean matches(final String method) {
        return this.method.getValue().equals(method);
    }

    public boolean matches(final String method, final String... paths) {
        boolean matches = false;
        for (String path : paths) {
            matches = this.method.getValue().equals(method) && this.path.getValue().equals(path);
            if (matches) {
                break;
            }
        }
        return matches;
    }

    public Parameters getPathParameters() {
        return this.pathParameters;
    }

    private Parameters getOrCreatePathParameters() {
        if (this.pathParameters == null) {
            this.pathParameters = new Parameters();
            this.hashCode = 0;
        }
        return this.pathParameters;
    }

    public HttpRequest withPathParameters(Parameters parameters) {
        if (parameters == null || parameters.isEmpty()) {
            this.pathParameters = null;
        } else {
            this.pathParameters = parameters;
        }
        this.hashCode = 0;
        return this;
    }

    /**
     * The path parameter to match on as a list of Parameter objects where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the list of Parameter objects where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withPathParameters(List<Parameter> parameters) {
        getOrCreatePathParameters().withEntries(parameters);
        this.hashCode = 0;
        return this;
    }

    /**
     * The path parameter to match on as a varags Parameter objects where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the varags Parameter objects where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withPathParameters(Parameter... parameters) {
        getOrCreatePathParameters().withEntries(parameters);
        this.hashCode = 0;
        return this;
    }

    /**
     * The path parameter to match on as a Map&lt;String, List&lt;String&gt;&gt; where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the Map&lt;String, List&lt;String&gt;&gt; object where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withPathParameters(Map<String, List<String>> parameters) {
        getOrCreatePathParameters().withEntries(parameters);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one path parameter to match on as a Parameter object where the parameter values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameter the Parameter object which can have a values list of strings or regular expressions
     */
    public HttpRequest withPathParameter(Parameter parameter) {
        getOrCreatePathParameters().withEntry(parameter);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one path parameter to match which can specified using plain strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param name   the parameter name
     * @param values the parameter values which can be a varags of strings or regular expressions
     */
    public HttpRequest withPathParameter(String name, String... values) {
        if (values.length == 0) {
            values = new String[]{".*"};
        }
        getOrCreatePathParameters().withEntry(name, values);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one path parameter to match which the values are JSON schema i.e. "{ \"type\": \"string\", \"pattern\": \"^someV[a-z]{4}$\" }"
     * (for more details of the supported JSON schema see https://json-schema.org)
     *
     * @param name   the parameter name
     * @param values the parameter values which can be a varags of JSON schemas
     */
    public HttpRequest withSchemaPathParameter(String name, String... values) {
        if (values.length == 0) {
            values = new String[]{".*"};
        }
        getOrCreatePathParameters().withEntry(string(name), Arrays.stream(values).map(NottableSchemaString::schemaString).toArray(NottableString[]::new));
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one path parameter to match on or to not match on using the NottableString, each NottableString can either be a positive matching
     * value, such as string("match"), or a value to not match on, such as not("do not match"), the string values passed to the NottableString
     * can also be a plain string or a regex (for more details of the supported regex syntax
     * see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param name   the parameter name as a NottableString
     * @param values the parameter values which can be a varags of NottableStrings
     */
    public HttpRequest withPathParameter(NottableString name, NottableString... values) {
        if (values.length == 0) {
            values = new NottableString[]{string(".*")};
        }
        getOrCreatePathParameters().withEntry(name, values);
        this.hashCode = 0;
        return this;
    }

    public List<Parameter> getPathParameterList() {
        if (this.pathParameters != null) {
            return this.pathParameters.getEntries();
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unused")
    public boolean hasPathParameter(String name, String value) {
        if (this.pathParameters != null) {
            return this.pathParameters.containsEntry(name, value);
        } else {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public boolean hasPathParameter(NottableString name, NottableString value) {
        if (this.pathParameters != null) {
            return this.pathParameters.containsEntry(name, value);
        } else {
            return false;
        }
    }

    public String getFirstPathParameter(String name) {
        if (this.pathParameters != null) {
            return this.pathParameters.getFirstValue(name);
        } else {
            return "";
        }
    }

    public Parameters getQueryStringParameters() {
        return this.queryStringParameters;
    }

    private Parameters getOrCreateQueryStringParameters() {
        if (this.queryStringParameters == null) {
            this.queryStringParameters = new Parameters();
            this.hashCode = 0;
        }
        return this.queryStringParameters;
    }

    public HttpRequest withQueryStringParameters(Parameters parameters) {
        if (parameters == null || parameters.isEmpty()) {
            this.queryStringParameters = null;
        } else {
            this.queryStringParameters = parameters;
        }
        this.hashCode = 0;
        return this;
    }

    /**
     * The query string parameters to match on as a list of Parameter objects where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the list of Parameter objects where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withQueryStringParameters(List<Parameter> parameters) {
        getOrCreateQueryStringParameters().withEntries(parameters);
        this.hashCode = 0;
        return this;
    }

    /**
     * The query string parameters to match on as a varags Parameter objects where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the varags Parameter objects where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withQueryStringParameters(Parameter... parameters) {
        getOrCreateQueryStringParameters().withEntries(parameters);
        this.hashCode = 0;
        return this;
    }

    /**
     * The query string parameters to match on as a Map&lt;String, List&lt;String&gt;&gt; where the values or keys of each parameter can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameters the Map&lt;String, List&lt;String&gt;&gt; object where the values or keys of each parameter can be either a string or a regex
     */
    public HttpRequest withQueryStringParameters(Map<String, List<String>> parameters) {
        getOrCreateQueryStringParameters().withEntries(parameters);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one query string parameter to match on as a Parameter object where the parameter values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param parameter the Parameter object which can have a values list of strings or regular expressions
     */
    public HttpRequest withQueryStringParameter(Parameter parameter) {
        getOrCreateQueryStringParameters().withEntry(parameter);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one query string parameter to match which the values are plain strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param name   the parameter name
     * @param values the parameter values which can be a varags of strings or regular expressions
     */
    public HttpRequest withQueryStringParameter(String name, String... values) {
        getOrCreateQueryStringParameters().withEntry(name, values);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one query string parameter to match which the values are JSON schema i.e. "{ \"type\": \"string\", \"pattern\": \"^someV[a-z]{4}$\" }"
     * (for more details of the supported JSON schema see https://json-schema.org)
     *
     * @param name   the parameter name
     * @param values the parameter values which can be a varags of JSON schemas
     */
    public HttpRequest withSchemaQueryStringParameter(String name, String... values) {
        if (values.length == 0) {
            values = new String[]{".*"};
        }
        getOrCreateQueryStringParameters().withEntry(string(name), Arrays.stream(values).map(NottableSchemaString::schemaString).toArray(NottableString[]::new));
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one query string parameter to match on or to not match on using the NottableString, each NottableString can either be a positive matching
     * value, such as string("match"), or a value to not match on, such as not("do not match"), the string values passed to the NottableString
     * can also be a plain string or a regex (for more details of the supported regex syntax
     * see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param name   the parameter name as a NottableString
     * @param values the parameter values which can be a varags of NottableStrings
     */
    public HttpRequest withQueryStringParameter(NottableString name, NottableString... values) {
        if (values.length == 0) {
            values = new NottableString[]{string(".*")};
        }
        getOrCreateQueryStringParameters().withEntry(name, values);
        this.hashCode = 0;
        return this;
    }

    public List<Parameter> getQueryStringParameterList() {
        if (this.queryStringParameters != null) {
            return this.queryStringParameters.getEntries();
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unused")
    public boolean hasQueryStringParameter(String name, String value) {
        if (this.queryStringParameters != null) {
            return this.queryStringParameters.containsEntry(name, value);
        } else {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public boolean hasQueryStringParameter(NottableString name, NottableString value) {
        if (this.queryStringParameters != null) {
            return this.queryStringParameters.containsEntry(name, value);
        } else {
            return false;
        }
    }

    public String getFirstQueryStringParameter(String name) {
        if (this.queryStringParameters != null) {
            return this.queryStringParameters.getFirstValue(name);
        } else {
            return "";
        }
    }

    /**
     * The exact string body to match on such as "this is an exact string body"
     *
     * @param body the body on such as "this is an exact string body"
     */
    public HttpRequest withBody(String body) {
        this.body = new StringBody(body);
        this.hashCode = 0;
        return this;
    }

    /**
     * The exact string body to match on such as "this is an exact string body"
     *
     * @param body    the body on such as "this is an exact string body"
     * @param charset character set the string will be encoded in
     */
    public HttpRequest withBody(String body, Charset charset) {
        if (body != null) {
            this.body = new StringBody(body, charset);
            this.hashCode = 0;
        }
        return this;
    }

    /**
     * The body to match on as binary data such as a pdf or image
     *
     * @param body a byte array
     */
    public HttpRequest withBody(byte[] body) {
        this.body = new BinaryBody(body);
        this.hashCode = 0;
        return this;
    }

    /**
     * The body match rules on such as using one of the Body subclasses as follows:
     * <p>
     * exact string match:
     * - exact("this is an exact string body");
     * <p>
     * or
     * <p>
     * - new StringBody("this is an exact string body")
     * <p>
     * regular expression match:
     * - regex("username[a-z]{4}");
     * <p>
     * or
     * <p>
     * - new RegexBody("username[a-z]{4}");
     * <p>
     * json match:
     * - json("{username: 'foo', password: 'bar'}");
     * <p>
     * or
     * <p>
     * - json("{username: 'foo', password: 'bar'}", MatchType.STRICT);
     * <p>
     * or
     * <p>
     * - new JsonBody("{username: 'foo', password: 'bar'}");
     * <p>
     * json schema match:
     * - jsonSchema("{type: 'object', properties: { 'username': { 'type': 'string' }, 'password': { 'type': 'string' } }, 'required': ['username', 'password']}");
     * <p>
     * or
     * <p>
     * - jsonSchemaFromResource("org/mockserver/model/loginSchema.json");
     * <p>
     * or
     * <p>
     * - new JsonSchemaBody("{type: 'object', properties: { 'username': { 'type': 'string' }, 'password': { 'type': 'string' } }, 'required': ['username', 'password']}");
     * <p>
     * xpath match:
     * - xpath("/element[key = 'some_key' and value = 'some_value']");
     * <p>
     * or
     * <p>
     * - new XPathBody("/element[key = 'some_key' and value = 'some_value']");
     * <p>
     * body parameter match:
     * - params(
     * param("name_one", "value_one_one", "value_one_two")
     * param("name_two", "value_two")
     * );
     * <p>
     * or
     * <p>
     * - new ParameterBody(
     * new Parameter("name_one", "value_one_one", "value_one_two")
     * new Parameter("name_two", "value_two")
     * );
     * <p>
     * binary match:
     * - binary(IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("example.pdf"), 1024));
     * <p>
     * or
     * <p>
     * - new BinaryBody(IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("example.pdf"), 1024));
     * <p>
     * for more details of the supported regular expression syntax see <a href="http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html</a>
     * for more details of the supported json syntax see <a href="http://jsonassert.skyscreamer.org">http://jsonassert.skyscreamer.org</a>
     * for more details of the supported json schema syntax see <a href="http://json-schema.org/">http://json-schema.org/</a>
     * for more detail of XPath syntax see <a href="http://saxon.sourceforge.net/saxon6.5.3/expressions.html">http://saxon.sourceforge.net/saxon6.5.3/expressions.html</a>
     *
     * @param body an instance of one of the Body subclasses including StringBody, ParameterBody or BinaryBody
     */
    public HttpRequest withBody(Body body) {
        this.body = body;
        this.hashCode = 0;
        return this;
    }

    public Body getBody() {
        return body;
    }

    @JsonIgnore
    public byte[] getBodyAsRawBytes() {
        return this.body != null ? this.body.getRawBytes() : new byte[0];
    }

    @JsonIgnore
    public String getBodyAsString() {
        if (body != null) {
            return body.toString();
        } else {
            return null;
        }
    }

    @JsonIgnore
    public String getBodyAsJsonOrXmlString() {
        if (body != null) {
            if (body instanceof StringBody) {
                // if it should be json (and it has been validated i.e. control plane request)
                // assume the Content-Type header was forgotten so should be parsed as json
                return new String(body.toString().getBytes(MediaType.parse(getFirstHeader(CONTENT_TYPE.toString())).getCharsetOrDefault()), StandardCharsets.UTF_8);
            } else {
                return getBodyAsString();
            }
        } else {
            return null;
        }
    }

    public Headers getHeaders() {
        return this.headers;
    }

    private Headers getOrCreateHeaders() {
        if (this.headers == null) {
            this.headers = new Headers();
            this.hashCode = 0;
        }
        return this.headers;
    }

    public HttpRequest withHeaders(Headers headers) {
        if (headers == null || headers.isEmpty()) {
            this.headers = null;
        } else {
            this.headers = headers;
        }
        this.hashCode = 0;
        return this;
    }

    /**
     * The headers to match on as a list of Header objects where the values or keys of each header can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param headers the list of Header objects where the values or keys of each header can be either a string or a regex
     */
    public HttpRequest withHeaders(List<Header> headers) {
        getOrCreateHeaders().withEntries(headers);
        this.hashCode = 0;
        return this;
    }

    /**
     * The headers to match on as a varags of Header objects where the values or keys of each header can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param headers the varags of Header objects where the values or keys of each header can be either a string or a regex
     */
    public HttpRequest withHeaders(Header... headers) {
        getOrCreateHeaders().withEntries(headers);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one header to match on as a Header object where the header values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param header the Header object which can have a values list of strings or regular expressions
     */
    public HttpRequest withHeader(Header header) {
        getOrCreateHeaders().withEntry(header);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one header to match which can specified using plain strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param name   the header name
     * @param values the header values which can be a varags of strings or regular expressions
     */
    public HttpRequest withHeader(String name, String... values) {
        if (values.length == 0) {
            values = new String[]{".*"};
        }
        getOrCreateHeaders().withEntry(header(name, values));
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one header to match which the values are JSON schema i.e. "{ \"type\": \"string\", \"pattern\": \"^someV[a-z]{4}$\" }"
     * (for more details of the supported JSON schema see https://json-schema.org)
     *
     * @param name   the header name
     * @param values the header values which can be a varags of JSON schemas
     */
    public HttpRequest withSchemaHeader(String name, String... values) {
        if (values.length == 0) {
            values = new String[]{".*"};
        }
        getOrCreateHeaders().withEntry(header(string(name), Arrays.stream(values).map(NottableSchemaString::schemaString).toArray(NottableString[]::new)));
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one header to match on or to not match on using the NottableString, each NottableString can either be a positive matching value,
     * such as string("match"), or a value to not match on, such as not("do not match"), the string values passed to the NottableString
     * can also be a plain string or a regex (for more details of the supported regex syntax
     * see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param name   the header name as a NottableString
     * @param values the header values which can be a varags of NottableStrings
     */
    public HttpRequest withHeader(NottableString name, NottableString... values) {
        if (values.length == 0) {
            values = new NottableString[]{string(".*")};
        }
        getOrCreateHeaders().withEntry(header(name, values));
        this.hashCode = 0;
        return this;
    }

    public HttpRequest withContentType(MediaType mediaType) {
        getOrCreateHeaders().withEntry(header(CONTENT_TYPE.toString(), mediaType.toString()));
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one header to match on as a Header object where the header values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param header the Header object which can have a values list of strings or regular expressions
     */
    public HttpRequest replaceHeader(Header header) {
        getOrCreateHeaders().replaceEntry(header);
        this.hashCode = 0;
        return this;
    }

    public List<Header> getHeaderList() {
        if (this.headers != null) {
            return this.headers.getEntries();
        } else {
            return Collections.emptyList();
        }
    }

    public List<String> getHeader(String name) {
        if (this.headers != null) {
            return this.headers.getValues(name);
        } else {
            return Collections.emptyList();
        }
    }

    public String getFirstHeader(String name) {
        if (this.headers != null) {
            return this.headers.getFirstValue(name);
        } else {
            return "";
        }
    }

    /**
     * Returns true if a header with the specified name has been added
     *
     * @param name the header name
     * @return true if a header has been added with that name otherwise false
     */
    public boolean containsHeader(String name) {
        if (this.headers != null) {
            return this.headers.containsEntry(name);
        } else {
            return false;
        }
    }

    /**
     * Returns true if a header with the specified name and value has been added
     *
     * @param name  the header name
     * @param value the header value
     * @return true if a header has been added with that name otherwise false
     */
    public boolean containsHeader(String name, String value) {
        if (this.headers != null) {
            return this.headers.containsEntry(name, value);
        } else {
            return false;
        }
    }

    public HttpRequest removeHeader(String name) {
        if (this.headers != null) {
            headers.remove(name);
            this.hashCode = 0;
        }
        return this;
    }

    public HttpRequest removeHeader(NottableString name) {
        if (this.headers != null) {
            headers.remove(name);
            this.hashCode = 0;
        }
        return this;
    }

    public Cookies getCookies() {
        return this.cookies;
    }

    private Cookies getOrCreateCookies() {
        if (this.cookies == null) {
            this.cookies = new Cookies();
            this.hashCode = 0;
        }
        return this.cookies;
    }

    public HttpRequest withCookies(Cookies cookies) {
        if (cookies == null || cookies.isEmpty()) {
            this.cookies = null;
        } else {
            this.cookies = cookies;
        }
        this.hashCode = 0;
        return this;
    }

    /**
     * The cookies to match on as a list of Cookie objects where the values or keys of each cookie can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param cookies a list of Cookie objects
     */
    public HttpRequest withCookies(List<Cookie> cookies) {
        getOrCreateCookies().withEntries(cookies);
        this.hashCode = 0;
        return this;
    }

    /**
     * The cookies to match on as a varags Cookie objects where the values or keys of each cookie can be either a string or a regex
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param cookies a varargs of Cookie objects
     */
    public HttpRequest withCookies(Cookie... cookies) {
        getOrCreateCookies().withEntries(cookies);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one cookie to match on as a Cookie object where the cookie values list can be a list of strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param cookie a Cookie object
     */
    public HttpRequest withCookie(Cookie cookie) {
        getOrCreateCookies().withEntry(cookie);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one cookie to match on, which the value is plain strings or regular expressions
     * (for more details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param name  the cookies name
     * @param value the cookies value
     */
    public HttpRequest withCookie(String name, String value) {
        getOrCreateCookies().withEntry(name, value);
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one cookie to match on, which the value the values is JSON schema i.e. "{ \"type\": \"string\", \"pattern\": \"^someV[a-z]{4}$\" }"
     * (for more details of the supported JSON schema see https://json-schema.org)
     *
     * @param name  the cookies name
     * @param value the cookies value as JSON schema
     */
    public HttpRequest withSchemaCookie(String name, String value) {
        getOrCreateCookies().withEntry(string(name), schemaString(value));
        this.hashCode = 0;
        return this;
    }

    /**
     * Adds one cookie to match on or to not match on using the NottableString, each NottableString can either be a positive matching value,
     * such as string("match"), or a value to not match on, such as not("do not match"), the string values passed to the NottableString
     * can be a plain string or a regex (for more details of the supported regex syntax see
     * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
     *
     * @param name  the cookies name
     * @param value the cookies value
     */
    public HttpRequest withCookie(NottableString name, NottableString value) {
        getOrCreateCookies().withEntry(name, value);
        this.hashCode = 0;
        return this;
    }

    public List<Cookie> getCookieList() {
        if (this.cookies != null) {
            return this.cookies.getEntries();
        } else {
            return Collections.emptyList();
        }
    }

    public InetSocketAddress socketAddressFromHostHeader() {
        if (socketAddress != null && socketAddress.getHost() != null) {
            boolean isSsl = socketAddress.getScheme() != null && socketAddress.getScheme().equals(SocketAddress.Scheme.HTTPS);
            return new InetSocketAddress(socketAddress.getHost(), socketAddress.getPort() != null ? socketAddress.getPort() : isSsl ? 443 : 80);
        } else if (isNotBlank(getFirstHeader(HOST.toString()))) {
            boolean isSsl = isSecure() != null && isSecure();
            String[] hostHeaderParts = getFirstHeader(HOST.toString()).split(":");
            return new InetSocketAddress(hostHeaderParts[0], hostHeaderParts.length > 1 ? Integer.parseInt(hostHeaderParts[1]) : isSsl ? 443 : 80);
        } else {
            throw new IllegalArgumentException("Host header must be provided to determine remote socket address, the request does not include the \"Host\" header:" + NEW_LINE + this);
        }
    }

    public HttpRequest shallowClone() {
        return not(request(), not)
            .withMethod(method)
            .withPath(path)
            .withPathParameters(pathParameters)
            .withQueryStringParameters(queryStringParameters)
            .withBody(body)
            .withHeaders(headers)
            .withCookies(cookies)
            .withKeepAlive(keepAlive)
            .withSecure(secure)
            .withProtocol(protocol)
            .withStreamId(streamId)
            .withClientCertificateChain(clientCertificateChain)
            .withSocketAddress(socketAddress)
            .withLocalAddress(localAddress)
            .withRemoteAddress(remoteAddress);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public HttpRequest clone() {
        return not(request(), not)
            .withMethod(method)
            .withPath(path)
            .withPathParameters(pathParameters != null ? pathParameters.clone() : null)
            .withQueryStringParameters(queryStringParameters != null ? queryStringParameters.clone() : null)
            .withBody(body)
            .withHeaders(headers != null ? headers.clone() : null)
            .withCookies(cookies != null ? cookies.clone() : null)
            .withKeepAlive(keepAlive)
            .withSecure(secure)
            .withProtocol(protocol)
            .withStreamId(streamId)
            .withClientCertificateChain(clientCertificateChain != null && !clientCertificateChain.isEmpty() ? clientCertificateChain.stream().map(X509Certificate::clone).collect(Collectors.toList()) : null)
            .withSocketAddress(socketAddress)
            .withLocalAddress(localAddress)
            .withRemoteAddress(remoteAddress);
    }

    public HttpRequest update(HttpRequest requestOverride, HttpRequestModifier requestModifier) {
        if (requestOverride != null) {
            if (requestOverride.getMethod() != null && isNotBlank(requestOverride.getMethod().getValue())) {
                withMethod(requestOverride.getMethod());
            }
            if (requestOverride.getPath() != null && isNotBlank(requestOverride.getPath().getValue())) {
                withPath(requestOverride.getPath());
            }
            for (Parameter parameter : requestOverride.getPathParameterList()) {
                getOrCreatePathParameters().replaceEntry(parameter);
            }
            for (Parameter parameter : requestOverride.getQueryStringParameterList()) {
                getOrCreateQueryStringParameters().replaceEntry(parameter);
            }
            if (requestOverride.getBody() != null) {
                withBody(requestOverride.getBody());
            }
            for (Header header : requestOverride.getHeaderList()) {
                getOrCreateHeaders().replaceEntry(header);
            }
            for (Cookie cookie : requestOverride.getCookieList()) {
                withCookie(cookie);
            }
            if (requestOverride.isSecure() != null) {
                withSecure(requestOverride.isSecure());
            }
            if (requestOverride.getProtocol() != null) {
                withProtocol(requestOverride.getProtocol());
            }
            if (requestOverride.getStreamId() != null) {
                withStreamId(requestOverride.getStreamId());
            }
            if (requestOverride.isKeepAlive() != null) {
                withKeepAlive(requestOverride.isKeepAlive());
            }
            if (requestOverride.getSocketAddress() != null) {
                withSocketAddress(requestOverride.getSocketAddress());
            }
            this.hashCode = 0;
        }
        if (requestModifier != null) {
            if (requestModifier.getPath() != null) {
                withPath(requestModifier.getPath().update(getPath()));
            }
            if (requestModifier.getQueryStringParameters() != null) {
                withQueryStringParameters(requestModifier.getQueryStringParameters().update(getQueryStringParameters()));
            }
            if (requestModifier.getHeaders() != null) {
                withHeaders(requestModifier.getHeaders().update(getHeaders()));
            }
            if (requestModifier.getCookies() != null) {
                withCookies(requestModifier.getCookies().update(getCookies()));
            }
        }
        return this;
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
        HttpRequest that = (HttpRequest) o;
        return Objects.equals(method, that.method) &&
            Objects.equals(path, that.path) &&
            Objects.equals(pathParameters, that.pathParameters) &&
            Objects.equals(queryStringParameters, that.queryStringParameters) &&
            Objects.equals(body, that.body) &&
            Objects.equals(headers, that.headers) &&
            Objects.equals(cookies, that.cookies) &&
            Objects.equals(keepAlive, that.keepAlive) &&
            Objects.equals(secure, that.secure) &&
            Objects.equals(protocol, that.protocol) &&
            Objects.equals(streamId, that.streamId) &&
            Objects.equals(clientCertificateChain, that.clientCertificateChain) &&
            Objects.equals(socketAddress, that.socketAddress) &&
            Objects.equals(localAddress, that.localAddress) &&
            Objects.equals(remoteAddress, that.remoteAddress);
    }

    @Override
    public int hashCode() {
        // need to call isSecure because getter can change the hashcode
        isSecure();
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), method, path, pathParameters, queryStringParameters, body, headers, cookies, keepAlive, secure, protocol, streamId, clientCertificateChain, socketAddress, localAddress, remoteAddress);
        }
        return hashCode;
    }
}
