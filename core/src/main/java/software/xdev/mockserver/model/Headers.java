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

import java.util.Collection;
import java.util.List;
import java.util.Map;


public class Headers extends KeysToMultiValues<Header, Headers>
{
	public Headers(final List<Header> headers)
	{
		this.withEntries(headers);
	}
	
	public Headers(final Header... headers)
	{
		this.withEntries(headers);
	}
	
	public Headers(final Map<NottableString, List<NottableString>> headers)
	{
		super(headers);
	}
	
	public static Headers headers(final Header... headers)
	{
		return new Headers(headers);
	}
	
	@Override
	public Header build(final NottableString name, final Collection<NottableString> values)
	{
		return new Header(name, values);
	}
	
	@Override
	protected void isModified()
	{
	}
	
	@Override
	public Headers withKeyMatchStyle(final KeyMatchStyle keyMatchStyle)
	{
		super.withKeyMatchStyle(keyMatchStyle);
		return this;
	}
	
	@SuppressWarnings("checkstyle:NoClone")
	@Override
	public Headers clone()
	{
		return new Headers(this.getMultimap());
	}
}
