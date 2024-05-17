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

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.model.Header.header;
import static software.xdev.mockserver.model.NottableString.string;
import static software.xdev.mockserver.model.SocketAddress.Scheme.HTTP;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;


@SuppressWarnings({"rawtypes", "UnusedReturnValue"})
public class HttpRequest extends RequestDefinition implements HttpMessage<HttpRequest, Body>
{
	private int hashCode;
	private NottableString method = string("");
	private NottableString path = string("");
	private Parameters pathParameters;
	private Parameters queryStringParameters;
	private Body body;
	private Headers headers;
	private Cookies cookies;
	private Boolean keepAlive;
	private Protocol protocol;
	private Integer streamId;
	private SocketAddress socketAddress;
	private String localAddress;
	private String remoteAddress;
	
	public static HttpRequest request()
	{
		return new HttpRequest();
	}
	
	public static HttpRequest request(final String path)
	{
		return new HttpRequest().withPath(path);
	}
	
	public Boolean isKeepAlive()
	{
		return this.keepAlive;
	}
	
	/**
	 * Match on whether the request was made using an HTTP persistent connection, also called HTTP keep-alive, or HTTP
	 * connection reuse
	 *
	 * @param isKeepAlive true if the request was made with an HTTP persistent connection
	 */
	public HttpRequest withKeepAlive(final Boolean isKeepAlive)
	{
		this.keepAlive = isKeepAlive;
		this.hashCode = 0;
		return this;
	}
	
	public Protocol getProtocol()
	{
		return this.protocol;
	}
	
	/**
	 * Match on whether the request was made over HTTP or HTTP2
	 *
	 * @param protocol used to indicate HTTP or HTTP2
	 */
	public HttpRequest withProtocol(final Protocol protocol)
	{
		this.protocol = protocol;
		this.hashCode = 0;
		return this;
	}
	
	public Integer getStreamId()
	{
		return this.streamId;
	}
	
	/**
	 * HTTP2 stream id request was received on
	 *
	 * @param streamId HTTP2 stream id request was received on
	 */
	public HttpRequest withStreamId(final Integer streamId)
	{
		this.streamId = streamId;
		this.hashCode = 0;
		return this;
	}
	
	public SocketAddress getSocketAddress()
	{
		return this.socketAddress;
	}
	
