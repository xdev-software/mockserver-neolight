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
import software.xdev.mockserver.model.HttpForward;


public class HttpForwardDTO implements DTO<HttpForward>
{
	private String host;
	private Integer port;
	private HttpForward.Scheme scheme;
	private DelayDTO delay;
	
	public HttpForwardDTO(final HttpForward httpForward)
	{
		if(httpForward != null)
		{
			this.host = httpForward.getHost();
			this.port = httpForward.getPort();
			this.scheme = httpForward.getScheme();
			if(httpForward.getDelay() != null)
			{
				this.delay = new DelayDTO(httpForward.getDelay());
			}
		}
	}
	
	public HttpForwardDTO()
	{
	}
	
	@Override
	public HttpForward buildObject()
	{
		Delay delay = null;
		if(this.delay != null)
		{
			delay = this.delay.buildObject();
		}
		return new HttpForward()
			.withHost(this.host)
			.withPort(this.port != null ? this.port : 80)
			.withScheme((this.scheme != null ? this.scheme : HttpForward.Scheme.HTTP))
			.withDelay(delay);
	}
	
	public String getHost()
	{
		return this.host;
	}
	
	public HttpForwardDTO setHost(final String host)
	{
		this.host = host;
		return this;
	}
	
	public Integer getPort()
	{
		return this.port;
	}
	
	public HttpForwardDTO setPort(final Integer port)
	{
		this.port = port;
		return this;
	}
	
	public HttpForward.Scheme getScheme()
	{
		return this.scheme;
	}
	
	public HttpForwardDTO setScheme(final HttpForward.Scheme scheme)
	{
		this.scheme = scheme;
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
		if(!(o instanceof final HttpForwardDTO that))
		{
			return false;
		}
		return Objects.equals(this.getHost(), that.getHost())
			&& Objects.equals(this.getPort(), that.getPort())
			&& this.getScheme() == that.getScheme()
			&& Objects.equals(this.getDelay(), that.getDelay());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getHost(), this.getPort(), this.getScheme(), this.getDelay());
	}
}

