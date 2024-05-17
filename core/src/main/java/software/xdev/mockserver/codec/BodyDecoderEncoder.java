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
package software.xdev.mockserver.codec;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import software.xdev.mockserver.model.BinaryBody;
import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.BodyWithContentType;
import software.xdev.mockserver.model.MediaType;
import software.xdev.mockserver.model.StringBody;


@SuppressWarnings("rawtypes")
public class BodyDecoderEncoder
{
	public ByteBuf bodyToByteBuf(final Body body, final String contentTypeHeader)
	{
		final byte[] bytes = this.bodyToBytes(body, contentTypeHeader);
		if(bytes != null)
		{
			return Unpooled.copiedBuffer(bytes);
		}
		else
		{
			return Unpooled.buffer(0, 0);
		}
	}
	
	public ByteBuf[] bodyToByteBuf(final Body body, final String contentTypeHeader, final int chunkSize)
	{
		final byte[][] chunks = split(this.bodyToBytes(body, contentTypeHeader), chunkSize);
		final ByteBuf[] byteBufs = new ByteBuf[chunks.length];
		for(int i = 0; i < chunks.length; i++)
		{
			if(chunks[i] != null)
			{
				byteBufs[i] = Unpooled.copiedBuffer(chunks[i]);
			}
			else
			{
				byteBufs[i] = Unpooled.buffer(0, 0);
			}
		}
		return byteBufs;
	}
	
	public static byte[][] split(final byte[] array, final int chunkSize)
	{
		if(chunkSize < array.length)
		{
			final int numOfChunks = (array.length + chunkSize - 1) / chunkSize;
			final byte[][] output = new byte[numOfChunks][];
			
			for(int i = 0; i < numOfChunks; ++i)
			{
				final int start = i * chunkSize;
				final int length = Math.min(array.length - start, chunkSize);
				
				final byte[] temp = new byte[length];
				System.arraycopy(array, start, temp, 0, length);
				output[i] = temp;
			}
			return output;
		}
		else
		{
			return new byte[][]{array};
		}
	}
	
	byte[] bodyToBytes(final Body body, final String contentTypeHeader)
	{
		if(body != null)
		{
			if(body instanceof BinaryBody)
			{
				return body.getRawBytes();
			}
			else if(body.getValue() instanceof String)
			{
				final Charset contentTypeCharset = MediaType.parse(contentTypeHeader).getCharsetOrDefault();
				final Charset bodyCharset = body.getCharset(contentTypeCharset);
				return ((String)body.getValue()).getBytes(
					bodyCharset != null ? bodyCharset : MediaType.DEFAULT_TEXT_HTTP_CHARACTER_SET);
			}
			else
			{
				return body.getRawBytes();
			}
		}
		return null;
	}
	
	public BodyWithContentType byteBufToBody(final ByteBuf content, final String contentTypeHeader)
	{
		if(content != null && content.readableBytes() > 0)
		{
			final byte[] bodyBytes = new byte[content.readableBytes()];
			content.readBytes(bodyBytes);
			return this.bytesToBody(bodyBytes, contentTypeHeader);
		}
		return null;
	}
	
	public BodyWithContentType bytesToBody(final byte[] bodyBytes, final String contentTypeHeader)
	{
		if(bodyBytes.length > 0)
		{
			final MediaType mediaType = MediaType.parse(contentTypeHeader);
			if(mediaType.isString())
			{
				return new StringBody(
					new String(bodyBytes, mediaType.getCharsetOrDefault()),
					bodyBytes,
					false,
					isNotBlank(contentTypeHeader) ? mediaType : null
				);
			}
			else
			{
				return new BinaryBody(bodyBytes, mediaType);
			}
		}
		return null;
	}
}
