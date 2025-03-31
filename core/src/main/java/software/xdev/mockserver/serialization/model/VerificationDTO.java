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

import static software.xdev.mockserver.verify.Verification.verification;
import static software.xdev.mockserver.verify.VerificationTimes.once;

import java.util.Objects;

import software.xdev.mockserver.model.ExpectationId;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.verify.Verification;


public class VerificationDTO extends ObjectWithJsonToString implements DTO<Verification>
{
	private RequestDefinitionDTO httpRequest;
	private ExpectationId expectationId;
	private VerificationTimesDTO times;
	private Integer maximumNumberOfRequestToReturnInVerificationFailure;
	
	public VerificationDTO(final Verification verification)
	{
		if(verification != null)
		{
			if(verification.getHttpRequest() instanceof HttpRequest)
			{
				this.httpRequest = new HttpRequestDTO((HttpRequest)verification.getHttpRequest());
			}
			this.expectationId = verification.getExpectationId();
			this.times = new VerificationTimesDTO(verification.getTimes());
			this.maximumNumberOfRequestToReturnInVerificationFailure =
				verification.getMaximumNumberOfRequestToReturnInVerificationFailure();
		}
	}
	
	public VerificationDTO()
	{
	}
	
	@Override
	public Verification buildObject()
	{
		return verification()
			.withRequest(this.httpRequest != null ? this.httpRequest.buildObject() : null)
			.withExpectationId(this.expectationId)
			.withTimes(this.times != null ? this.times.buildObject() : once())
			.withMaximumNumberOfRequestToReturnInVerificationFailure(
				this.maximumNumberOfRequestToReturnInVerificationFailure);
	}
	
	public RequestDefinitionDTO getHttpRequest()
	{
		return this.httpRequest;
	}
	
	public VerificationDTO setHttpRequest(final HttpRequestDTO httpRequest)
	{
		this.httpRequest = httpRequest;
		return this;
	}
	
	public ExpectationId getExpectationId()
	{
		return this.expectationId;
	}
	
	public VerificationDTO setExpectationId(final ExpectationId expectationId)
	{
		this.expectationId = expectationId;
		return this;
	}
	
	public VerificationTimesDTO getTimes()
	{
		return this.times;
	}
	
	public VerificationDTO setTimes(final VerificationTimesDTO times)
	{
		this.times = times;
		return this;
	}
	
	public Integer getMaximumNumberOfRequestToReturnInVerificationFailure()
	{
		return this.maximumNumberOfRequestToReturnInVerificationFailure;
	}
	
	public VerificationDTO setMaximumNumberOfRequestToReturnInVerificationFailure(
		final Integer maximumNumberOfRequestToReturnInVerificationFailure)
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
		if(!(o instanceof final VerificationDTO that))
		{
			return false;
		}
		return Objects.equals(this.getHttpRequest(), that.getHttpRequest()) && Objects.equals(
			this.getExpectationId(),
			that.getExpectationId()) && Objects.equals(this.getTimes(), that.getTimes()) && Objects.equals(
			this.getMaximumNumberOfRequestToReturnInVerificationFailure(),
			that.getMaximumNumberOfRequestToReturnInVerificationFailure());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			this.getHttpRequest(),
			this.getExpectationId(),
			this.getTimes(),
			this.getMaximumNumberOfRequestToReturnInVerificationFailure());
	}
}
