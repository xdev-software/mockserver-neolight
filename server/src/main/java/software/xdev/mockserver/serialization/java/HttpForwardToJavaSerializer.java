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

import software.xdev.mockserver.model.HttpForward;


public class HttpForwardToJavaSerializer implements ToJavaSerializer<HttpForward>
{
	@Override
	public String serialize(final int numberOfSpacesToIndent, final HttpForward httpForward)
	{
		final StringBuilder output = new StringBuilder();
		if(httpForward != null)
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("forward()");
			if(httpForward.getHost() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withHost(\"")
					.append(httpForward.getHost())
					.append("\")");
			}
			if(httpForward.getPort() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withPort(")
					.append(httpForward.getPort())
					.append(")");
			}
			if(httpForward.getScheme() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(
					".withScheme(HttpForward.Scheme.").append(httpForward.getScheme()).append(")");
			}
			if(httpForward.getDelay() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(".withDelay(")
					.append(new DelayToJavaSerializer().serialize(0, httpForward.getDelay()))
					.append(")");
			}
		}
		return output.toString();
	}
	
	private StringBuilder appendNewLineAndIndent(final int numberOfSpacesToIndent, final StringBuilder output)
	{
		return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
	}
}
