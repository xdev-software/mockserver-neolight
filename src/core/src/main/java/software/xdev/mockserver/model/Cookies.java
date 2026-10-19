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

import java.util.List;
import java.util.Map;


public class Cookies extends KeysAndValues<Cookie, Cookies>
{
	public Cookies(final List<Cookie> cookies)
	{
		this.withEntries(cookies);
	}
	
	public Cookies(final Cookie... cookies)
	{
		this.withEntries(cookies);
	}
	
	public Cookies(final Map<NottableString, NottableString> cookies)
	{
		super(cookies);
	}
	
	public static Cookies cookies(final Cookie... cookies)
	{
		return new Cookies(cookies);
	}
	
	@Override
	public Cookie build(final NottableString name, final NottableString value)
	{
		return new Cookie(name, value);
	}
	
	@SuppressWarnings("checkstyle:NoClone")
	@Override
	public Cookies clone()
	{
		return new Cookies(this.getMap());
	}
}
