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


public class WebSocketErrorDTO
{
	private String message;
	private String webSocketCorrelationId;
	
	public String getMessage()
	{
		return this.message;
	}
	
	public WebSocketErrorDTO setMessage(final String message)
	{
		this.message = message;
		return this;
	}
	
	public String getWebSocketCorrelationId()
	{
		return this.webSocketCorrelationId;
	}
	
	public WebSocketErrorDTO setWebSocketCorrelationId(final String webSocketCorrelationId)
	{
		this.webSocketCorrelationId = webSocketCorrelationId;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final WebSocketErrorDTO that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.getMessage(), that.getMessage()) && Objects.equals(
			this.getWebSocketCorrelationId(),
			that.getWebSocketCorrelationId());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), this.getMessage(), this.getWebSocketCorrelationId());
	}
}
