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

import static software.xdev.mockserver.model.MediaType.DEFAULT_TEXT_HTTP_CHARACTER_SET;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class StringBody extends BodyWithContentType<String>
{
	private int hashCode;
	public static final MediaType DEFAULT_CONTENT_TYPE = MediaType.create("text", "plain");
	private final boolean subString;
	private final String value;
	private final byte[] rawBytes;
	
	public StringBody(final String value)
	{
		this(value, null, false, null);
	}
	
	public StringBody(final String value, final Charset charset)
	{
		this(value, null, false, charset != null ? DEFAULT_CONTENT_TYPE.withCharset(charset) : null);
	}
	
	public StringBody(final String value, final MediaType contentType)
	{
		this(value, null, false, contentType);
	}
	
	public StringBody(final String value, final byte[] rawBytes, final boolean subString, final MediaType contentType)
	{
		super(Type.STRING, contentType);
		this.value = isNotBlank(value) ? value : "";
		this.subString = subString;
		
		if(rawBytes == null && value != null)
		{
			this.rawBytes = value.getBytes(this.determineCharacterSet(contentType, DEFAULT_TEXT_HTTP_CHARACTER_SET));
		}
		else
		{
			this.rawBytes = rawBytes;
		}
	}
	
	public static StringBody exact(final String body)
	{
		return new StringBody(body);
	}
	
	public static StringBody exact(final String body, final Charset charset)
	{
		return new StringBody(body, charset);
	}
	
	public static StringBody exact(final String body, final MediaType contentType)
	{
		return new StringBody(body, contentType);
	}
	
	public static StringBody subString(final String body)
	{
		return new StringBody(body, null, true, null);
	}
	
	public static StringBody subString(final String body, final Charset charset)
	{
		return new StringBody(body, null, true, charset != null ? DEFAULT_CONTENT_TYPE.withCharset(charset) : null);
	}
	
	public static StringBody subString(final String body, final MediaType contentType)
	{
		return new StringBody(body, null, true, contentType);
	}
	
	@Override
	public String getValue()
	{
		return this.value;
	}
	
	@Override
	@JsonIgnore
	public byte[] getRawBytes()
	{
		return this.rawBytes;
	}
	
	public boolean isSubString()
	{
		return this.subString;
	}
	
	@Override
	public String toString()
	{
		return this.value;
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
		final StringBody that = (StringBody)o;
		return this.subString == that.subString
			&& Objects.equals(this.value, that.value)
			&& Arrays.equals(this.rawBytes, that.rawBytes);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			final int result = Objects.hash(super.hashCode(), this.subString, this.value);
			this.hashCode = 31 * result + Arrays.hashCode(this.rawBytes);
		}
		return this.hashCode;
	}
}
