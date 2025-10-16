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
package software.xdev.mockserver.configuration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IntegerStringListParser
{
	private static final Logger LOG = LoggerFactory.getLogger(IntegerStringListParser.class);
	
	public Integer[] toArray(final String integers)
	{
		return this.toList(integers).toArray(new Integer[0]);
	}
	
	List<Integer> toList(final String integers)
	{
		final List<Integer> integerList = new ArrayList<>();
		for(final String integer : integers.split(","))
		{
			try
			{
				integerList.add(Integer.parseInt(integer.trim()));
			}
			catch(final NumberFormatException nfe)
			{
				LOG.error("NumberFormatException converting {} to integer", integer, nfe);
			}
		}
		return integerList;
	}
}
