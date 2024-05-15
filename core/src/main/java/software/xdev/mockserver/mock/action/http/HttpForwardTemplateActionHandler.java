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

import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.serialization.model.HttpRequestDTO;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpTemplate;
import software.xdev.mockserver.templates.engine.TemplateEngine;
import software.xdev.mockserver.templates.engine.javascript.JavaScriptTemplateEngine;
import software.xdev.mockserver.templates.engine.mustache.MustacheTemplateEngine;
import software.xdev.mockserver.templates.engine.velocity.VelocityTemplateEngine;

public class HttpForwardTemplateActionHandler extends HttpForwardAction {

    private final Configuration configuration;
    private VelocityTemplateEngine velocityTemplateEngine;
    private JavaScriptTemplateEngine javascriptTemplateEngine;
    private MustacheTemplateEngine mustacheTemplateEngine;

    public HttpForwardTemplateActionHandler(MockServerLogger mockServerLogger, Configuration configuration, NettyHttpClient httpClient) {
        super(mockServerLogger, httpClient);
        this.configuration = configuration;
    }

    public HttpForwardActionResult handle(HttpTemplate httpTemplate, HttpRequest originalRequest) {
        TemplateEngine templateEngine;
        switch (httpTemplate.getTemplateType()) {
            case VELOCITY:
                templateEngine = getVelocityTemplateEngine();
                break;
            case JAVASCRIPT:
                templateEngine = getJavaScriptTemplateEngine();
                break;
            case MUSTACHE:
                templateEngine = getMustacheTemplateEngine();
                break;
            default:
                throw new RuntimeException("Unknown no template engine available for " + httpTemplate.getTemplateType());
        }
        if (templateEngine != null) {
            HttpRequest templatedRequest = templateEngine.executeTemplate(httpTemplate.getTemplate(), originalRequest, HttpRequestDTO.class);
            if (templatedRequest != null) {
                return sendRequest(templatedRequest, null, null);
            }
        }

        return notFoundFuture(originalRequest);
    }

    private VelocityTemplateEngine getVelocityTemplateEngine() {
        if (velocityTemplateEngine == null) {
            velocityTemplateEngine = new VelocityTemplateEngine(mockServerLogger, configuration);
        }
        return velocityTemplateEngine;
    }

    private JavaScriptTemplateEngine getJavaScriptTemplateEngine() {
        if (javascriptTemplateEngine == null) {
            javascriptTemplateEngine = new JavaScriptTemplateEngine(mockServerLogger, configuration);
        }
        return javascriptTemplateEngine;
    }

    private MustacheTemplateEngine getMustacheTemplateEngine() {
        if (mustacheTemplateEngine == null) {
            mustacheTemplateEngine = new MustacheTemplateEngine(mockServerLogger, configuration);
        }
        return mustacheTemplateEngine;
    }
}
