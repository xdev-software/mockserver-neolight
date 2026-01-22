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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;


public abstract class Body<T> extends Not
{
	private int hashCode;
	private final Type type;
	private Boolean optional;
	
	protected Body(final Type type)
	{
		this.type = type;
	}
	
	public Type getType()
	{
		return this.type;
	}
	
	public Boolean getOptional()
	{
		return this.optional;
	}
	
	public Body<T> withOptional(final Boolean optional)
	{
		this.optional = optional;
		return this;
	}
	
	public abstract T getValue();
	
	@JsonIgnore
	public byte[] getRawBytes()
	{
		return this.toString().getBytes(UTF_8);
	}
	
	@JsonIgnore
	public Charset getCharset(final Charset defaultCharset)
	{
		if(this instanceof BodyWithContentType)
		{
			return this.getCharset(defaultCharset);
		}
		return defaultCharset;
	}
	
	public String getContentType()
	{
		if(this instanceof BodyWithContentType)
		{
			return this.getContentType();
		}
		return null;
	}
	
	public enum Type
	{
		BINARY,
		PARAMETERS,
		REGEX,
		STRING,
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
		final Body<?> body = (Body<?>)o;
		return this.type == body.type && Objects.equals(this.optional, body.optional);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(super.hashCode(), this.type, this.optional);
		}
		return this.hashCode;
	}
}
