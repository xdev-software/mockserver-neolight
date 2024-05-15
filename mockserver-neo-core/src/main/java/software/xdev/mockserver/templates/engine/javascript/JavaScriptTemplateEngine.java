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
package software.xdev.mockserver.templates.engine.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import org.openjdk.nashorn.api.scripting.ClassFilter;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.serialization.model.DTO;
import software.xdev.mockserver.templates.engine.TemplateEngine;
import software.xdev.mockserver.templates.engine.TemplateFunctions;
import software.xdev.mockserver.templates.engine.javascript.bindings.ScriptBindings;
import software.xdev.mockserver.templates.engine.model.HttpRequestTemplateObject;
import software.xdev.mockserver.templates.engine.serializer.HttpTemplateOutputDeserializer;
import org.slf4j.event.Level;

import javax.script.*;
import java.util.stream.StreamSupport;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.xdev.mockserver.configuration.Configuration.configuration;
import static software.xdev.mockserver.formatting.StringFormatter.formatLogMessage;
import static software.xdev.mockserver.formatting.StringFormatter.indentAndToString;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.TEMPLATE_GENERATED;
import static software.xdev.mockserver.log.model.LogEntryMessages.TEMPLATE_GENERATED_MESSAGE_FORMAT;

@SuppressWarnings({"RedundantSuppression", "deprecation", "removal", "FieldMayBeFinal"})
public class JavaScriptTemplateEngine implements TemplateEngine {

    private ScriptEngine engine;
    private ObjectMapper objectMapper;
    private final MockServerLogger mockServerLogger;
    private HttpTemplateOutputDeserializer httpTemplateOutputDeserializer;
    private final Configuration configuration;

    public JavaScriptTemplateEngine(MockServerLogger mockServerLogger, Configuration configuration) {
        System.setProperty("nashorn.args", "--language=es6");
        this.configuration = (configuration == null) ? configuration() : configuration;
        this.engine = new NashornScriptEngineFactory().getScriptEngine(new DisallowClassesInTemplates(configuration));
        this.mockServerLogger = mockServerLogger;
        this.httpTemplateOutputDeserializer = new HttpTemplateOutputDeserializer(mockServerLogger);
        this.objectMapper = ObjectMapperFactory.createObjectMapper();
    }

    @Override
    public <T> T executeTemplate(String template, HttpRequest request, Class<? extends DTO<T>> dtoClass) {
        T result = null;
        String script = wrapTemplate(template);
        try {
            validateTemplate(template);
            if (engine != null) {
                Compilable compilable = (Compilable) engine;
                // HttpResponse handle(HttpRequest httpRequest) - ES6
                CompiledScript compiledScript = compilable.compile(script + " function serialise(request) { return JSON.stringify(handle(JSON.parse(request)), null, 2); }");

                Bindings serialiseBindings = engine.createBindings();
                engine.setBindings(new ScriptBindings(TemplateFunctions.BUILT_IN_FUNCTIONS), ScriptContext.ENGINE_SCOPE);
                compiledScript.eval(serialiseBindings);

                ScriptObjectMirror scriptObjectMirror = (ScriptObjectMirror) serialiseBindings.get("serialise");
                Object stringifiedResponse = scriptObjectMirror.call(null, new HttpRequestTemplateObject(request));

                JsonNode generatedObject = null;
                try {
                    generatedObject = objectMapper.readTree(String.valueOf(stringifiedResponse));
                } catch (Throwable throwable) {
                    if (MockServerLogger.isEnabled(Level.INFO)) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(Level.INFO)
                                .setHttpRequest(request)
                                .setMessageFormat("exception deserialising generated content:{}into json node for request:{}")
                                .setArguments(stringifiedResponse, request)
                        );
                    }
                }
                if (MockServerLogger.isEnabled(Level.INFO)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(TEMPLATE_GENERATED)
                            .setLogLevel(Level.INFO)
                            .setHttpRequest(request)
                            .setMessageFormat(TEMPLATE_GENERATED_MESSAGE_FORMAT)
                            .setArguments(generatedObject != null ? generatedObject : stringifiedResponse, script, request)
                    );
                }
                result = httpTemplateOutputDeserializer.deserializer(request, (String) stringifiedResponse, dtoClass);
            } else {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setHttpRequest(request)
                        .setMessageFormat(
                            "JavaScript based templating is only available in a JVM with the \"nashorn\" JavaScript engine, " +
                                "please use a JVM with the \"nashorn\" JavaScript engine, such as Oracle Java 8+"
                        )
                        .setArguments(new RuntimeException("\"nashorn\" JavaScript engine not available"))
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(formatLogMessage("Exception:{}transforming template:{}for request:{}", isNotBlank(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName(), template, request), e);
        }
        return result;
    }

    static String wrapTemplate(String template) {
        return "function handle(request) {" + indentAndToString(template)[0] + "}";
    }

    private void validateTemplate(String template) {
        if (isNotBlank(template) && isNotBlank(configuration.javascriptDisallowedText())) {
            Iterable<String> deniedStrings = Splitter.on(",").trimResults().split(configuration.javascriptDisallowedText());
            for (String deniedString : deniedStrings) {
                if (template.contains(deniedString)) {
                    throw new UnsupportedOperationException("Found disallowed string \"" + deniedString + "\" in template: " + template);
                }
            }
        }
    }

    private static class DisallowClassesInTemplates implements ClassFilter {
        private Iterable<String> restrictedClassesList = null;
        private final Configuration configuration;

        private DisallowClassesInTemplates(Configuration configuration) {
            this.configuration = configuration;
            init();
        }

        void init() {
            restrictedClassesList = Splitter.on(",").trimResults().split(configuration.javascriptDisallowedClasses());
        }

        /**
         * Specifies whether the Java class of the specified name be exposed to javascript
         *
         * @param className is the fully qualified name of the java class being checked.
         *                  This will not be null. Only non-array class names will be passed.
         * @return true if the java class can be exposed to javascript, false otherwise
         */
        @Override
        public boolean exposeToScripts(String className) {
            if (restrictedClassesList != null) {
                return StreamSupport
                    .stream(restrictedClassesList.spliterator(), false)
                    .noneMatch(restrictedClass -> restrictedClass.equalsIgnoreCase(className));
            } else {
                return true;
            }
        }
    }

}
