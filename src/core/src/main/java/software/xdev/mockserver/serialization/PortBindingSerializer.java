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

import software.xdev.mockserver.model.PortBinding;


public class PortBindingSerializer extends AbstractSerializer<PortBinding>
{
	@Override
	public String serialize(final PortBinding portBinding)
	{
		try
		{
			return this.objectWriter.writeValueAsString(portBinding);
		}
		catch(final Exception ex)
		{
			throw new IllegalStateException(
				"Exception while serializing portBinding to JSON with value " + portBinding,
				ex);
		}
	}
	
	@Override
	public PortBinding deserialize(final String jsonPortBinding)
	{
		PortBinding portBinding = null;
		if(jsonPortBinding != null && !jsonPortBinding.isEmpty())
		{
			try
			{
				portBinding = this.objectMapper.readValue(jsonPortBinding, PortBinding.class);
			}
			catch(final Exception ex)
			{
				throw new IllegalArgumentException(
					"exception while parsing PortBinding for [" + jsonPortBinding + "]",
					ex);
			}
		}
		return portBinding;
	}
}
