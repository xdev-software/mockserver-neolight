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
package software.xdev.mockserver.serialization;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.Base64;


public final class Base64Converter
{
	private static final Base64.Encoder ENCODER = Base64.getEncoder();
	
	public static String bytesToBase64String(final byte[] data)
	{
		return bytesToBase64String(data, UTF_8);
	}
	
	public static String bytesToBase64String(final byte[] data, final Charset charset)
	{
		return new String(ENCODER.encode(data), charset);
	}
	
	private Base64Converter()
	{
	}
}
