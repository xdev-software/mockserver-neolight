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

import software.xdev.mockserver.model.ParameterBody;
import software.xdev.mockserver.model.Parameters;


public class ParameterBodyDTO extends BodyDTO
{
	private final Parameters parameters;
	
	public ParameterBodyDTO(final ParameterBody parameterBody)
	{
		this(parameterBody, null);
	}
	
	public ParameterBodyDTO(final ParameterBody parameterBody, final Boolean not)
	{
		super(parameterBody.getType(), not);
		this.parameters = parameterBody.getValue();
		this.withOptional(parameterBody.getOptional());
	}
	
	public Parameters getParameters()
	{
		return this.parameters;
	}
	
	@Override
	public ParameterBody buildObject()
	{
		return (ParameterBody)new ParameterBody(this.parameters).withOptional(this.getOptional());
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final ParameterBodyDTO that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.getParameters(), that.getParameters());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.getParameters());
	}
}
