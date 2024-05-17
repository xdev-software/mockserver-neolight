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

import software.xdev.mockserver.model.ExpectationId;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.verify.Verification;

import static software.xdev.mockserver.verify.Verification.verification;
import static software.xdev.mockserver.verify.VerificationTimes.once;

import java.util.Objects;


public class VerificationDTO extends ObjectWithJsonToString implements DTO<Verification> {
    private RequestDefinitionDTO httpRequest;
    private ExpectationId expectationId;
    private VerificationTimesDTO times;
    private Integer maximumNumberOfRequestToReturnInVerificationFailure;

    public VerificationDTO(Verification verification) {
        if (verification != null) {
            if (verification.getHttpRequest() instanceof HttpRequest) {
                httpRequest = new HttpRequestDTO((HttpRequest) verification.getHttpRequest());
            }
            expectationId = verification.getExpectationId();
            times = new VerificationTimesDTO(verification.getTimes());
            maximumNumberOfRequestToReturnInVerificationFailure = verification.getMaximumNumberOfRequestToReturnInVerificationFailure();
        }
    }

    public VerificationDTO() {
    }

    public Verification buildObject() {
        return verification()
            .withRequest((httpRequest != null ? httpRequest.buildObject() : null))
            .withExpectationId(expectationId)
            .withTimes((times != null ? times.buildObject() : once()))
            .withMaximumNumberOfRequestToReturnInVerificationFailure(maximumNumberOfRequestToReturnInVerificationFailure);
    }

    public RequestDefinitionDTO getHttpRequest() {
        return httpRequest;
    }

    public VerificationDTO setHttpRequest(HttpRequestDTO httpRequest) {
        this.httpRequest = httpRequest;
        return this;
    }

    public ExpectationId getExpectationId() {
        return expectationId;
    }

    public VerificationDTO setExpectationId(ExpectationId expectationId) {
        this.expectationId = expectationId;
        return this;
    }

    public VerificationTimesDTO getTimes() {
        return times;
    }

    public VerificationDTO setTimes(VerificationTimesDTO times) {
        this.times = times;
        return this;
    }

    public Integer getMaximumNumberOfRequestToReturnInVerificationFailure() {
        return maximumNumberOfRequestToReturnInVerificationFailure;
    }

    public VerificationDTO setMaximumNumberOfRequestToReturnInVerificationFailure(Integer maximumNumberOfRequestToReturnInVerificationFailure) {
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
		return Objects.equals(getHttpRequest(), that.getHttpRequest()) && Objects.equals(
            getExpectationId(),
            that.getExpectationId()) && Objects.equals(getTimes(), that.getTimes()) && Objects.equals(
            getMaximumNumberOfRequestToReturnInVerificationFailure(),
            that.getMaximumNumberOfRequestToReturnInVerificationFailure());
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(
            getHttpRequest(),
            getExpectationId(),
            getTimes(),
            getMaximumNumberOfRequestToReturnInVerificationFailure());
    }
}
