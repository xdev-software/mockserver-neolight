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

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import software.xdev.mockserver.serialization.Base64Converter;


public class BinaryBody extends BodyWithContentType<byte[]>
{
	private int hashCode;
	private final byte[] bytes;
	
	public BinaryBody(final byte[] bytes)
	{
		this(bytes, null);
	}
	
	public BinaryBody(final byte[] bytes, final MediaType contentType)
	{
		super(Type.BINARY, contentType);
		this.bytes = bytes;
	}
	
	public static BinaryBody binary(final byte[] body)
	{
		return new BinaryBody(body);
	}
	
	public static BinaryBody binary(final byte[] body, final MediaType contentType)
	{
		return new BinaryBody(body, contentType);
	}
	
	@Override
	public byte[] getValue()
	{
		return this.bytes;
	}
	
	@Override
	@JsonIgnore
	public byte[] getRawBytes()
	{
		return this.bytes;
	}
	
	@Override
	public String toString()
	{
		return this.bytes != null ? Base64Converter.bytesToBase64String(this.bytes) : null;
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
		final BinaryBody that = (BinaryBody)o;
		return Arrays.equals(this.bytes, that.bytes);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			final int result = Objects.hash(super.hashCode());
			this.hashCode = 31 * result + Arrays.hashCode(this.bytes);
		}
		return this.hashCode;
	}
}
