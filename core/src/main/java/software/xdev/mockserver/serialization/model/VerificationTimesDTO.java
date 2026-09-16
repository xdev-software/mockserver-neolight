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
package software.xdev.mockserver.serialization.model;

import java.util.Objects;

import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.verify.VerificationTimes;


public class VerificationTimesDTO extends ObjectWithJsonToString implements DTO<VerificationTimes>
{
	private int atLeast;
	private int atMost;
	
	public VerificationTimesDTO(final VerificationTimes times)
	{
		this.atLeast = times.getAtLeast();
		this.atMost = times.getAtMost();
	}
	
	public VerificationTimesDTO()
	{
	}
	
	@Override
	public VerificationTimes buildObject()
	{
		return VerificationTimes.between(this.atLeast, this.atMost);
	}
	
	public int getAtLeast()
	{
		return this.atLeast;
	}
	
	public int getAtMost()
	{
		return this.atMost;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final VerificationTimesDTO that))
		{
			return false;
		}
		return this.getAtLeast() == that.getAtLeast() && this.getAtMost() == that.getAtMost();
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(this.getAtLeast(), this.getAtMost());
	}
}
