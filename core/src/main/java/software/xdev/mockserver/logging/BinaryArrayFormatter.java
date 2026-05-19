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
package software.xdev.mockserver.logging;

import static software.xdev.mockserver.character.Character.NEW_LINE;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBufUtil;


public final class BinaryArrayFormatter
{
	public static String byteArrayToString(final byte[] bytes)
	{
		if(bytes != null && bytes.length > 0)
		{
			return "base64:" + NEW_LINE + "  " + formatFixedLength(Base64.getEncoder().encodeToString(bytes)) + NEW_LINE
				+ "hex:" + NEW_LINE + "  " + formatFixedLength(bytesToHex(bytes));
		}
		return "base64:" + NEW_LINE + NEW_LINE
			+ "hex:" + NEW_LINE;
	}
	
	private static final Pattern PATTERN_FORMAT_FIXED_LENGTH = Pattern.compile("(?<=\\G.{64})");
	
	public static String formatFixedLengthHex(final byte[] bytes)
	{
		return formatFixedLength(ByteBufUtil.hexDump(bytes));
	}
	
	private static String formatFixedLength(final String s)
	{
		return String.join("\n", PATTERN_FORMAT_FIXED_LENGTH.split(s));
	}
	
	private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
	
	@SuppressWarnings("checkstyle:MagicNumber")
	private static String bytesToHex(final byte[] bytes)
	{
		final byte[] hexChars = new byte[bytes.length * 2];
		for(int j = 0; j < bytes.length; j++)
		{
			final int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars, StandardCharsets.UTF_8);
	}
	
	private BinaryArrayFormatter()
	{
	}
}
