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

import java.nio.charset.Charset;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;


public abstract class BodyWithContentType<T> extends Body<T>
{
	private int hashCode;
	protected final MediaType contentType;
	
	protected BodyWithContentType(final Type type, final MediaType contentType)
	{
		super(type);
		this.contentType = contentType;
	}
	
	@JsonIgnore
	Charset determineCharacterSet(final MediaType mediaType, final Charset defaultCharset)
	{
		if(mediaType != null)
		{
			final Charset charset = mediaType.getCharset();
			if(charset != null)
			{
				return charset;
			}
		}
		return defaultCharset;
	}
	
	@Override
	@JsonIgnore
	public Charset getCharset(final Charset defaultCharset)
	{
		return this.determineCharacterSet(this.contentType, defaultCharset);
	}
	
	@Override
	public String getContentType()
	{
		return this.contentType != null ? this.contentType.toString() : null;
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
		final BodyWithContentType<?> that = (BodyWithContentType<?>)o;
		return Objects.equals(this.contentType, that.contentType);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(super.hashCode(), this.contentType);
		}
		return this.hashCode;
	}
}
