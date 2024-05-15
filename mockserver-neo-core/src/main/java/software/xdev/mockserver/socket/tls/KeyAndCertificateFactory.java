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
package software.xdev.mockserver.socket.tls;

import software.xdev.mockserver.keys.AsymmetricKeyPairAlgorithm;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

public interface KeyAndCertificateFactory {

    /**
     * default key pair generation and signing algorithm
     */
    AsymmetricKeyPairAlgorithm DEFAULT_KEY_GENERATION_AND_SIGNING_ALGORITHM = AsymmetricKeyPairAlgorithm.RSA2048_SHA256;
    /**
     * Current time minus 1 year, just in case software clock goes back due to time synchronization
     */
    Date NOT_BEFORE = new Date(System.currentTimeMillis() - 86400000L * 5);
    /**
     * The maximum possible value in X.509 specification: 9999-12-31 23:59:59,
     * new Date(253402300799000L), but Apple iOS 8 fails with a certificate
     * expiration date grater than Mon, 24 Jan 6084 02:07:59 GMT (issue #6).
     * <p>
     * A hundred years in the future from starting the proxy should be enough.
     */
    Date NOT_AFTER = new Date(System.currentTimeMillis() + 86400000L * 365);
    /**
     * CN for CA distinguishing name
     */
    String ROOT_COMMON_NAME = "www.mockserver.com";
    /**
     * default CN for leaf distinguishing name
     */
    String CERTIFICATE_DOMAIN = "localhost";
    /**
     * O for distinguishing name
     */
    String ORGANISATION = "MockServer";
    /**
     * L for distinguishing name
     */
    String LOCALITY = "London";
    /**
     * ST for distinguishing name
     */
    String STATE = "England";
    /**
     * C for distinguishing name
     */
    String COUNTRY = "UK";

    @SuppressWarnings("unused")
    void buildAndSaveCertificateAuthorityPrivateKeyAndX509Certificate();

    void buildAndSavePrivateKeyAndX509Certificate();

    boolean certificateNotYetCreated();

    PrivateKey privateKey();

    X509Certificate x509Certificate();

    X509Certificate certificateAuthorityX509Certificate();

    List<X509Certificate> certificateChain();

}
