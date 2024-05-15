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

import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.XPathBody;

import java.util.Map;

public class XPathBodyDTO extends BodyDTO {

    private final String xpath;
    private final Map<String, String> namespacePrefixes;

    public XPathBodyDTO(XPathBody xPathBody) {
        this(xPathBody, null);
    }

    public XPathBodyDTO(XPathBody xPathBody, Boolean not) {
        super(Body.Type.XPATH, not);
        this.xpath = xPathBody.getValue();
        this.namespacePrefixes = xPathBody.getNamespacePrefixes();
        withOptional(xPathBody.getOptional());
    }

    public String getXPath() {
      return xpath;
    }

    public Map<String, String> getNamespacePrefixes() {
        return namespacePrefixes;
    }

    public XPathBody buildObject() {
        return (XPathBody) new XPathBody(getXPath(), getNamespacePrefixes()).withOptional(getOptional());
    }
}
