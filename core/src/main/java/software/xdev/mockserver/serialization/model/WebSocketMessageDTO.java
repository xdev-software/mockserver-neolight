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


public class WebSocketMessageDTO
{
	private String type;
	private String value;
	
	public String getType()
	{
		return this.type;
	}
	
	public WebSocketMessageDTO setType(final String type)
	{
		this.type = type;
		return this;
	}
	
	public String getValue()
	{
		return this.value;
	}
	
	public WebSocketMessageDTO setValue(final String value)
	{
		this.value = value;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final WebSocketMessageDTO that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.getType(), that.getType()) && Objects.equals(this.getValue(), that.getValue());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.getType(), this.getValue());
	}
}
