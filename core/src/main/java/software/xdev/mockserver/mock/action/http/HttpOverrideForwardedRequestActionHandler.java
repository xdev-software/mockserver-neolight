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
package software.xdev.mockserver.mock.action.http;

import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpOverrideForwardedRequest;
import software.xdev.mockserver.model.HttpRequest;

public class HttpOverrideForwardedRequestActionHandler extends HttpForwardAction {

    public HttpOverrideForwardedRequestActionHandler(MockServerLogger logFormatter, NettyHttpClient httpClient) {
        super(logFormatter, httpClient);
    }

    public HttpForwardActionResult handle(final HttpOverrideForwardedRequest httpOverrideForwardedRequest, final HttpRequest request) {
        if (httpOverrideForwardedRequest != null) {
            HttpRequest requestToSend = request.clone().update(httpOverrideForwardedRequest.getRequestOverride(), httpOverrideForwardedRequest.getRequestModifier());
            return sendRequest(requestToSend, null, httpResponse -> {
                if (httpResponse == null) {
                    return httpOverrideForwardedRequest.getResponseOverride();
                } else {
                    return httpResponse.update(httpOverrideForwardedRequest.getResponseOverride(), httpOverrideForwardedRequest.getResponseModifier());
                }
            });
        } else {
            return sendRequest(request, null, httpResponse -> httpResponse);
        }
    }

}
