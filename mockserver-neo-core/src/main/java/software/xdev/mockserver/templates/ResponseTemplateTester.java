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
package software.xdev.mockserver.templates;

import org.apache.commons.lang3.NotImplementedException;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.serialization.model.HttpResponseDTO;
import software.xdev.mockserver.templates.engine.javascript.JavaScriptTemplateEngine;
import software.xdev.mockserver.templates.engine.mustache.MustacheTemplateEngine;
import software.xdev.mockserver.templates.engine.velocity.VelocityTemplateEngine;

import javax.script.ScriptEngineManager;

public class ResponseTemplateTester {

    private static final MockServerLogger MOCK_SERVER_LOGGER = new MockServerLogger(ResponseTemplateTester.class);

    public static HttpResponse testMustacheTemplate(String template, HttpRequest request) {
        return new MustacheTemplateEngine(MOCK_SERVER_LOGGER, new Configuration()).executeTemplate(template, request, HttpResponseDTO.class);
    }

    public static HttpResponse testVelocityTemplate(String template, HttpRequest request) {
        return new VelocityTemplateEngine(MOCK_SERVER_LOGGER, new Configuration()).executeTemplate(template, request, HttpResponseDTO.class);
    }

    public static HttpResponse testJavaScriptTemplate(String template, HttpRequest request) {
        if (new ScriptEngineManager().getEngineByName("nashorn") != null) {
            return new JavaScriptTemplateEngine(MOCK_SERVER_LOGGER, new Configuration()).executeTemplate(template, request, HttpResponseDTO.class);
        } else {
            throw new NotImplementedException("Nashorn is not available on this JVM so JavaScript templates are not supported");
        }
    }

}