	/**
	 * Specify remote address if the remote address can't be derived from the host header, if no value is specified the
	 * host header will be used to determine remote address
	 *
	 * @param socketAddress the remote address to send request to
	 */
	public HttpRequest withSocketAddress(final SocketAddress socketAddress)
	{
		this.socketAddress = socketAddress;
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Specify remote address if the remote address can't be derived from the host header, if no value is specified the
	 * host header will be used to determine remote address
	 *
	 * @param host   the remote host or ip to send request to
	 * @param port   the remote port to send request to
	 * @param scheme the scheme to use for remote socket
	 */
	public HttpRequest withSocketAddress(final String host, final Integer port, final SocketAddress.Scheme scheme)
	{
		this.socketAddress = new SocketAddress()
			.withHost(host)
			.withPort(port)
			.withScheme(scheme);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Specify remote address by attempting to derive it from the host header
	 */
	public HttpRequest withSocketAddressFromHostHeader()
	{
		this.withSocketAddress(this.getFirstHeader("host"), null);
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
	public HttpRequest withSocketAddress(final String host, final Integer port)
	{
		if(isNotBlank(host))
		{
			final String[] hostParts = host.split(":");
			if(hostParts.length > 1)
			{
				this.withSocketAddress(hostParts[0], port != null ? port : Integer.parseInt(hostParts[1]), HTTP);
			}
			else
			{
				this.withSocketAddress(host, port != null ? port : 80, HTTP);
			}
		}
		this.hashCode = 0;
		return this;
	}
	
	public HttpRequest withLocalAddress(final String localAddress)
	{
		this.localAddress = localAddress;
		this.hashCode = 0;
		return this;
	}
	
	public String getLocalAddress()
	{
		return this.localAddress;
	}
	
	public HttpRequest withRemoteAddress(final String remoteAddress)
	{
		this.remoteAddress = remoteAddress;
		this.hashCode = 0;
		return this;
	}
	
	public String getRemoteAddress()
	{
		return this.remoteAddress;
	}
	
	/**
	 * The HTTP method to match on such as "GET" or "POST"
	 *
	 * @param method the HTTP method such as "GET" or "POST"
	 */
	public HttpRequest withMethod(final String method)
	{
		return this.withMethod(string(method));
	}
	
	/**
	 * The HTTP method all method except a specific value using the "not" operator, for example this allows operations
	 * such as not("GET")
	 *
	 * @param method the HTTP method to not match on not("GET") or not("POST")
	 */
	public HttpRequest withMethod(final NottableString method)
	{
		this.method = method;
		this.hashCode = 0;
		return this;
	}
	
	public NottableString getMethod()
	{
		return this.method;
	}
	
	public String getMethod(final String defaultValue)
	{
		if(isBlank(this.method.getValue()))
		{
			return defaultValue;
		}
		else
		{
			return this.method.getValue();
		}
	}
	
	/**
	 * The path to match on such as "/some_mocked_path" any servlet context path is ignored for matching and should not
	 * be specified here regex values are also supported such as ".*_path", see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html for full details of the supported regex
	 * syntax
	 *
	 * @param path the path such as "/some_mocked_path" or a regex
	 */
	public HttpRequest withPath(final String path)
	{
		this.withPath(string(path));
		return this;
	}
	
	/**
	 * The path to not match on for example not("/some_mocked_path") with match any path not equal to
	 * "/some_mocked_path", the servlet context path is ignored for matching and should not be specified hereregex
	 * values are also supported such as not(".*_path"), see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html for full details of the supported regex
	 * syntax
	 *
	 * @param path the path to not match on such as not("/some_mocked_path") or not(".*_path")
	 */
	public HttpRequest withPath(final NottableString path)
	{
		this.path = path;
		this.hashCode = 0;
		return this;
	}
	
	public NottableString getPath()
	{
		return this.path;
	}
	
	public boolean matches(final String method)
	{
		return this.method.getValue().equals(method);
	}
	
	public boolean matches(final String method, final String... paths)
	{
		if(!this.matches(method))
		{
			return false;
		}
		return this.matchesPath(paths);
	}
	
	public boolean matchesPath(final String... paths)
	{
		boolean matches = false;
		for(final String path : paths)
		{
			matches = this.path.getValue().equals(path);
			if(matches)
			{
				break;
			}
		}
		return matches;
	}
	
	public Parameters getPathParameters()
	{
		return this.pathParameters;
	}
	
	private Parameters getOrCreatePathParameters()
	{
		if(this.pathParameters == null)
		{
			this.pathParameters = new Parameters();
			this.hashCode = 0;
		}
		return this.pathParameters;
	}
	
	public HttpRequest withPathParameters(final Parameters parameters)
	{
		if(parameters == null || parameters.isEmpty())
		{
			this.pathParameters = null;
		}
		else
		{
			this.pathParameters = parameters;
		}
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The path parameter to match on as a list of Parameter objects where the values or keys of each parameter can be
	 * either a string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param parameters the list of Parameter objects where the values or keys of each parameter can be either a
	 *                      string
	 *                   or a regex
	 */
	public HttpRequest withPathParameters(final List<Parameter> parameters)
	{
		this.getOrCreatePathParameters().withEntries(parameters);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The path parameter to match on as a varags Parameter objects where the values or keys of each parameter can be
	 * either a string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param parameters the varags Parameter objects where the values or keys of each parameter can be either a string
	 *                   or a regex
	 */
	public HttpRequest withPathParameters(final Parameter... parameters)
	{
		this.getOrCreatePathParameters().withEntries(parameters);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The path parameter to match on as a Map&lt;String, List&lt;String&gt;&gt; where the values or keys of each
	 * parameter can be either a string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param parameters the Map&lt;String, List&lt;String&gt;&gt; object where the values or keys of each parameter
	 *                     can
	 *                   be either a string or a regex
	 */
	public HttpRequest withPathParameters(final Map<String, List<String>> parameters)
	{
		this.getOrCreatePathParameters().withEntries(parameters);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one path parameter to match on as a Parameter object where the parameter values list can be a list of
	 * strings or regular expressions (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param parameter the Parameter object which can have a values list of strings or regular expressions
	 */
	public HttpRequest withPathParameter(final Parameter parameter)
	{
		this.getOrCreatePathParameters().withEntry(parameter);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one path parameter to match which can specified using plain strings or regular expressions (for more
	 * details
	 * of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param name   the parameter name
	 * @param values the parameter values which can be a varags of strings or regular expressions
	 */
	public HttpRequest withPathParameter(final String name, String... values)
	{
		if(values.length == 0)
		{
			values = new String[]{".*"};
		}
		this.getOrCreatePathParameters().withEntry(name, values);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one path parameter to match on or to not match on using the NottableString, each NottableString can either
	 * be a positive matching value, such as string("match"), or a value to not match on, such as not("do not match"),
	 * the string values passed to the NottableString can also be a plain string or a regex (for more details of the
	 * supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param name   the parameter name as a NottableString
	 * @param values the parameter values which can be a varags of NottableStrings
	 */
	public HttpRequest withPathParameter(final NottableString name, NottableString... values)
	{
		if(values.length == 0)
		{
			values = new NottableString[]{string(".*")};
		}
		this.getOrCreatePathParameters().withEntry(name, values);
		this.hashCode = 0;
		return this;
	}
	
	public List<Parameter> getPathParameterList()
	{
		if(this.pathParameters != null)
		{
			return this.pathParameters.getEntries();
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	@SuppressWarnings("unused")
	public boolean hasPathParameter(final String name, final String value)
	{
		if(this.pathParameters != null)
		{
			return this.pathParameters.containsEntry(name, value);
		}
		else
		{
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	public boolean hasPathParameter(final NottableString name, final NottableString value)
	{
		if(this.pathParameters != null)
		{
			return this.pathParameters.containsEntry(name, value);
		}
		else
		{
			return false;
		}
	}
	
	public String getFirstPathParameter(final String name)
	{
		if(this.pathParameters != null)
		{
			return this.pathParameters.getFirstValue(name);
		}
		else
		{
			return "";
		}
	}
	
	public Parameters getQueryStringParameters()
	{
		return this.queryStringParameters;
	}
	
	private Parameters getOrCreateQueryStringParameters()
	{
		if(this.queryStringParameters == null)
		{
			this.queryStringParameters = new Parameters();
			this.hashCode = 0;
		}
		return this.queryStringParameters;
	}
	
	public HttpRequest withQueryStringParameters(final Parameters parameters)
	{
		if(parameters == null || parameters.isEmpty())
		{
			this.queryStringParameters = null;
		}
		else
		{
			this.queryStringParameters = parameters;
		}
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The query string parameters to match on as a list of Parameter objects where the values or keys of each
	 * parameter
	 * can be either a string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param parameters the list of Parameter objects where the values or keys of each parameter can be either a
	 *                      string
	 *                   or a regex
	 */
	public HttpRequest withQueryStringParameters(final List<Parameter> parameters)
	{
		this.getOrCreateQueryStringParameters().withEntries(parameters);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The query string parameters to match on as a varags Parameter objects where the values or keys of each parameter
	 * can be either a string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param parameters the varags Parameter objects where the values or keys of each parameter can be either a string
	 *                   or a regex
	 */
	public HttpRequest withQueryStringParameters(final Parameter... parameters)
	{
		this.getOrCreateQueryStringParameters().withEntries(parameters);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The query string parameters to match on as a Map&lt;String, List&lt;String&gt;&gt; where the values or keys of
	 * each parameter can be either a string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param parameters the Map&lt;String, List&lt;String&gt;&gt; object where the values or keys of each parameter
	 *                     can
	 *                   be either a string or a regex
	 */
	public HttpRequest withQueryStringParameters(final Map<String, List<String>> parameters)
	{
		this.getOrCreateQueryStringParameters().withEntries(parameters);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one query string parameter to match on as a Parameter object where the parameter values list can be a list
	 * of strings or regular expressions (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param parameter the Parameter object which can have a values list of strings or regular expressions
	 */
	public HttpRequest withQueryStringParameter(final Parameter parameter)
	{
		this.getOrCreateQueryStringParameters().withEntry(parameter);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one query string parameter to match which the values are plain strings or regular expressions (for more
	 * details of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param name   the parameter name
	 * @param values the parameter values which can be a varags of strings or regular expressions
	 */
	public HttpRequest withQueryStringParameter(final String name, final String... values)
	{
		this.getOrCreateQueryStringParameters().withEntry(name, values);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one query string parameter to match on or to not match on using the NottableString, each NottableString can
	 * either be a positive matching value, such as string("match"), or a value to not match on, such as not("do not
	 * match"), the string values passed to the NottableString can also be a plain string or a regex (for more details
	 * of the supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param name   the parameter name as a NottableString
	 * @param values the parameter values which can be a varags of NottableStrings
	 */
	public HttpRequest withQueryStringParameter(final NottableString name, NottableString... values)
	{
		if(values.length == 0)
		{
			values = new NottableString[]{string(".*")};
		}
		this.getOrCreateQueryStringParameters().withEntry(name, values);
		this.hashCode = 0;
		return this;
	}
	
	public List<Parameter> getQueryStringParameterList()
	{
		if(this.queryStringParameters != null)
		{
			return this.queryStringParameters.getEntries();
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	@SuppressWarnings("unused")
	public boolean hasQueryStringParameter(final String name, final String value)
	{
		if(this.queryStringParameters != null)
		{
			return this.queryStringParameters.containsEntry(name, value);
		}
		else
		{
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	public boolean hasQueryStringParameter(final NottableString name, final NottableString value)
	{
		if(this.queryStringParameters != null)
		{
			return this.queryStringParameters.containsEntry(name, value);
		}
		else
		{
			return false;
		}
	}
	
	public String getFirstQueryStringParameter(final String name)
	{
		if(this.queryStringParameters != null)
		{
			return this.queryStringParameters.getFirstValue(name);
		}
		else
		{
			return "";
		}
	}
	
	/**
	 * The exact string body to match on such as "this is an exact string body"
	 *
	 * @param body the body on such as "this is an exact string body"
	 */
	@Override
	public HttpRequest withBody(final String body)
	{
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
	@Override
	public HttpRequest withBody(final String body, final Charset charset)
	{
		if(body != null)
		{
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
	@Override
	public HttpRequest withBody(final byte[] body)
	{
		this.body = new BinaryBody(body);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The body match rules on such as using one of the Body subclasses as follows:
	 * <p>
	 * exact string match: - exact("this is an exact string body");
	 * <p>
	 * or
	 * <p>
	 * - new StringBody("this is an exact string body")
	 * <p>
	 * regular expression match: - regex("username[a-z]{4}");
	 * <p>
	 * or
	 * <p>
	 * - new RegexBody("username[a-z]{4}");
	 * <p>
	 * json match: - json("{username: 'foo', password: 'bar'}");
	 * <p>
	 * or
	 * <p>
	 * - json("{username: 'foo', password: 'bar'}", MatchType.STRICT);
	 * <p>
	 * or
	 * <p>
	 * - new JsonBody("{username: 'foo', password: 'bar'}");
	 * <p>
	 * json schema match: - jsonSchema("{type: 'object', properties: { 'username': { 'type': 'string' }, 'password': {
	 * 'type': 'string' } }, 'required': ['username', 'password']}");
	 * <p>
	 * or
	 * <p>
	 * - jsonSchemaFromResource("org/mockserver/model/loginSchema.json");
	 * <p>
	 * or
	 * <p>
	 * - new JsonSchemaBody("{type: 'object', properties: { 'username': { 'type': 'string' }, 'password': { 'type':
	 * 'string' } }, 'required': ['username', 'password']}");
	 * <p>
	 * xpath match: - xpath("/element[key = 'some_key' and value = 'some_value']");
	 * <p>
	 * or
	 * <p>
	 * - new XPathBody("/element[key = 'some_key' and value = 'some_value']");
	 * <p>
	 * body parameter match: - params( param("name_one", "value_one_one", "value_one_two") param("name_two",
	 * "value_two") );
	 * <p>
	 * or
	 * <p>
	 * - new ParameterBody( new Parameter("name_one", "value_one_one", "value_one_two") new Parameter("name_two",
	 * "value_two") );
	 * <p>
	 * binary match: - binary(IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("example.pdf"), 1024));
	 * <p>
	 * or
	 * <p>
	 * - new BinaryBody(IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("example.pdf"), 1024));
	 * <p>
	 * for more details of the supported regular expression syntax see <a
	 * href="http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">http://docs.oracle
	 * .com/javase/8/docs/api/java/util/regex/Pattern.html</a>
	 *
	 * @param body an instance of one of the Body subclasses including StringBody, ParameterBody or BinaryBody
	 */
	@Override
	public HttpRequest withBody(final Body body)
	{
		this.body = body;
		this.hashCode = 0;
		return this;
	}
	
	@Override
	public Body getBody()
	{
		return this.body;
	}
	
	@Override
	@JsonIgnore
	public byte[] getBodyAsRawBytes()
	{
		return this.body != null ? this.body.getRawBytes() : new byte[0];
	}
	
	@Override
	@JsonIgnore
	public String getBodyAsString()
	{
		if(this.body != null)
		{
			return this.body.toString();
		}
		else
		{
			return null;
		}
	}
	
	@JsonIgnore
	public String getBodyAsJsonOrXmlString()
	{
		if(this.body != null)
		{
			if(this.body instanceof StringBody)
			{
				// if it should be json (and it has been validated i.e. control plane request)
				// assume the Content-Type header was forgotten so should be parsed as json
				return new String(
					this.body.toString()
						.getBytes(MediaType.parse(this.getFirstHeader(CONTENT_TYPE.toString())).getCharsetOrDefault()),
					StandardCharsets.UTF_8);
			}
			else
			{
				return this.getBodyAsString();
			}
		}
		else
		{
			return null;
		}
	}
	
	@Override
	public Headers getHeaders()
	{
		return this.headers;
	}
	
	private Headers getOrCreateHeaders()
	{
		if(this.headers == null)
		{
			this.headers = new Headers();
			this.hashCode = 0;
		}
		return this.headers;
	}
	
	@Override
	public HttpRequest withHeaders(final Headers headers)
	{
		if(headers == null || headers.isEmpty())
		{
			this.headers = null;
		}
		else
		{
			this.headers = headers;
		}
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The headers to match on as a list of Header objects where the values or keys of each header can be either a
	 * string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param headers the list of Header objects where the values or keys of each header can be either a string or a
	 *                regex
	 */
	@Override
	public HttpRequest withHeaders(final List<Header> headers)
	{
		this.getOrCreateHeaders().withEntries(headers);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The headers to match on as a varags of Header objects where the values or keys of each header can be either a
	 * string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param headers the varags of Header objects where the values or keys of each header can be either a string or a
	 *                regex
	 */
	@Override
	public HttpRequest withHeaders(final Header... headers)
	{
		this.getOrCreateHeaders().withEntries(headers);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one header to match on as a Header object where the header values list can be a list of strings or regular
	 * expressions (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param header the Header object which can have a values list of strings or regular expressions
	 */
	@Override
	public HttpRequest withHeader(final Header header)
	{
		this.getOrCreateHeaders().withEntry(header);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one header to match which can specified using plain strings or regular expressions (for more details of the
	 * supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param name   the header name
	 * @param values the header values which can be a varags of strings or regular expressions
	 */
	@Override
	public HttpRequest withHeader(final String name, String... values)
	{
		if(values.length == 0)
		{
			values = new String[]{".*"};
		}
		this.getOrCreateHeaders().withEntry(header(name, values));
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one header to match on or to not match on using the NottableString, each NottableString can either be a
	 * positive matching value, such as string("match"), or a value to not match on, such as not("do not match"), the
	 * string values passed to the NottableString can also be a plain string or a regex (for more details of the
	 * supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param name   the header name as a NottableString
	 * @param values the header values which can be a varags of NottableStrings
	 */
	@Override
	public HttpRequest withHeader(final NottableString name, NottableString... values)
	{
		if(values.length == 0)
		{
			values = new NottableString[]{string(".*")};
		}
		this.getOrCreateHeaders().withEntry(header(name, values));
		this.hashCode = 0;
		return this;
	}
	
	@Override
	public HttpRequest withContentType(final MediaType mediaType)
	{
		this.getOrCreateHeaders().withEntry(header(CONTENT_TYPE.toString(), mediaType.toString()));
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one header to match on as a Header object where the header values list can be a list of strings or regular
	 * expressions (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param header the Header object which can have a values list of strings or regular expressions
	 */
	@Override
	public HttpRequest replaceHeader(final Header header)
	{
		this.getOrCreateHeaders().replaceEntry(header);
		this.hashCode = 0;
		return this;
	}
	
	@Override
	public List<Header> getHeaderList()
	{
		if(this.headers != null)
		{
			return this.headers.getEntries();
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	@Override
	public List<String> getHeader(final String name)
	{
		if(this.headers != null)
		{
			return this.headers.getValues(name);
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	@Override
	public String getFirstHeader(final String name)
	{
		if(this.headers != null)
		{
			return this.headers.getFirstValue(name);
		}
		else
		{
			return "";
		}
	}
	
	/**
	 * Returns true if a header with the specified name has been added
	 *
	 * @param name the header name
	 * @return true if a header has been added with that name otherwise false
	 */
	@Override
	public boolean containsHeader(final String name)
	{
		if(this.headers != null)
		{
			return this.headers.containsEntry(name);
		}
		else
		{
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
	public boolean containsHeader(final String name, final String value)
	{
		if(this.headers != null)
		{
			return this.headers.containsEntry(name, value);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public HttpRequest removeHeader(final String name)
	{
		if(this.headers != null)
		{
			this.headers.remove(name);
			this.hashCode = 0;
		}
		return this;
	}
	
	@Override
	public HttpRequest removeHeader(final NottableString name)
	{
		if(this.headers != null)
		{
			this.headers.remove(name);
			this.hashCode = 0;
		}
		return this;
	}
	
	@Override
	public Cookies getCookies()
	{
		return this.cookies;
	}
	
	private Cookies getOrCreateCookies()
	{
		if(this.cookies == null)
		{
			this.cookies = new Cookies();
			this.hashCode = 0;
		}
		return this.cookies;
	}
	
	@Override
	public HttpRequest withCookies(final Cookies cookies)
	{
		if(cookies == null || cookies.isEmpty())
		{
			this.cookies = null;
		}
		else
		{
			this.cookies = cookies;
		}
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The cookies to match on as a list of Cookie objects where the values or keys of each cookie can be either a
	 * string or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param cookies a list of Cookie objects
	 */
	@Override
	public HttpRequest withCookies(final List<Cookie> cookies)
	{
		this.getOrCreateCookies().withEntries(cookies);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The cookies to match on as a varags Cookie objects where the values or keys of each cookie can be either a
	 * string
	 * or a regex (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param cookies a varargs of Cookie objects
	 */
	@Override
	public HttpRequest withCookies(final Cookie... cookies)
	{
		this.getOrCreateCookies().withEntries(cookies);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one cookie to match on as a Cookie object where the cookie values list can be a list of strings or regular
	 * expressions (for more details of the supported regex syntax see
	 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param cookie a Cookie object
	 */
	@Override
	public HttpRequest withCookie(final Cookie cookie)
	{
		this.getOrCreateCookies().withEntry(cookie);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one cookie to match on, which the value is plain strings or regular expressions (for more details of the
	 * supported regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param name  the cookies name
	 * @param value the cookies value
	 */
	@Override
	public HttpRequest withCookie(final String name, final String value)
	{
		this.getOrCreateCookies().withEntry(name, value);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Adds one cookie to match on or to not match on using the NottableString, each NottableString can either be a
	 * positive matching value, such as string("match"), or a value to not match on, such as not("do not match"), the
	 * string values passed to the NottableString can be a plain string or a regex (for more details of the supported
	 * regex syntax see http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
	 *
	 * @param name  the cookies name
	 * @param value the cookies value
	 */
	@Override
	public HttpRequest withCookie(final NottableString name, final NottableString value)
	{
		this.getOrCreateCookies().withEntry(name, value);
		this.hashCode = 0;
		return this;
	}
	
	@Override
	public List<Cookie> getCookieList()
	{
		if(this.cookies != null)
		{
			return this.cookies.getEntries();
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	public InetSocketAddress socketAddressFromHostHeader()
	{
		if(this.socketAddress != null && this.socketAddress.getHost() != null)
		{
			final boolean isSsl =
				this.socketAddress.getScheme() != null && this.socketAddress.getScheme()
					.equals(SocketAddress.Scheme.HTTPS);
			return new InetSocketAddress(
				this.socketAddress.getHost(),
				this.socketAddress.getPort() != null ? this.socketAddress.getPort() : isSsl ? 443 : 80);
		}
		else if(isNotBlank(this.getFirstHeader(HOST.toString())))
		{
			final String[] hostHeaderParts = this.getFirstHeader(HOST.toString()).split(":");
			return new InetSocketAddress(
				hostHeaderParts[0],
				hostHeaderParts.length > 1 ? Integer.parseInt(hostHeaderParts[1]) : 80);
		}
		else
		{
			throw new IllegalArgumentException(
				"Host header must be provided to determine remote socket address, the request does not include the "
					+ "\"Host\" header:"
					+ NEW_LINE + this);
		}
	}
	
	@Override
	public HttpRequest shallowClone()
	{
		return not(request(), this.not)
			.withMethod(this.method)
			.withPath(this.path)
			.withPathParameters(this.pathParameters)
			.withQueryStringParameters(this.queryStringParameters)
			.withBody(this.body)
			.withHeaders(this.headers)
			.withCookies(this.cookies)
			.withKeepAlive(this.keepAlive)
			.withProtocol(this.protocol)
			.withStreamId(this.streamId)
			.withSocketAddress(this.socketAddress)
			.withLocalAddress(this.localAddress)
			.withRemoteAddress(this.remoteAddress);
	}
	
	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public HttpRequest clone()
	{
		return not(request(), this.not)
			.withMethod(this.method)
			.withPath(this.path)
			.withPathParameters(this.pathParameters != null ? this.pathParameters.clone() : null)
			.withQueryStringParameters(this.queryStringParameters != null ? this.queryStringParameters.clone() : null)
			.withBody(this.body)
			.withHeaders(this.headers != null ? this.headers.clone() : null)
			.withCookies(this.cookies != null ? this.cookies.clone() : null)
			.withKeepAlive(this.keepAlive)
			.withProtocol(this.protocol)
			.withStreamId(this.streamId)
			.withSocketAddress(this.socketAddress)
			.withLocalAddress(this.localAddress)
			.withRemoteAddress(this.remoteAddress);
	}
	
	public HttpRequest update(final HttpRequest requestOverride, final HttpRequestModifier requestModifier)
	{
		if(requestOverride != null)
		{
			if(requestOverride.getMethod() != null && isNotBlank(requestOverride.getMethod().getValue()))
			{
				this.withMethod(requestOverride.getMethod());
			}
			if(requestOverride.getPath() != null && isNotBlank(requestOverride.getPath().getValue()))
			{
				this.withPath(requestOverride.getPath());
			}
			for(final Parameter parameter : requestOverride.getPathParameterList())
			{
				this.getOrCreatePathParameters().replaceEntry(parameter);
			}
			for(final Parameter parameter : requestOverride.getQueryStringParameterList())
			{
				this.getOrCreateQueryStringParameters().replaceEntry(parameter);
			}
			if(requestOverride.getBody() != null)
			{
				this.withBody(requestOverride.getBody());
			}
			for(final Header header : requestOverride.getHeaderList())
			{
				this.getOrCreateHeaders().replaceEntry(header);
			}
			for(final Cookie cookie : requestOverride.getCookieList())
			{
				this.withCookie(cookie);
			}
			if(requestOverride.getProtocol() != null)
			{
				this.withProtocol(requestOverride.getProtocol());
			}
			if(requestOverride.getStreamId() != null)
			{
				this.withStreamId(requestOverride.getStreamId());
			}
			if(requestOverride.isKeepAlive() != null)
			{
				this.withKeepAlive(requestOverride.isKeepAlive());
			}
			if(requestOverride.getSocketAddress() != null)
			{
				this.withSocketAddress(requestOverride.getSocketAddress());
			}
			this.hashCode = 0;
		}
		if(requestModifier != null)
		{
			if(requestModifier.getPath() != null)
			{
				this.withPath(requestModifier.getPath().update(this.getPath()));
			}
			if(requestModifier.getQueryStringParameters() != null)
			{
				this.withQueryStringParameters(requestModifier.getQueryStringParameters()
					.update(this.getQueryStringParameters()));
			}
			if(requestModifier.getHeaders() != null)
			{
				this.withHeaders(requestModifier.getHeaders().update(this.getHeaders()));
			}
			if(requestModifier.getCookies() != null)
			{
				this.withCookies(requestModifier.getCookies().update(this.getCookies()));
			}
		}
		return this;
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
		final HttpRequest that = (HttpRequest)o;
		return Objects.equals(this.method, that.method)
			&& Objects.equals(this.path, that.path)
			&& Objects.equals(this.pathParameters, that.pathParameters)
			&& Objects.equals(this.queryStringParameters, that.queryStringParameters)
			&& Objects.equals(this.body, that.body)
			&& Objects.equals(this.headers, that.headers)
			&& Objects.equals(this.cookies, that.cookies)
			&& Objects.equals(this.keepAlive, that.keepAlive)
			&& Objects.equals(this.protocol, that.protocol)
			&& Objects.equals(this.streamId, that.streamId)
			&& Objects.equals(this.socketAddress, that.socketAddress)
			&& Objects.equals(this.localAddress, that.localAddress)
			&& Objects.equals(this.remoteAddress, that.remoteAddress);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(
				super.hashCode(),
				this.method,
				this.path,
				this.pathParameters,
				this.queryStringParameters,
				this.body,
				this.headers,
				this.cookies,
				this.keepAlive,
				this.protocol,
				this.streamId,
				this.socketAddress,
				this.localAddress,
				this.remoteAddress);
		}
		return this.hashCode;
	}
}
