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
package software.xdev.mockserver.serialization.model;

import java.util.Arrays;
import java.util.Objects;

import software.xdev.mockserver.model.StringBody;


public class StringBodyDTO extends BodyWithContentTypeDTO
{
	private final String string;
	private final boolean subString;
	private final byte[] rawBytes;
	
	public StringBodyDTO(final StringBody stringBody)
	{
		this(stringBody, stringBody.getNot());
	}
	
	public StringBodyDTO(final StringBody stringBody, final Boolean not)
	{
		super(stringBody.getType(), not, stringBody);
		this.string = stringBody.getValue();
		this.subString = stringBody.isSubString();
		this.rawBytes = stringBody.getRawBytes();
	}
	
	public String getString()
	{
		return this.string;
	}
	
	public boolean isSubString()
	{
		return this.subString;
	}
	
	public byte[] getRawBytes()
	{
		return this.rawBytes;
	}
	
	@Override
	public StringBody buildObject()
	{
		return (StringBody)new StringBody(
			this.getString(),
			this.getRawBytes(),
			this.isSubString(),
			this.getMediaType()).withOptional(
			this.getOptional());
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final StringBodyDTO that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return this.isSubString() == that.isSubString() && Objects.equals(this.getString(), that.getString())
			&& Objects.deepEquals(this.getRawBytes(), that.getRawBytes());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			super.hashCode(),
			this.getString(),
			this.isSubString(),
			Arrays.hashCode(this.getRawBytes()));
	}
}
