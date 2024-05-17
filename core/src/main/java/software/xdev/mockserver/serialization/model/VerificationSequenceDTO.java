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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import software.xdev.mockserver.model.ExpectationId;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.verify.VerificationSequence;


public class VerificationSequenceDTO implements DTO<VerificationSequence>
{
	private List<RequestDefinitionDTO> httpRequests = new ArrayList<>();
	private List<ExpectationId> expectationIds = new ArrayList<>();
	private Integer maximumNumberOfRequestToReturnInVerificationFailure;
	
	public VerificationSequenceDTO(final VerificationSequence verification)
	{
		if(verification != null)
		{
			for(final RequestDefinition httpRequest : verification.getHttpRequests())
			{
				if(httpRequest instanceof HttpRequest)
				{
					this.httpRequests.add(new HttpRequestDTO((HttpRequest)httpRequest));
				}
			}
			this.expectationIds.addAll(verification.getExpectationIds());
			this.maximumNumberOfRequestToReturnInVerificationFailure =
				verification.getMaximumNumberOfRequestToReturnInVerificationFailure();
		}
	}
	
	public VerificationSequenceDTO()
	{
	}
	
	@Override
	public VerificationSequence buildObject()
	{
		final List<RequestDefinition> httpRequests = new ArrayList<>();
		for(final RequestDefinitionDTO httpRequest : this.httpRequests)
		{
			httpRequests.add(httpRequest.buildObject());
		}
		return new VerificationSequence()
			.withRequests(httpRequests)
			.withExpectationIds(this.expectationIds)
			.withMaximumNumberOfRequestToReturnInVerificationFailure(this.maximumNumberOfRequestToReturnInVerificationFailure);
	}
	
	public List<RequestDefinitionDTO> getHttpRequests()
	{
		return this.httpRequests;
	}
	
	public VerificationSequenceDTO setHttpRequests(final List<RequestDefinitionDTO> httpRequests)
	{
		this.httpRequests = httpRequests;
		return this;
	}
	
	public List<ExpectationId> getExpectationIds()
	{
		return this.expectationIds;
	}
	
	public VerificationSequenceDTO setExpectationIds(final List<ExpectationId> expectationIds)
	{
		this.expectationIds = expectationIds;
		return this;
	}
	
	public Integer getMaximumNumberOfRequestToReturnInVerificationFailure()
	{
		return this.maximumNumberOfRequestToReturnInVerificationFailure;
	}
	
	public VerificationSequenceDTO setMaximumNumberOfRequestToReturnInVerificationFailure(final Integer maximumNumberOfRequestToReturnInVerificationFailure)
	{
		this.maximumNumberOfRequestToReturnInVerificationFailure = maximumNumberOfRequestToReturnInVerificationFailure;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final VerificationSequenceDTO that))
		{
			return false;
		}
		if(!super.equals(o))
		{
			return false;
		}
		return Objects.equals(this.getHttpRequests(), that.getHttpRequests()) && Objects.equals(
			this.getExpectationIds(),
			that.getExpectationIds()) && Objects.equals(
			this.getMaximumNumberOfRequestToReturnInVerificationFailure(),
			that.getMaximumNumberOfRequestToReturnInVerificationFailure());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			super.hashCode(),
			this.getHttpRequests(),
			this.getExpectationIds(),
			this.getMaximumNumberOfRequestToReturnInVerificationFailure());
	}
}
