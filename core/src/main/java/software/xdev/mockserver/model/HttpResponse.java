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
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;
import static software.xdev.mockserver.model.Header.header;
import static software.xdev.mockserver.model.HttpStatusCode.NOT_FOUND_404;
import static software.xdev.mockserver.model.HttpStatusCode.OK_200;
import static software.xdev.mockserver.model.NottableString.string;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;


@SuppressWarnings("rawtypes")
public class HttpResponse extends Action<HttpResponse> implements HttpMessage<HttpResponse, BodyWithContentType>
{
	private int hashCode;
	private Integer statusCode;
	private String reasonPhrase;
	private BodyWithContentType body;
	private Headers headers;
	private Cookies cookies;
	private ConnectionOptions connectionOptions;
	private Integer streamId;
	
	/**
	 * Static builder to create a response.
	 */
	public static HttpResponse response()
	{
		return new HttpResponse();
	}
	
	/**
	 * Static builder to create a response with a 200 status code and the string response body.
	 *
	 * @param body a string
	 */
	public static HttpResponse response(final String body)
	{
		return new HttpResponse().withStatusCode(OK_200.code()).withReasonPhrase(OK_200.reasonPhrase()).withBody(body);
	}
	
	/**
	 * Static builder to create a not found response.
	 */
	public static HttpResponse notFoundResponse()
	{
		return new HttpResponse().withStatusCode(NOT_FOUND_404.code()).withReasonPhrase(NOT_FOUND_404.reasonPhrase());
	}
	
	/**
	 * The status code to return, such as 200, 404, the status code specified here will result in the default status
	 * message for this status code for example for 200 the status message "OK" is used
	 *
	 * @param statusCode an integer such as 200 or 404
	 */
	public HttpResponse withStatusCode(final Integer statusCode)
	{
		this.statusCode = statusCode;
		this.hashCode = 0;
		return this;
	}
	
	public Integer getStatusCode()
	{
		return this.statusCode;
	}
	
	/**
	 * The reason phrase to return, if no reason code is returned this will be defaulted to the standard reason phrase
	 * for the statusCode, i.e. for a statusCode of 200 the standard reason phrase is "OK"
	 *
	 * @param reasonPhrase an string such as "Not Found" or "OK"
	 */
	public HttpResponse withReasonPhrase(final String reasonPhrase)
	{
		this.reasonPhrase = reasonPhrase;
		this.hashCode = 0;
		return this;
	}
	
	public String getReasonPhrase()
	{
		return this.reasonPhrase;
	}
	
	/**
	 * Set response body to return as a string response body. The character set will be determined by the Content-Type
	 * header on the response. To force the character set, use {@link #withBody(String, Charset)}.
	 *
	 * @param body a string
	 */
	@Override
	public HttpResponse withBody(final String body)
	{
		if(body != null)
		{
			this.body = new StringBody(body);
			this.hashCode = 0;
		}
		return this;
	}
	
	/**
	 * Set response body to return a string response body with the specified encoding. <b>Note:</b> The character
	 * set of
	 * the response will be forced to the specified charset, even if the Content-Type header specifies otherwise.
	 *
	 * @param body    a string
	 * @param charset character set the string will be encoded in
	 */
	@Override
	public HttpResponse withBody(final String body, final Charset charset)
	{
		if(body != null)
		{
			this.body = new StringBody(body, charset);
			this.hashCode = 0;
		}
		return this;
	}
	
	/**
	 * Set response body to return a string response body with the specified encoding. <b>Note:</b> The character
	 * set of
	 * the response will be forced to the specified charset, even if the Content-Type header specifies otherwise.
	 *
	 * @param body        a string
	 * @param contentType media type, if charset is included this will be used for encoding string
	 */
	public HttpResponse withBody(final String body, final MediaType contentType)
	{
		if(body != null)
		{
			this.body = new StringBody(body, contentType);
			this.hashCode = 0;
		}
		return this;
	}
	
