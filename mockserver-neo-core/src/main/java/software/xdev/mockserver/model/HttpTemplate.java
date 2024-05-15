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
package software.xdev.mockserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class HttpTemplate extends Action<HttpTemplate> {
    private int hashCode;
    private final TemplateType templateType;
    private String template;
    private Type actionType;

    public HttpTemplate(TemplateType type) {
        this.templateType = type;
    }

    /**
     * Static builder to create an template for responding or forwarding requests.
     */
    public static HttpTemplate template(TemplateType type) {
        return new HttpTemplate(type);
    }

    /**
     * Static builder to create an template for responding or forwarding requests.
     *
     * @param template the template for the response or request
     */
    public static HttpTemplate template(TemplateType type, String template) {
        return new HttpTemplate(type).withTemplate(template);
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public HttpTemplate withTemplate(String template) {
        this.template = template;
        this.hashCode = 0;
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public void withActionType(Type actionType) {
        this.actionType = actionType;
        this.hashCode = 0;
    }

    @Override
    @JsonIgnore
    public Type getType() {
        return actionType;
    }

    public enum TemplateType {
        JAVASCRIPT,
        VELOCITY,
        MUSTACHE
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (hashCode() != o.hashCode()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        HttpTemplate that = (HttpTemplate) o;
        return templateType == that.templateType &&
            Objects.equals(template, that.template) &&
            actionType == that.actionType;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(super.hashCode(), templateType, template, actionType);
        }
        return hashCode;
    }
}

