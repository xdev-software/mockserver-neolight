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
package software.xdev.mockserver.mock.action;

import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;

public interface ExpectationForwardAndResponseCallback extends ExpectationForwardCallback {

    /**
     * Called for every request when expectation condition has been satisfied.
     * The request that satisfied the expectation condition is passed as the
     * parameter and the return value is the request that will be proxied.
     *
     * @param httpRequest the request that satisfied the expectation condition
     * @return the request that will be proxied
     */
    default HttpRequest handle(HttpRequest httpRequest) throws Exception {
        return httpRequest;
    }

    /**
     * Called for every response received from a proxied request, the return
     * value is the returned by MockServer.
     *
     * @param httpRequest  the request that was proxied
     * @param httpResponse the response the MockServer will return
     * @return the request that will be proxied
     */
    HttpResponse handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception;

}
