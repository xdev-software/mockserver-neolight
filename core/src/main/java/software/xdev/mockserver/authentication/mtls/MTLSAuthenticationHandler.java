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
package software.xdev.mockserver.authentication.mtls;

import com.google.common.collect.ImmutableMap;
import software.xdev.mockserver.authentication.AuthenticationException;
import software.xdev.mockserver.authentication.AuthenticationHandler;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import org.slf4j.event.Level;

import java.security.cert.X509Certificate;

public class MTLSAuthenticationHandler implements AuthenticationHandler {

    private final MockServerLogger mockServerLogger;
    private final X509Certificate[] controlPlaneTLSMutualAuthenticationCAChain;

    public MTLSAuthenticationHandler(MockServerLogger mockServerLogger, X509Certificate[] controlPlaneTLSMutualAuthenticationCAChain) {
        this.mockServerLogger = mockServerLogger;
        this.controlPlaneTLSMutualAuthenticationCAChain = controlPlaneTLSMutualAuthenticationCAChain;
    }

    @Override
    public boolean controlPlaneRequestAuthenticated(HttpRequest request) {
        if (controlPlaneTLSMutualAuthenticationCAChain != null && controlPlaneTLSMutualAuthenticationCAChain.length != 0) {
            if (request.getClientCertificateChain() != null) {
                for (software.xdev.mockserver.model.X509Certificate clientCertificate : request.getClientCertificateChain()) {
                    for (X509Certificate caCertificate : controlPlaneTLSMutualAuthenticationCAChain) {
                        String clientCertificateInformation = getClientCertificateInformation(
                            clientCertificate.getSerialNumber(),
                            clientCertificate.getIssuerDistinguishedName(),
                            clientCertificate.getSubjectDistinguishedName()
                        );
                        String caCertificateInformation = getClientCertificateInformation(
                            caCertificate.getSerialNumber().toString(),
                            caCertificate.getIssuerX500Principal().getName(),
                            caCertificate.getSubjectX500Principal().getName()
                        );
                        try {
                            clientCertificate.getCertificate().verify(caCertificate.getPublicKey());
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(Level.DEBUG)
                                    .setHttpRequest(request)
                                    .setMessageFormat("validated client certificate:{}against control plane trust store certificate:{}")
                                    .setArguments(clientCertificateInformation, caCertificateInformation)
                            );
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(Level.DEBUG)
                                    .setHttpRequest(request)
                                    .setMessageFormat("control plane request passed authentication:{}")
                                    .setArguments(request)
                            );
                            return true;
                        } catch (Throwable throwable) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(Level.TRACE)
                                    .setHttpRequest(request)
                                    .setMessageFormat("exception validating client certificate:{}against control plane trust store certificate:{}")
                                    .setArguments(clientCertificateInformation, caCertificateInformation)
                                    .setThrowable(throwable)
                            );
                        }
                    }
                }
                throw new AuthenticationException("control plane request failed authentication no client certificates can be validated by control plane CA");
            } else {
                throw new AuthenticationException("control plane request failed authentication no client certificates found");
            }
        }
        throw new AuthenticationException("control plane request failed authentication no control plane CA specified");
    }

    private String getClientCertificateInformation(String serialNumber, String issuerDistinguishedName, String subjectDistinguishedName) {
        try {
            return ObjectMapperFactory.createObjectMapper(true, false).writeValueAsString(ImmutableMap.of(
                "serialNumber", serialNumber,
                "issuerDistinguishedName", issuerDistinguishedName,
                "subjectDistinguishedName", subjectDistinguishedName
            ));
        } catch (Throwable throwable) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.TRACE)
                    .setMessageFormat("exception serialising certificate information")
                    .setThrowable(throwable)
            );
            return "";
        }
    }

}
