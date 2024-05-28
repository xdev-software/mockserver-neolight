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

import software.xdev.mockserver.matchers.TimeToLive;


public class TimeToLiveToJavaSerializer implements ToJavaSerializer<TimeToLive>
{
	@Override
	public String serialize(final int numberOfSpacesToIndent, final TimeToLive timeToLive)
	{
		final StringBuilder output = new StringBuilder();
		if(timeToLive != null)
		{
			this.appendNewLineAndIndent(numberOfSpacesToIndent * INDENT_SIZE, output);
			if(timeToLive.isUnlimited())
			{
				output.append("TimeToLive.unlimited()");
			}
			else
			{
				output.append("TimeToLive.exactly(TimeUnit.")
					.append(timeToLive.getTimeUnit().name())
					.append(", ")
					.append(timeToLive.getTimeToLive())
					.append("L)");
			}
		}
		
		return output.toString();
	}
	
	private StringBuilder appendNewLineAndIndent(final int numberOfSpacesToIndent, final StringBuilder output)
	{
		return output.append(NEW_LINE).append(" ".repeat(numberOfSpacesToIndent));
	}
}
