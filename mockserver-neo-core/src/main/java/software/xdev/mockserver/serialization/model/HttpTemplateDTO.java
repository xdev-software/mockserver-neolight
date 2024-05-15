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
package software.xdev.mockserver.serialization.model;

import software.xdev.mockserver.model.HttpTemplate;
import software.xdev.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;

public class HttpTemplateDTO extends ObjectWithReflectiveEqualsHashCodeToString implements DTO<HttpTemplate> {

    private String template;
    private HttpTemplate.TemplateType templateType;
    private DelayDTO delay;

    public HttpTemplateDTO(HttpTemplate httpTemplate) {
        if (httpTemplate != null) {
            templateType = httpTemplate.getTemplateType();
            template = httpTemplate.getTemplate();
            delay = (httpTemplate.getDelay() != null ? new DelayDTO(httpTemplate.getDelay()) : null);
        }
    }

    public HttpTemplateDTO() {
    }

    public HttpTemplate buildObject() {
        return new HttpTemplate(templateType)
            .withTemplate(template)
            .withDelay((delay != null ? delay.buildObject() : null));
    }

    public HttpTemplate.TemplateType getTemplateType() {
        return templateType;
    }

    public HttpTemplateDTO setTemplateType(HttpTemplate.TemplateType templateType) {
        this.templateType = templateType;
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public HttpTemplateDTO setTemplate(String template) {
        this.template = template;
        return this;
    }

    public DelayDTO getDelay() {
        return delay;
    }

    public HttpTemplateDTO setDelay(DelayDTO delay) {
        this.delay = delay;
        return this;
    }
}

