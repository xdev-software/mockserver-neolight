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
package software.xdev.mockserver.serialization.java;

import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.serialization.java.ExpectationToJavaSerializer.INDENT_SIZE;

import java.util.Arrays;
import java.util.List;

import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.NottableString;


public class HeaderToJavaSerializer implements MultiValueToJavaSerializer<Header>
{
	@Override
	public String serialize(final int numberOfSpacesToIndent, final Header header)
	{
		final StringBuilder output = new StringBuilder();
		output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent * INDENT_SIZE));
		final String serializedKey = NottableStringToJavaSerializer.serialize(header.getName(), false);
		output.append("new Header(").append(serializedKey);
		for(final NottableString value : header.getValues())
		{
			output.append(", ").append(NottableStringToJavaSerializer.serialize(value, serializedKey.endsWith(")")));
		}
		output.append(')');
		return output.toString();
	}
	
	@Override
	public String serializeAsJava(final int numberOfSpacesToIndent, final List<Header> headers)
	{
		final StringBuilder output = new StringBuilder();
		for(int i = 0; i < headers.size(); i++)
		{
			output.append(this.serialize(numberOfSpacesToIndent, headers.get(i)));
			if(i < (headers.size() - 1))
			{
				output.append(',');
			}
		}
		return output.toString();
	}
	
	@Override
	public String serializeAsJava(final int numberOfSpacesToIndent, final Header... object)
	{
		return this.serializeAsJava(numberOfSpacesToIndent, Arrays.asList(object));
	}
}
