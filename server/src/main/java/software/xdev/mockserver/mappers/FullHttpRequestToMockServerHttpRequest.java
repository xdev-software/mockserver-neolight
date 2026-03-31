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
package software.xdev.mockserver.mappers;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http2.HttpConversionUtil;
import software.xdev.mockserver.codec.BodyDecoderEncoder;
import software.xdev.mockserver.codec.ExpandedParameterDecoder;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.Protocol;
import software.xdev.mockserver.url.URLParser;
import software.xdev.mockserver.util.StringUtils;


public class FullHttpRequestToMockServerHttpRequest
{
	private static final Logger LOG = LoggerFactory.getLogger(FullHttpRequestToMockServerHttpRequest.class);
	private final BodyDecoderEncoder bodyDecoderEncoder;
	private final ExpandedParameterDecoder formParameterParser;
	private final Integer port;
	
	public FullHttpRequestToMockServerHttpRequest(final ServerConfiguration configuration, final Integer port)
	{
		this.bodyDecoderEncoder = new BodyDecoderEncoder();
		this.formParameterParser = new ExpandedParameterDecoder(configuration);
		this.port = port;
	}
	
	public HttpRequest mapFullHttpRequestToMockServerRequest(
		final FullHttpRequest fullHttpRequest,
		final List<Header> preservedHeaders,
		final SocketAddress localAddress,
		final SocketAddress remoteAddress)
	{
		final HttpRequest httpRequest = new HttpRequest();
		try
		{
			if(fullHttpRequest != null)
			{
				if(fullHttpRequest.decoderResult().isFailure())
				{
					LOG.error("Exception decoding request", fullHttpRequest.decoderResult().cause());
				}
				this.setMethod(httpRequest, fullHttpRequest);
				httpRequest.withKeepAlive(isKeepAlive(fullHttpRequest));
				httpRequest.withProtocol(Protocol.HTTP_1_1);
				
				this.setPath(httpRequest, fullHttpRequest);
				this.setQueryString(httpRequest, fullHttpRequest);
				this.setHeaders(httpRequest, fullHttpRequest, preservedHeaders);
				this.setCookies(httpRequest, fullHttpRequest);
				this.setBody(httpRequest, fullHttpRequest);
				this.setSocketAddress(httpRequest, fullHttpRequest, this.port, localAddress, remoteAddress);
			}
		}
		catch(final Exception ex)
		{
			LOG.error("Exception decoding request {}", fullHttpRequest, ex);
		}
		return httpRequest;
	}
	
	private void setSocketAddress(
		final HttpRequest httpRequest,
		final FullHttpRequest fullHttpRequest,
		final Integer port,
		final SocketAddress localAddress,
		final SocketAddress remoteAddress)
	{
		httpRequest.withSocketAddress(fullHttpRequest.headers().get("host"), port);
		if(remoteAddress instanceof InetSocketAddress)
		{
			httpRequest.withRemoteAddress(StringUtils.removeStart(remoteAddress.toString(), "/"));
		}
		if(localAddress instanceof InetSocketAddress)
		{
			httpRequest.withLocalAddress(StringUtils.removeStart(localAddress.toString(), "/"));
		}
	}
	
	private void setMethod(final HttpRequest httpRequest, final FullHttpRequest fullHttpResponse)
	{
		httpRequest.withMethod(fullHttpResponse.method().name());
	}
	
	private void setPath(final HttpRequest httpRequest, final FullHttpRequest fullHttpRequest)
	{
		httpRequest.withPath(URLParser.returnPath(fullHttpRequest.uri()));
	}
	
	private void setQueryString(final HttpRequest httpRequest, final FullHttpRequest fullHttpRequest)
	{
		if(fullHttpRequest.uri().contains("?"))
		{
			httpRequest.withQueryStringParameters(this.formParameterParser.retrieveQueryParameters(
				fullHttpRequest.uri(),
				true));
		}
	}
	
	private void setHeaders(
		final HttpRequest httpRequest,
		final FullHttpRequest fullHttpResponse,
		final List<Header> preservedHeaders)
	{
		final HttpHeaders httpHeaders = fullHttpResponse.headers();
		if(!httpHeaders.isEmpty())
		{
			final Headers headers = new Headers();
			for(final String headerName : httpHeaders.names())
			{
				headers.withEntry(headerName, httpHeaders.getAll(headerName));
			}
			httpRequest.withHeaders(headers);
		}
		if(preservedHeaders != null && !preservedHeaders.isEmpty())
		{
			for(final Header preservedHeader : preservedHeaders)
			{
				httpRequest.withHeader(preservedHeader);
			}
		}
		if(Protocol.HTTP_2.equals(httpRequest.getProtocol()))
		{
			final Integer streamId =
				fullHttpResponse.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
			httpRequest.withStreamId(streamId);
		}
	}
	
	private void setCookies(final HttpRequest httpRequest, final FullHttpRequest fullHttpResponse)
	{
		final List<String> cookieHeaders = fullHttpResponse.headers().getAll(COOKIE);
		if(!cookieHeaders.isEmpty())
		{
			final Cookies cookies = new Cookies();
			for(final String cookieHeader : cookieHeaders)
			{
				final Set<Cookie> decodedCookies = ServerCookieDecoder.LAX.decode(cookieHeader);
				for(final io.netty.handler.codec.http.cookie.Cookie decodedCookie : decodedCookies)
				{
					cookies.withEntry(
						decodedCookie.name(),
						decodedCookie.value()
					);
				}
			}
			httpRequest.withCookies(cookies);
		}
	}
	
	private void setBody(final HttpRequest httpRequest, final FullHttpRequest fullHttpRequest)
	{
		httpRequest.withBody(this.bodyDecoderEncoder.byteBufToBody(
			fullHttpRequest.content(),
			fullHttpRequest.headers().get(CONTENT_TYPE)));
	}
}
