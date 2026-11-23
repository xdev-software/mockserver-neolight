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
package software.xdev.mockserver.netty.proxy.socks;

import static io.netty.handler.codec.socksx.v5.Socks5AuthMethod.GSSAPI;
import static io.netty.handler.codec.socksx.v5.Socks5AuthMethod.NO_AUTH;
import static io.netty.handler.codec.socksx.v5.Socks5AuthMethod.PASSWORD;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.Socks4CommandType;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;


/**
 * This class is expected to be used from within a {@link ReplayingDecoder}, or with enough bytes available. Readable
 * bytes are not checked and so if not enough bytes are supplied, index exceptions will arise.
 */
@SuppressWarnings("checkstyle:FinalParameters")
public final class SocksDetector
{
	private SocksDetector()
	{
	}
	
	public static boolean isSocks4(final ByteBuf msg, final int actualReadableBytes)
	{
		// first byte has to be 4
		int i = msg.readerIndex();
		if(SocksVersion.valueOf(msg.getByte(i++)) != SocksVersion.SOCKS4a)
		{
			return false;
		}
		
		// second byte has to be 1 or 2
		final Socks4CommandType commandType = Socks4CommandType.valueOf(msg.getByte(i++));
		if(!(commandType.equals(Socks4CommandType.CONNECT) || commandType.equals(Socks4CommandType.BIND)))
		{
			return false;
		}
		
		if(-1 == (i = consumeFields(msg, i + 2)))
		{
			return false;
		}
		
		// end of available bytes reached
		// if not, it is probably not SOCKS4
		// do this check last so that any waiting for data is already done
		return i == actualReadableBytes;
	}
	
	private static int consumeFields(final ByteBuf msg, int i)
	{
		final boolean socks4a = msg.getByte(i++) == 0
			&& msg.getByte(i) == 0
			&& msg.getByte(i + 1) == 0
			&& msg.getByte(i + 2) != 0;
		
		if(-1 == (i = consumeUsername(msg, i + 3)))
		{
			return -1;
		}
		
		if(socks4a
			&& -1 == (i = consumeHostname(msg, i)))
		{
			return -1;
		}
		
		return i;
	}
	
	private static int consumeUsername(final ByteBuf msg, int i)
	{
		// consume the username (maximum 256 characters to not wait for the 0 endlessly if none comes)
		final int j = i + 256;
		while(i < j && msg.getByte(i) != 0)
		{
			i++;
		}
		
		// hostname was not 0-terminated
		if(i == j)
		{
			return -1;
		}
		
		return i + 1;
	}
	
	@SuppressWarnings("checkstyle:EmptyBlock")
	private static int consumeHostname(final ByteBuf msg, int i)
	{
		// empty hostname
		if(msg.getByte(i) == 0)
		{
			return -1;
		}
		
		// consume the remaining hostname (maximum 256 characters to not wait for the 0 endlessly if none comes)
		final int j = i + 256;
		while(++i < j && msg.getByte(i) != 0)
		{
			// Imported from upstream
		}
		
		// hostname was not 0-terminated
		if(i == j)
		{
			return -1;
		}
		
		return i + 1;
	}
	
	public static boolean isSocks5(final ByteBuf msg, final int actualReadableBytes)
	{
		// first byte has to be 5
		if(SocksVersion.valueOf(msg.getByte(msg.readerIndex())) != SocksVersion.SOCKS5)
		{
			return false;
		}
		
		// then the amount of authentication methods
		final byte numberOfAuthenticationMethods = msg.getByte(msg.readerIndex() + 1);
		
		// now the authentication methods
		for(int i = 0; i < numberOfAuthenticationMethods; i++)
		{
			final Socks5AuthMethod authMethod = Socks5AuthMethod.valueOf(msg.getByte(msg.readerIndex() + 2 + i));
			if(!(NO_AUTH.equals(authMethod) || PASSWORD.equals(authMethod) || GSSAPI.equals(authMethod)))
			{
				return false;
			}
		}
		
		// more methods than advertised, either broken request or not actually SOCKS5
		// do this check last so that any waiting for data is already done
		return actualReadableBytes == (2 + numberOfAuthenticationMethods);
	}
}
