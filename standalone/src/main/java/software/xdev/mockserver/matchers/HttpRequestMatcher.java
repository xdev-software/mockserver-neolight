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
package software.xdev.mockserver.matchers;

import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.mock.listeners.MockServerMatcherNotifier;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.RequestDefinition;

import java.util.List;

public interface HttpRequestMatcher extends Matcher<RequestDefinition> {

    List<HttpRequest> getHttpRequests();

    boolean matches(final RequestDefinition request);

    boolean matches(MatchDifference context, RequestDefinition httpRequest);

    Expectation getExpectation();

    boolean update(Expectation expectation);

    boolean update(RequestDefinition requestDefinition);

    @SuppressWarnings("UnusedReturnValue")
    HttpRequestMatcher setResponseInProgress(boolean responseInProgress);

    boolean isResponseInProgress();

    MockServerMatcherNotifier.Cause getSource();

    @SuppressWarnings("UnusedReturnValue")
    HttpRequestMatcher withSource(MockServerMatcherNotifier.Cause source);

    boolean isActive();

}
