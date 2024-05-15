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
package software.xdev.mockserver.serialization.serializers.certificate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.X509Certificate;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class X509CertificateSerializer extends StdSerializer<X509Certificate> {

    public X509CertificateSerializer() {
        super(X509Certificate.class);
    }

    @Override
    public void serialize(X509Certificate x509Certificate, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (isNotBlank(x509Certificate.getSerialNumber())) {
            jgen.writeObjectField("serialNumber", x509Certificate.getSerialNumber());
        }
        if (isNotBlank(x509Certificate.getIssuerDistinguishedName())) {
            jgen.writeObjectField("issuerDistinguishedName", x509Certificate.getIssuerDistinguishedName());
        }
        if (isNotBlank(x509Certificate.getSubjectDistinguishedName())) {
            jgen.writeObjectField("subjectDistinguishedName", x509Certificate.getSubjectDistinguishedName());
        }
        if (isNotBlank(x509Certificate.getSignatureAlgorithmName())) {
            jgen.writeObjectField("signatureAlgorithmName", x509Certificate.getSignatureAlgorithmName());
        }
        jgen.writeEndObject();
    }
}
