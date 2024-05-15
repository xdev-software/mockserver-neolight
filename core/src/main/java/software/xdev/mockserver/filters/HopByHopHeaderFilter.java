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
package software.xdev.mockserver.filters;

import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HopByHopHeaderFilter {

    private static final List<String> requestHeadersToRemove = Arrays.asList(
        "proxy-connection",
        "connection",
        "keep-alive",
        "transfer-encoding",
        "te",
        "trailer",
        "proxy-authorization",
        "proxy-authenticate",
        "upgrade"
    );

    private static final List<String> responseHeadersToRemove = Arrays.asList(
        "proxy-connection",
        "connection",
        "keep-alive",
        "transfer-encoding",
        "content-length",
        "te",
        "trailer",
        "upgrade"
    );

    public HttpRequest onRequest(HttpRequest request) {
        if (request != null) {
            Headers headers = new Headers();
            for (Header header : request.getHeaderList()) {
                if (!requestHeadersToRemove.contains(header.getName().getValue().toLowerCase(Locale.ENGLISH))) {
                    headers.withEntry(header);
                }
            }
            HttpRequest clonedRequest = request.clone();
            if (!headers.isEmpty()) {
                clonedRequest.withHeaders(headers);
            }
            return clonedRequest;
        } else {
            return null;
        }
    }

    public HttpResponse onResponse(HttpResponse response) {
        if (response != null) {
            Headers headers = new Headers();
            for (Header header : response.getHeaderList()) {
                if (!responseHeadersToRemove.contains(header.getName().getValue().toLowerCase(Locale.ENGLISH))) {
                    headers.withEntry(header);
                }
            }
            HttpResponse clonedResponse = response.clone();
            if (!headers.isEmpty()) {
                clonedResponse.withHeaders(headers);
            }
            return clonedResponse;
        } else {
            return null;
        }
    }

}
