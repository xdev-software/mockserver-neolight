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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class HttpForward extends Action<HttpForward>
{
	private int hashCode;
	private String host;
	private Integer port = 80;
	private Scheme scheme = Scheme.HTTP;
	
	/**
	 * Static builder to create a forward.
	 */
	public static HttpForward forward()
	{
		return new HttpForward();
	}
	
	@Override
	@JsonIgnore
	public Type getType()
	{
		return Type.FORWARD;
	}
	
	public String getHost()
	{
		return this.host;
	}
	
	/**
	 * The host or ip address to forward the request to i.e. "www.mock-server.com"
	 *
	 * @param host a hostname or ip address as a string
	 */
	public HttpForward withHost(final String host)
	{
		this.host = host;
		this.hashCode = 0;
		return this;
	}
	
	public Integer getPort()
	{
		return this.port;
	}
	
	/**
	 * The port to forward the request to i.e. 80.  If not specified the port defaults to 80.
	 *
	 * @param port a port as an integer
	 */
	public HttpForward withPort(final Integer port)
	{
		this.port = port;
		this.hashCode = 0;
		return this;
	}
	
	public Scheme getScheme()
	{
		return this.scheme;
	}
	
	/**
	 * The scheme to use when forwarded the request, either HTTP or HTTPS.  If not specified the scheme defaults to
	 * HTTP.
	 *
	 * @param scheme the scheme as a HttpForward.Scheme value
	 */
	public HttpForward withScheme(final Scheme scheme)
	{
		this.scheme = scheme;
		this.hashCode = 0;
		return this;
	}
	
	public enum Scheme
	{
		HTTP,
		HTTPS
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
		final HttpForward that = (HttpForward)o;
		return Objects.equals(this.host, that.host) &&
			Objects.equals(this.port, that.port) &&
			this.scheme == that.scheme;
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(super.hashCode(), this.host, this.port, this.scheme);
		}
		return this.hashCode;
	}
}
