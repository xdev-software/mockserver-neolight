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


public class Parameters extends KeysToMultiValues<Parameter, Parameters>
{
	private String rawParameterString;
	
	public Parameters(final List<Parameter> parameters)
	{
		this.withEntries(parameters);
	}
	
	public Parameters(final Parameter... parameters)
	{
		this.withEntries(parameters);
	}
	
	public Parameters(final Map<NottableString, List<NottableString>> headers)
	{
		super(headers);
	}
	
	public static Parameters parameters(final Parameter... parameters)
	{
		return new Parameters(parameters);
	}
	
	@Override
	public Parameter build(final NottableString name, final Collection<NottableString> values)
	{
		return new Parameter(name, values);
	}
	
	@Override
	protected void isModified()
	{
		this.rawParameterString = null;
	}
	
	@Override
	public Parameters withKeyMatchStyle(final KeyMatchStyle keyMatchStyle)
	{
		super.withKeyMatchStyle(keyMatchStyle);
		return this;
	}
	
	public String getRawParameterString()
	{
		return this.rawParameterString;
	}
	
	public Parameters withRawParameterString(final String rawParameterString)
	{
		this.rawParameterString = rawParameterString;
		return this;
	}
	
	@SuppressWarnings("checkstyle:NoClone")
	@Override
	public Parameters clone()
	{
		return new Parameters(this.getMultimap())
			.withRawParameterString(this.rawParameterString);
	}
}