	/**
	 * Set response body to return as binary such as a pdf or image
	 *
	 * @param body a byte array
	 */
	@Override
	public HttpResponse withBody(final byte[] body)
	{
		this.body = new BinaryBody(body);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Set the body to return for example:
	 * <p/>
	 * string body: - exact("<html><head/><body><div>a simple string body</div></body></html>");
	 * <p/>
	 * or
	 * <p/>
	 * - new StringBody("<html><head/><body><div>a simple string body</div></body></html>")
	 * <p/>
	 * binary body: - binary(IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("example.pdf"), 1024));
	 * <p/>
	 * or
	 * <p/>
	 * - new BinaryBody(IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("example.pdf"), 1024));
	 *
	 * @param body an instance of one of the Body subclasses including StringBody or BinaryBody
	 */
	@Override
	public HttpResponse withBody(final BodyWithContentType body)
	{
		this.body = body;
		this.hashCode = 0;
		return this;
	}
	
	@Override
	public BodyWithContentType getBody()
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
	public HttpResponse withHeaders(final Headers headers)
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
	 * The headers to return as a list of Header objects
	 *
	 * @param headers a list of Header objects
	 */
	@Override
	public HttpResponse withHeaders(final List<Header> headers)
	{
		this.getOrCreateHeaders().withEntries(headers);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The headers to return as a varargs of Header objects
	 *
	 * @param headers varargs of Header objects
	 */
	@Override
	public HttpResponse withHeaders(final Header... headers)
	{
		this.getOrCreateHeaders().withEntries(headers);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Add a header to return as a Header object, if a header with the same name already exists this will NOT be
	 * modified but two headers will exist
	 *
	 * @param header a Header object
	 */
	@Override
	public HttpResponse withHeader(final Header header)
	{
		this.getOrCreateHeaders().withEntry(header);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Add a header to return as a Header object, if a header with the same name already exists this will NOT be
	 * modified but two headers will exist
	 *
	 * @param name   the header name
	 * @param values the header values
	 */
	@Override
	public HttpResponse withHeader(final String name, String... values)
	{
		if(values.length == 0)
		{
			values = new String[]{".*"};
		}
		this.getOrCreateHeaders().withEntry(name, values);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Add a header to return as a Header object, if a header with the same name already exists this will NOT be
	 * modified but two headers will exist
	 *
	 * @param name   the header name as a NottableString
	 * @param values the header values which can be a varags of NottableStrings
	 */
	@Override
	public HttpResponse withHeader(final NottableString name, NottableString... values)
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
	public HttpResponse withContentType(final MediaType mediaType)
	{
		this.getOrCreateHeaders().withEntry(header(CONTENT_TYPE.toString(), mediaType.toString()));
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Update header to return as a Header object, if a header with the same name already exists it will be modified
	 *
	 * @param header a Header object
	 */
	@Override
	public HttpResponse replaceHeader(final Header header)
	{
		this.getOrCreateHeaders().replaceEntry(header);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Update header to return as a Header object, if a header with the same name already exists it will be modified
	 *
	 * @param name   the header name
	 * @param values the header values
	 */
	public HttpResponse replaceHeader(final String name, String... values)
	{
		if(values.length == 0)
		{
			values = new String[]{".*"};
		}
		this.getOrCreateHeaders().replaceEntry(name, values);
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
	
	public Map<NottableString, List<NottableString>> getHeaderMultimap()
	{
		if(this.headers != null)
		{
			return this.headers.getMultimap();
		}
		else
		{
			return null;
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
	
	@Override
	public HttpResponse removeHeader(final String name)
	{
		if(this.headers != null)
		{
			this.headers.remove(name);
			this.hashCode = 0;
		}
		return this;
	}
	
	@Override
	public HttpResponse removeHeader(final NottableString name)
	{
		if(this.headers != null)
		{
			this.headers.remove(name);
			this.hashCode = 0;
		}
		return this;
	}
	
	/**
	 * Returns true if a header with the specified name has been added
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
	public HttpResponse withCookies(final Cookies cookies)
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
	 * The cookies to return as Set-Cookie headers as a list of Cookie objects
	 *
	 * @param cookies a list of Cookie objects
	 */
	@Override
	public HttpResponse withCookies(final List<Cookie> cookies)
	{
		this.getOrCreateCookies().withEntries(cookies);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The cookies to return as Set-Cookie headers as a varargs of Cookie objects
	 *
	 * @param cookies a varargs of Cookie objects
	 */
	@Override
	public HttpResponse withCookies(final Cookie... cookies)
	{
		this.getOrCreateCookies().withEntries(cookies);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Add cookie to return as Set-Cookie header
	 *
	 * @param cookie a Cookie object
	 */
	@Override
	public HttpResponse withCookie(final Cookie cookie)
	{
		this.getOrCreateCookies().withEntry(cookie);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * Add cookie to return as Set-Cookie header
	 *
	 * @param name  the cookies name
	 * @param value the cookies value
	 */
	@Override
	public HttpResponse withCookie(final String name, final String value)
	{
		this.getOrCreateCookies().withEntry(name, value);
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * <p>
	 * Adds one cookie to match on or to not match on using the NottableString, each NottableString can either be a
	 * positive matching value, such as string("match"), or a value to not match on, such as not("do not match"), the
	 * string values passed to the NottableString can be a plain string or a regex (for more details of the supported
	 * regex syntax see <a href="http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">...</a>)
	 * </p>
	 *
	 * @param name  the cookies name
	 * @param value the cookies value
	 */
	@Override
	public HttpResponse withCookie(final NottableString name, final NottableString value)
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
	
	public Map<NottableString, NottableString> getCookieMap()
	{
		if(this.cookies != null)
		{
			return this.cookies.getMap();
		}
		else
		{
			return null;
		}
	}
	
	public boolean cookieHeaderDoesNotAlreadyExists(final Cookie cookieValue)
	{
		final List<String> setCookieHeaders = this.getHeader(SET_COOKIE.toString());
		for(final String setCookieHeader : setCookieHeaders)
		{
			final String existingCookieName = ClientCookieDecoder.LAX.decode(setCookieHeader).name();
			final String existingCookieValue = ClientCookieDecoder.LAX.decode(setCookieHeader).value();
			if(existingCookieName.equalsIgnoreCase(cookieValue.getName().getValue())
				&& existingCookieValue.equalsIgnoreCase(cookieValue.getValue().getValue()))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean cookieHeaderDoesNotAlreadyExists(final String name, final String value)
	{
		final List<String> setCookieHeaders = this.getHeader(SET_COOKIE.toString());
		for(final String setCookieHeader : setCookieHeaders)
		{
			final String existingCookieName = ClientCookieDecoder.LAX.decode(setCookieHeader).name();
			final String existingCookieValue = ClientCookieDecoder.LAX.decode(setCookieHeader).value();
			if(existingCookieName.equalsIgnoreCase(name) && existingCookieValue.equalsIgnoreCase(value))
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * The connection options for override the default connection behaviour, this allows full control of headers
	 * such as
	 * "Connection" or "Content-Length" or controlling whether the socket is closed after the response has been sent
	 *
	 * @param connectionOptions the connection options for override the default connection behaviour
	 */
	public HttpResponse withConnectionOptions(final ConnectionOptions connectionOptions)
	{
		this.connectionOptions = connectionOptions;
		this.hashCode = 0;
		return this;
	}
	
	public ConnectionOptions getConnectionOptions()
	{
		return this.connectionOptions;
	}
	
	public HttpResponse withStreamId(final Integer streamId)
	{
		this.streamId = streamId;
		this.hashCode = 0;
		return this;
	}
	
	public Integer getStreamId()
	{
		return this.streamId;
	}
	
	@Override
	@JsonIgnore
	public Type getType()
	{
		return Type.RESPONSE;
	}
	
	public HttpResponse shallowClone()
	{
		return response()
			.withStatusCode(this.statusCode)
			.withReasonPhrase(this.reasonPhrase)
			.withBody(this.body)
			.withHeaders(this.headers)
			.withCookies(this.cookies)
			.withDelay(this.getDelay())
			.withConnectionOptions(this.connectionOptions)
			.withStreamId(this.streamId);
	}
	
	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public HttpResponse clone()
	{
		return response()
			.withStatusCode(this.statusCode)
			.withReasonPhrase(this.reasonPhrase)
			.withBody(this.body)
			.withHeaders(this.headers != null ? this.headers.clone() : null)
			.withCookies(this.cookies != null ? this.cookies.clone() : null)
			.withDelay(this.getDelay())
			.withConnectionOptions(this.connectionOptions)
			.withStreamId(this.streamId);
	}
	
	public HttpResponse update(final HttpResponse responseOverride, final HttpResponseModifier responseModifier)
	{
		if(responseOverride != null)
		{
			if(responseOverride.getStatusCode() != null)
			{
				this.withStatusCode(responseOverride.getStatusCode());
			}
			if(responseOverride.getReasonPhrase() != null)
			{
				this.withReasonPhrase(responseOverride.getReasonPhrase());
			}
			for(final Header header : responseOverride.getHeaderList())
			{
				this.getOrCreateHeaders().replaceEntry(header);
			}
			for(final Cookie cookie : responseOverride.getCookieList())
			{
				this.withCookie(cookie);
			}
			if(responseOverride.getBody() != null)
			{
				this.withBody(responseOverride.getBody());
			}
			if(responseOverride.getConnectionOptions() != null)
			{
				this.withConnectionOptions(responseOverride.getConnectionOptions());
			}
			if(responseOverride.getStreamId() != null)
			{
				this.withStreamId(responseOverride.getStreamId());
			}
			this.hashCode = 0;
		}
		if(responseModifier != null)
		{
			if(responseModifier.getHeaders() != null)
			{
				this.withHeaders(responseModifier.getHeaders().update(this.getHeaders()));
			}
			if(responseModifier.getCookies() != null)
			{
				this.withCookies(responseModifier.getCookies().update(this.getCookies()));
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
		final HttpResponse that = (HttpResponse)o;
		return Objects.equals(this.statusCode, that.statusCode) &&
			Objects.equals(this.reasonPhrase, that.reasonPhrase) &&
			Objects.equals(this.body, that.body) &&
			Objects.equals(this.headers, that.headers) &&
			Objects.equals(this.cookies, that.cookies) &&
			Objects.equals(this.connectionOptions, that.connectionOptions) &&
			Objects.equals(this.streamId, that.streamId);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(
				super.hashCode(),
				this.statusCode,
				this.reasonPhrase,
				this.body,
				this.headers,
				this.cookies,
				this.connectionOptions,
				this.streamId);
		}
		return this.hashCode;
	}
}
