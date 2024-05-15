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
import software.xdev.mockserver.model.XmlSchemaBody;

public class XmlSchemaBodyDTO extends BodyDTO {

    private final String xmlSchema;

    public XmlSchemaBodyDTO(XmlSchemaBody xmlSchemaBody) {
        this(xmlSchemaBody, null);
    }

    public XmlSchemaBodyDTO(XmlSchemaBody xmlSchemaBody, Boolean not) {
        super(Body.Type.XML_SCHEMA, not);
        this.xmlSchema = xmlSchemaBody.getValue();
        withOptional(xmlSchemaBody.getOptional());
    }

    public String getXml() {
        return xmlSchema;
    }

    public XmlSchemaBody buildObject() {
        return (XmlSchemaBody) new XmlSchemaBody(getXml()).withOptional(getOptional());
    }
}
