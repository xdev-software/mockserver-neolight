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

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;
import static software.xdev.mockserver.util.StringUtils.isBlank;
import static software.xdev.mockserver.util.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpObject;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http2.HttpConversionUtil;
import software.xdev.mockserver.codec.BodyDecoderEncoder;
import software.xdev.mockserver.model.ConnectionOptions;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.NottableString;


public class MockServerHttpResponseToFullHttpResponse
{
	private static final Logger LOG = LoggerFactory.getLogger(MockServerHttpResponseToFullHttpResponse.class);
	private final BodyDecoderEncoder bodyDecoderEncoder;
	
	public MockServerHttpResponseToFullHttpResponse()
	{
		this.bodyDecoderEncoder = new BodyDecoderEncoder();
	}
	
	public List<DefaultHttpObject> mapMockServerResponseToNettyResponse(final HttpResponse httpResponse)
	{
		try
		{
			final ConnectionOptions connectionOptions = httpResponse.getConnectionOptions();
			if(connectionOptions != null && connectionOptions.getChunkSize() != null
				&& connectionOptions.getChunkSize() > 0)
			{
				final List<DefaultHttpObject> httpMessages = new ArrayList<>();
				final ByteBuf body = this.getBody(httpResponse);
				final DefaultHttpResponse defaultHttpResponse = new DefaultHttpResponse(
					HttpVersion.HTTP_1_1,
					this.getStatus(httpResponse)
				);
				this.setHeaders(httpResponse, defaultHttpResponse, body);
				HttpUtil.setTransferEncodingChunked(defaultHttpResponse, true);
				this.setCookies(httpResponse, defaultHttpResponse);
				httpMessages.add(defaultHttpResponse);
				
				final ByteBuf[] chunks = this.bodyDecoderEncoder.bodyToByteBuf(
					httpResponse.getBody(),
					httpResponse.getFirstHeader(CONTENT_TYPE.toString()),
					connectionOptions.getChunkSize());
				for(int i = 0; i < chunks.length - 1; i++)
				{
					final DefaultHttpContent defaultHttpContent = new DefaultHttpContent(chunks[i]);
					httpMessages.add(defaultHttpContent);
				}
				httpMessages.add(new DefaultLastHttpContent(chunks[chunks.length - 1]));
				return httpMessages;
			}
			else
			{
				final ByteBuf body = this.getBody(httpResponse);
				final DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(
					HttpVersion.HTTP_1_1,
					this.getStatus(httpResponse),
					body
				);
				this.setHeaders(httpResponse, defaultFullHttpResponse, body);
				this.setCookies(httpResponse, defaultFullHttpResponse);
				return Collections.singletonList(defaultFullHttpResponse);
			}
		}
		catch(final Exception ex)
		{
			LOG.error("Exception encoding response {}", httpResponse, ex);
			return Collections.singletonList(new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1,
				this.getStatus(httpResponse)));
		}
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	private HttpResponseStatus getStatus(final HttpResponse httpResponse)
	{
		final int statusCode = httpResponse.getStatusCode() != null ? httpResponse.getStatusCode() : 200;
		if(!isEmpty(httpResponse.getReasonPhrase()))
		{
			return new HttpResponseStatus(statusCode, httpResponse.getReasonPhrase());
		}
		else
		{
			return HttpResponseStatus.valueOf(statusCode);
		}
	}
	
	private ByteBuf getBody(final HttpResponse httpResponse)
	{
		return this.bodyDecoderEncoder.bodyToByteBuf(
			httpResponse.getBody(),
			httpResponse.getFirstHeader(CONTENT_TYPE.toString()));
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	private void setHeaders(final HttpResponse httpResponse, final DefaultHttpResponse response, final ByteBuf body)
	{
		if(httpResponse.getHeaderMultimap() != null)
		{
			httpResponse
				.getHeaderMultimap()
				.entrySet()
				.stream()
				.flatMap(e -> e.getValue().stream().map(e2 -> Map.entry(e.getKey(), e2.getValue())))
				.forEach(entry ->
					response
						.headers()
						.add(entry.getKey().getValue(), entry.getValue())
				);
		}
		
		// Content-Type
		if(isBlank(httpResponse.getFirstHeader(CONTENT_TYPE.toString()))
			&& httpResponse.getBody() != null
			&& httpResponse.getBody().getContentType() != null)
		{
			response.headers().set(CONTENT_TYPE, httpResponse.getBody().getContentType());
		}
		
		// Content-Length
		final ConnectionOptions connectionOptions = httpResponse.getConnectionOptions();
		if(isBlank(httpResponse.getFirstHeader(CONTENT_LENGTH.toString())))
		{
			final boolean overrideContentLength =
				connectionOptions != null && connectionOptions.getContentLengthHeaderOverride() != null;
			final boolean addContentLength =
				connectionOptions == null || !Boolean.TRUE.equals(connectionOptions.getSuppressContentLengthHeader());
			final boolean chunkedEncoding =
				(connectionOptions != null && connectionOptions.getChunkSize() != null) || response.headers()
					.contains(HttpHeaderNames.TRANSFER_ENCODING);
			if(overrideContentLength)
			{
				response.headers().set(CONTENT_LENGTH, connectionOptions.getContentLengthHeaderOverride());
			}
			else if(addContentLength && !chunkedEncoding)
			{
				response.headers().set(CONTENT_LENGTH, body.readableBytes());
			}
			if(chunkedEncoding)
			{
				response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
			}
		}
		
		// HTTP2 extension headers
		final Integer streamId = httpResponse.getStreamId();
		if(streamId != null)
		{
			response.headers().add(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
		}
	}
	
	private void setCookies(final HttpResponse httpResponse, final DefaultHttpResponse response)
	{
		if(httpResponse.getCookieMap() != null)
		{
			for(final Map.Entry<NottableString, NottableString> cookie : httpResponse.getCookieMap().entrySet())
			{
				if(httpResponse.cookieHeaderDoesNotAlreadyExists(
					cookie.getKey().getValue(),
					cookie.getValue().getValue()))
				{
					response.headers()
						.add(
							SET_COOKIE,
							io.netty.handler.codec.http.cookie.ServerCookieEncoder.LAX.encode(
								new DefaultCookie(cookie.getKey().getValue(), cookie.getValue().getValue())));
				}
			}
		}
	}
}
