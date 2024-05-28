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

import software.xdev.mockserver.model.Delay;


public class DelayToJavaSerializer implements ToJavaSerializer<Delay>
{
	@Override
	public String serialize(final int numberOfSpacesToIndent, final Delay delay)
	{
		final StringBuilder output = new StringBuilder();
		if(delay != null)
		{
			output.append("new Delay(TimeUnit.")
				.append(delay.getTimeUnit().name())
				.append(", ")
				.append(delay.getValue())
				.append(")");
		}
		return output.toString();
	}
}
