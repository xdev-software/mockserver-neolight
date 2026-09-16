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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import software.xdev.mockserver.codec.BodyDecoderEncoder;
import software.xdev.mockserver.model.Cookie;
import software.xdev.mockserver.model.Cookies;
import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.NottableString;


public class FullHttpResponseToMockServerHttpResponse
{
	private static final Logger LOG = LoggerFactory.getLogger(FullHttpResponseToMockServerHttpResponse.class);
	private final BodyDecoderEncoder bodyDecoderEncoder;
	
	public FullHttpResponseToMockServerHttpResponse()
	{
		this.bodyDecoderEncoder = new BodyDecoderEncoder();
	}
	
	public HttpResponse mapFullHttpResponseToMockServerResponse(final FullHttpResponse fullHttpResponse)
	{
		final HttpResponse httpResponse = new HttpResponse();
		try
		{
			if(fullHttpResponse != null)
			{
				if(fullHttpResponse.decoderResult().isFailure())
				{
					LOG.error("Exception decoding response {}", fullHttpResponse.decoderResult().cause().getMessage());
				}
				this.setStatusCode(httpResponse, fullHttpResponse);
				this.setHeaders(httpResponse, fullHttpResponse);
				this.setCookies(httpResponse);
				this.setBody(httpResponse, fullHttpResponse);
			}
		}
		catch(final Exception ex)
		{
			LOG.error("Exception decoding response", ex);
		}
		return httpResponse;
	}
	
	private void setStatusCode(final HttpResponse httpResponse, final FullHttpResponse fullHttpResponse)
	{
		final HttpResponseStatus status = fullHttpResponse.status();
		httpResponse.withStatusCode(status.code());
		httpResponse.withReasonPhrase(status.reasonPhrase());
	}
	
	private void setHeaders(final HttpResponse httpResponse, final FullHttpResponse fullHttpResponse)
	{
		final Set<String> headerNames = fullHttpResponse.headers().names();
		if(!headerNames.isEmpty())
		{
			final Headers headers = new Headers();
			for(final String headerName : headerNames)
			{
				headers.withEntry(headerName, fullHttpResponse.headers().getAll(headerName));
			}
			httpResponse.withHeaders(headers);
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	private void setCookies(final HttpResponse httpResponse)
	{
		final Cookies cookies = new Cookies();
		for(final Header header : httpResponse.getHeaderList())
		{
			if("Set-Cookie".equalsIgnoreCase(header.getName().getValue()))
			{
				for(final NottableString cookieHeader : header.getValues())
				{
					final io.netty.handler.codec.http.cookie.Cookie httpCookie =
						ClientCookieDecoder.LAX.decode(cookieHeader.getValue());
					final String name = httpCookie.name().trim();
					final String value = httpCookie.value() != null ? httpCookie.value().trim() : "";
					cookies.withEntry(new Cookie(name, value));
				}
			}
			if("Cookie".equalsIgnoreCase(header.getName().getValue()))
			{
				for(final NottableString cookieHeader : header.getValues())
				{
					for(final io.netty.handler.codec.http.cookie.Cookie httpCookie : ServerCookieDecoder.LAX.decode(
						cookieHeader.getValue()))
					{
						final String name = httpCookie.name().trim();
						final String value = httpCookie.value() != null ? httpCookie.value().trim() : "";
						cookies.withEntry(new Cookie(name, value));
					}
				}
			}
		}
		if(!cookies.isEmpty())
		{
			httpResponse.withCookies(cookies);
		}
	}
	
	private void setBody(final HttpResponse httpResponse, final FullHttpResponse fullHttpResponse)
	{
		httpResponse.withBody(this.bodyDecoderEncoder.byteBufToBody(
			fullHttpResponse.content(),
			fullHttpResponse.headers().get(CONTENT_TYPE)));
	}
}
