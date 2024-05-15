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
package software.xdev.mockserver.verify;

import software.xdev.mockserver.model.ExpectationId;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.model.RequestDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VerificationSequence extends ObjectWithJsonToString {
    private List<RequestDefinition> httpRequests = new ArrayList<>();
    private List<ExpectationId> expectationIds = new ArrayList<>();
    private Integer maximumNumberOfRequestToReturnInVerificationFailure;

    public static VerificationSequence verificationSequence() {
        return new VerificationSequence();
    }

    public VerificationSequence withRequests(RequestDefinition... httpRequests) {
        Collections.addAll(this.httpRequests, httpRequests);
        return this;
    }

    public VerificationSequence withRequests(List<RequestDefinition> httpRequests) {
        this.httpRequests = httpRequests;
        return this;
    }

    public List<RequestDefinition> getHttpRequests() {
        return httpRequests;
    }

    public VerificationSequence withExpectationIds(ExpectationId... expectationIds) {
        Collections.addAll(this.expectationIds, expectationIds);
        return this;
    }

    public VerificationSequence withExpectationIds(List<ExpectationId> expectationIds) {
        this.expectationIds = expectationIds;
        return this;
    }

    public List<ExpectationId> getExpectationIds() {
        return expectationIds;
    }

    public Integer getMaximumNumberOfRequestToReturnInVerificationFailure() {
        return maximumNumberOfRequestToReturnInVerificationFailure;
    }

    public VerificationSequence withMaximumNumberOfRequestToReturnInVerificationFailure(Integer maximumNumberOfRequestToReturnInVerificationFailure) {
        this.maximumNumberOfRequestToReturnInVerificationFailure = maximumNumberOfRequestToReturnInVerificationFailure;
        return this;
    }
}
