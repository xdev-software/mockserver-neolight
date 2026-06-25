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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import software.xdev.mockserver.model.BinaryBody;
import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.BodyWithContentType;
import software.xdev.mockserver.model.MediaType;
import software.xdev.mockserver.model.StringBody;


public abstract class BodyWithContentTypeDTO extends BodyDTO
{
	protected final String contentType;
	
	protected BodyWithContentTypeDTO(final Body.Type type, final Boolean not, final Body<?> body)
	{
		super(type, not);
		this.contentType = body.getContentType();
		this.withOptional(body.getOptional());
	}
	
	public static BodyWithContentTypeDTO createWithContentTypeDTO(final BodyWithContentType<?> body)
	{
		BodyWithContentTypeDTO result = null;
		
		if(body instanceof final BinaryBody binaryBody)
		{
			result = new BinaryBodyDTO(binaryBody, binaryBody.getNot());
		}
		else if(body instanceof final StringBody stringBody)
		{
			result = new StringBodyDTO(stringBody, stringBody.getNot());
		}
		
		if(result != null)
		{
			result.withOptional(body.getOptional());
		}
		
		return result;
	}
	
	public String getContentType()
	{
		return this.contentType;
	}
	
	@JsonIgnore
	MediaType getMediaType()
	{
		return this.contentType != null ? MediaType.parse(this.contentType) : null;
	}
	
	@Override
	public abstract BodyWithContentType<?> buildObject();
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final BodyWithContentTypeDTO that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.getContentType(), that.getContentType());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.getContentType());
	}
}
