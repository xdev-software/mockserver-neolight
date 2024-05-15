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

import static software.xdev.mockserver.model.HttpRequest.request;

public class Verification extends ObjectWithJsonToString {
    private RequestDefinition httpRequest;
    private ExpectationId expectationId;
    private VerificationTimes times = VerificationTimes.atLeast(1);
    private Integer maximumNumberOfRequestToReturnInVerificationFailure;

    public static Verification verification() {
        return new Verification();
    }

    public Verification withRequest(RequestDefinition requestDefinition) {
        this.httpRequest = requestDefinition;
        return this;
    }

    public RequestDefinition getHttpRequest() {
        return httpRequest;
    }

    public Verification withExpectationId(ExpectationId expectationId) {
        this.expectationId = expectationId;
        return this;
    }

    public ExpectationId getExpectationId() {
        return expectationId;
    }

    public Verification withTimes(VerificationTimes times) {
        this.times = times;
        return this;
    }

    public VerificationTimes getTimes() {
        return times;
    }

    public Integer getMaximumNumberOfRequestToReturnInVerificationFailure() {
        return maximumNumberOfRequestToReturnInVerificationFailure;
    }

    public Verification withMaximumNumberOfRequestToReturnInVerificationFailure(Integer maximumNumberOfRequestToReturnInVerificationFailure) {
        this.maximumNumberOfRequestToReturnInVerificationFailure = maximumNumberOfRequestToReturnInVerificationFailure;
        return this;
    }
}
