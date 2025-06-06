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

import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.util.Base64;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.xdev.mockserver.model.BinaryBody;
import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.ParameterBody;
import software.xdev.mockserver.model.RegexBody;
import software.xdev.mockserver.model.StringBody;
import software.xdev.mockserver.serialization.ObjectMapperFactory;


public abstract class BodyDTO extends NotDTO implements DTO<Body<?>>
{
	private static final Logger LOG = LoggerFactory.getLogger(BodyDTO.class);
	
	private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper();
	private final Body.Type type;
	private Boolean optional;
	
	protected BodyDTO(final Body.Type type, final Boolean not)
	{
		super(not);
		this.type = type;
	}
	
	public static BodyDTO createDTO(final Body<?> body)
	{
		BodyDTO result = null;
		
		if(body instanceof final BinaryBody typedDTO)
		{
			result = new BinaryBodyDTO(typedDTO, typedDTO.getNot());
		}
		else if(body instanceof final ParameterBody typedDTO)
		{
			result = new ParameterBodyDTO(typedDTO, typedDTO.getNot());
		}
		else if(body instanceof final RegexBody typedDTO)
		{
			result = new RegexBodyDTO(typedDTO, typedDTO.getNot());
		}
		else if(body instanceof final StringBody typedDTO)
		{
			result = new StringBodyDTO(typedDTO, typedDTO.getNot());
		}
		
		if(result != null)
		{
			result.withOptional(body.getOptional());
		}
		
		return result;
	}
	
	public static String toString(final BodyDTO body)
	{
		if(body instanceof final BinaryBodyDTO typedDTO)
		{
			return Base64.getEncoder().encodeToString(typedDTO.getBase64Bytes());
		}
		else if(body instanceof final ParameterBodyDTO typedDTO)
		{
			try
			{
				return OBJECT_MAPPER.writeValueAsString(typedDTO.getParameters().getMultimap());
			}
			catch(final Exception ex)
			{
				LOG.error(
					"Serialising parameter body into json string for javascript template {}",
					isNotBlank(ex.getMessage()) ? " " + ex.getMessage() : "",
					ex);
				return "";
			}
		}
		else if(body instanceof final RegexBodyDTO typedDTO)
		{
			return typedDTO.getRegex();
		}
		else if(body instanceof final StringBodyDTO typedDTO)
		{
			return typedDTO.getString();
		}
		
		return "";
	}
	
	public Body.Type getType()
	{
		return this.type;
	}
	
	public Boolean getOptional()
	{
		return this.optional;
	}
	
	public BodyDTO withOptional(final Boolean optional)
	{
		this.optional = optional;
		return this;
	}
	
	@Override
	public abstract Body<?> buildObject();
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final BodyDTO bodyDTO))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return this.getType() == bodyDTO.getType() && Objects.equals(this.getOptional(), bodyDTO.getOptional());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.getType(), this.getOptional());
	}
}
