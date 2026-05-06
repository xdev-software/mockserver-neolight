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
package software.xdev.mockserver.proxyconfiguration;

import static io.netty.handler.codec.http.HttpHeaderNames.PROXY_AUTHORIZATION;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.ObjectWithJsonToString;


public final class ProxyConfiguration extends ObjectWithJsonToString
{
	private final Type type;
	private final InetSocketAddress proxyAddress;
	private final String username;
	private final String password;
	
	private ProxyConfiguration(
		final Type type,
		final InetSocketAddress proxyAddress,
		final String username,
		final String password)
	{
		this.type = type;
		this.proxyAddress = proxyAddress;
		this.username = username;
		this.password = password;
	}
	
	public static List<ProxyConfiguration> proxyConfiguration(final Configuration configuration)
	{
		final List<ProxyConfiguration> proxyConfigurations = new ArrayList<>();
		final String username = configuration.forwardProxyAuthenticationUsername();
		final String password = configuration.forwardProxyAuthenticationPassword();
		
		final InetSocketAddress httpProxySocketAddress = configuration.forwardHttpProxy();
		if(httpProxySocketAddress != null)
		{
			proxyConfigurations.add(proxyConfiguration(Type.HTTP, httpProxySocketAddress, username, password));
		}
		
		final InetSocketAddress socksProxySocketAddress = configuration.forwardSocksProxy();
		if(socksProxySocketAddress != null)
		{
			if(proxyConfigurations.isEmpty())
			{
				proxyConfigurations.add(proxyConfiguration(Type.SOCKS5, socksProxySocketAddress, username, password));
			}
			else
			{
				throw new IllegalArgumentException(
					"Invalid proxy configuration it is not possible to configure HTTP or HTTPS proxy at the same time "
						+ "as a SOCKS proxy, please choose either HTTP(S) proxy OR a SOCKS proxy");
			}
		}
		
		return proxyConfigurations;
	}
	
	public static ProxyConfiguration proxyConfiguration(final Type type, final String address)
	{
		return proxyConfiguration(type, address, null, null);
	}
	
	public static ProxyConfiguration proxyConfiguration(final Type type, final InetSocketAddress address)
	{
		return proxyConfiguration(type, address, null, null);
	}
	
	@SuppressWarnings("PMD.PreserveStackTrace")
	public static ProxyConfiguration proxyConfiguration(
		final Type type,
		final String address,
		final String username,
		final String password)
	{
		final String[] addressParts = address.split(":");
		if(addressParts.length != 2)
		{
			throw new IllegalArgumentException(
				"Proxy address must be in the format <host>:<ip>, for example 127.0.0.1:9090 or localhost:9090");
		}
		else
		{
			try
			{
				return proxyConfiguration(
					type,
					new InetSocketAddress(addressParts[0], Integer.parseInt(addressParts[1])),
					username,
					password);
			}
			catch(final NumberFormatException nfe)
			{
				throw new IllegalArgumentException("Proxy address port \"" + addressParts[1] + "\" into an integer");
			}
		}
	}
	
	public static ProxyConfiguration proxyConfiguration(
		final Type type,
		final InetSocketAddress address,
		final String username,
		final String password)
	{
		return new ProxyConfiguration(type, address, username, password);
	}
	
	public Type getType()
	{
		return this.type;
	}
	
	public InetSocketAddress getProxyAddress()
	{
		return this.proxyAddress;
	}
	
	public String getUsername()
	{
		return this.username;
	}
	
	public String getPassword()
	{
		return this.password;
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public ProxyConfiguration addProxyAuthenticationHeader(final HttpRequest httpRequest)
	{
		if(isNotBlank(this.username) && isNotBlank(this.password))
		{
			httpRequest.withHeader(
				PROXY_AUTHORIZATION.toString(),
				"Basic " + Base64.encode(
					Unpooled.copiedBuffer(this.username + ':' + this.password, StandardCharsets.UTF_8),
					false).toString(StandardCharsets.US_ASCII)
			);
		}
		return this;
	}
	
	public enum Type
	{
		HTTP,
		SOCKS5
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final ProxyConfiguration that))
		{
			return false;
		}
		return this.getType() == that.getType() && Objects.equals(this.getProxyAddress(), that.getProxyAddress())
			&& Objects.equals(this.getUsername(), that.getUsername()) && Objects.equals(
			this.getPassword(),
			that.getPassword());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getType(), this.getProxyAddress(), this.getUsername(), this.getPassword());
	}
}
