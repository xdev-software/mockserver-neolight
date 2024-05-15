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

import software.xdev.mockserver.model.*;
import software.xdev.mockserver.verify.VerificationSequence;

import java.util.ArrayList;
import java.util.List;

public class VerificationSequenceDTO extends ObjectWithReflectiveEqualsHashCodeToString implements DTO<VerificationSequence> {
    private List<RequestDefinitionDTO> httpRequests = new ArrayList<>();
    private List<ExpectationId> expectationIds = new ArrayList<>();
    private Integer maximumNumberOfRequestToReturnInVerificationFailure;

    public VerificationSequenceDTO(VerificationSequence verification) {
        if (verification != null) {
            for (RequestDefinition httpRequest : verification.getHttpRequests()) {
                if (httpRequest instanceof HttpRequest) {
                    httpRequests.add(new HttpRequestDTO((HttpRequest) httpRequest));
                } else if (httpRequest instanceof OpenAPIDefinition) {
                    httpRequests.add(new OpenAPIDefinitionDTO((OpenAPIDefinition) httpRequest));
                }
            }
            expectationIds.addAll(verification.getExpectationIds());
            maximumNumberOfRequestToReturnInVerificationFailure = verification.getMaximumNumberOfRequestToReturnInVerificationFailure();
        }
    }

    public VerificationSequenceDTO() {
    }

    public VerificationSequence buildObject() {
        List<RequestDefinition> httpRequests = new ArrayList<>();
        for (RequestDefinitionDTO httpRequest : this.httpRequests) {
            httpRequests.add(httpRequest.buildObject());
        }
        return new VerificationSequence()
            .withRequests(httpRequests)
            .withExpectationIds(expectationIds)
            .withMaximumNumberOfRequestToReturnInVerificationFailure(maximumNumberOfRequestToReturnInVerificationFailure);
    }

    public List<RequestDefinitionDTO> getHttpRequests() {
        return httpRequests;
    }

    public VerificationSequenceDTO setHttpRequests(List<RequestDefinitionDTO> httpRequests) {
        this.httpRequests = httpRequests;
        return this;
    }

    public List<ExpectationId> getExpectationIds() {
        return expectationIds;
    }

    public VerificationSequenceDTO setExpectationIds(List<ExpectationId> expectationIds) {
        this.expectationIds = expectationIds;
        return this;
    }

    public Integer getMaximumNumberOfRequestToReturnInVerificationFailure() {
        return maximumNumberOfRequestToReturnInVerificationFailure;
    }

    public VerificationSequenceDTO setMaximumNumberOfRequestToReturnInVerificationFailure(Integer maximumNumberOfRequestToReturnInVerificationFailure) {
        this.maximumNumberOfRequestToReturnInVerificationFailure = maximumNumberOfRequestToReturnInVerificationFailure;
        return this;
    }
}
