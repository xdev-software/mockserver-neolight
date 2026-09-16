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
package software.xdev.mockserver.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class PortBinding extends ObjectWithJsonToString
{
	private List<Integer> ports = new ArrayList<>();
	
	public static PortBinding portBinding(final Integer... ports)
	{
		return portBinding(Arrays.asList(ports));
	}
	
	public static PortBinding portBinding(final List<Integer> ports)
	{
		return new PortBinding().setPorts(ports);
	}
	
	public List<Integer> getPorts()
	{
		return this.ports;
	}
	
	public PortBinding setPorts(final List<Integer> ports)
	{
		this.ports = ports;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final PortBinding that))
		{
			return false;
		}
		return Objects.equals(this.getPorts(), that.getPorts());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.getPorts());
	}
}
