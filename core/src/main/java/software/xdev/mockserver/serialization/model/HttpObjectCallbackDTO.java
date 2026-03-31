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

import software.xdev.mockserver.model.Delay;
import software.xdev.mockserver.model.HttpObjectCallback;


public class HttpObjectCallbackDTO implements DTO<HttpObjectCallback>
{
	private String clientId;
	private Boolean responseCallback;
	private DelayDTO delay;
	
	public HttpObjectCallbackDTO(final HttpObjectCallback httpObjectCallback)
	{
		if(httpObjectCallback != null)
		{
			this.clientId = httpObjectCallback.getClientId();
			this.responseCallback = httpObjectCallback.getResponseCallback();
			if(httpObjectCallback.getDelay() != null)
			{
				this.delay = new DelayDTO(httpObjectCallback.getDelay());
			}
		}
	}
	
	public HttpObjectCallbackDTO()
	{
	}
	
	@Override
	public HttpObjectCallback buildObject()
	{
		Delay delay = null;
		if(this.delay != null)
		{
			delay = this.delay.buildObject();
		}
		return new HttpObjectCallback()
			.withClientId(this.clientId)
			.withResponseCallback(this.responseCallback)
			.withDelay(delay);
	}
	
	public String getClientId()
	{
		return this.clientId;
	}
	
	public HttpObjectCallbackDTO setClientId(final String clientId)
	{
		this.clientId = clientId;
		return this;
	}
	
	public Boolean getResponseCallback()
	{
		return this.responseCallback;
	}
	
	public HttpObjectCallbackDTO setResponseCallback(final Boolean responseCallback)
	{
		this.responseCallback = responseCallback;
		return this;
	}
	
	public DelayDTO getDelay()
	{
		return this.delay;
	}
	
	public void setDelay(final DelayDTO delay)
	{
		this.delay = delay;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final HttpObjectCallbackDTO that))
		{
			return false;
		}
		return Objects.equals(this.getClientId(), that.getClientId())
			&& Objects.equals(this.getResponseCallback(), that.getResponseCallback()) && Objects.equals(
			this.getDelay(),
			that.getDelay());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getClientId(), this.getResponseCallback(), this.getDelay());
	}
}

