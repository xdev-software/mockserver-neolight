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

import software.xdev.mockserver.model.ConnectionOptions;


public class ConnectionOptionsToJavaSerializer implements ToJavaSerializer<ConnectionOptions>
{
	@Override
	public String serialize(final int numberOfSpacesToIndent, final ConnectionOptions connectionOptions)
	{
		final StringBuilder output = new StringBuilder();
		if(connectionOptions != null)
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output).append("connectionOptions()");
			if(connectionOptions.getSuppressContentLengthHeader() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(
						".withSuppressContentLengthHeader(")
					.append(connectionOptions.getSuppressContentLengthHeader())
					.append(")");
			}
			if(connectionOptions.getContentLengthHeaderOverride() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(
						".withContentLengthHeaderOverride(")
					.append(connectionOptions.getContentLengthHeaderOverride())
					.append(")");
			}
			if(connectionOptions.getSuppressConnectionHeader() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(
						".withSuppressConnectionHeader(")
					.append(connectionOptions.getSuppressConnectionHeader())
					.append(")");
			}
			if(connectionOptions.getChunkSize() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
					.append(".withChunkSize(")
					.append(connectionOptions.getChunkSize())
					.append(")");
			}
			if(connectionOptions.getKeepAliveOverride() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(
					".withKeepAliveOverride(").append(connectionOptions.getKeepAliveOverride()).append(")");
			}
			if(connectionOptions.getCloseSocket() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output)
					.append(".withCloseSocket(")
					.append(connectionOptions.getCloseSocket())
					.append(")");
			}
			if(connectionOptions.getCloseSocketDelay() != null)
			{
				this.appendNewLineAndIndent((numberOfSpacesToIndent + 1) * INDENT_SIZE, output).append(
						".withCloseSocketDelay(")
					.append(new DelayToJavaSerializer().serialize(0, connectionOptions.getCloseSocketDelay()))
					.append(")");
			}
		}
		return output.toString();
	}
	
	private StringBuilder appendNewLineAndIndent(final int numberOfSpacesToIndent, final StringBuilder output)
	{
		return output.append(NEW_LINE).append("".repeat(numberOfSpacesToIndent));
	}
}
