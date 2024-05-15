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
package software.xdev.mockserver.servlet.responsewriter;

import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mappers.MockServerHttpResponseToHttpServletResponseEncoder;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.responsewriter.ResponseWriter;

import jakarta.servlet.http.HttpServletResponse;

public class ServletResponseWriter extends ResponseWriter {
    private final HttpServletResponse httpServletResponse;
    @SuppressWarnings("FieldMayBeFinal")
    private MockServerHttpResponseToHttpServletResponseEncoder mockServerResponseToHttpServletResponseEncoder;

    public ServletResponseWriter(Configuration configuration, MockServerLogger mockServerLogger, HttpServletResponse httpServletResponse) {
        super(configuration, mockServerLogger);
        this.httpServletResponse = httpServletResponse;
        this.mockServerResponseToHttpServletResponseEncoder = new MockServerHttpResponseToHttpServletResponseEncoder(mockServerLogger);
    }

    @Override
    public void sendResponse(HttpRequest request, HttpResponse response) {
        mockServerResponseToHttpServletResponseEncoder.mapMockServerResponseToHttpServletResponse(response, httpServletResponse);
    }

}
