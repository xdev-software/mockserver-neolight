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
package software.xdev.mockserver.keys;

public enum AsymmetricKeyPairAlgorithm {

    EC256_SHA256("EC", "ES256", "SHA256WITHECDSA", "secp256r1"),
    EC384_SHA384("EC", "ES384", "SHA384WITHECDSA", "secp384r1"),
    ECP512_SHA512("EC", "ES512", "SHA512WITHECDSA", "secp521r1"),
    RSA2048_SHA256("RSA", "RS256", "SHA256WithRSA", 2048),
    RSA3072_SHA384("RSA", "RS384", "SHA384WITHRSA", 3072),
    RSA4096_SHA512("RSA", "RS512", "SHA512WITHRSA", 4096);

    private final String algorithm;
    private String ecDomainParameters;
    private int keyLength;
    private final String jwtAlgorithm;
    private final String signingAlgorithm;

    AsymmetricKeyPairAlgorithm(String algorithm, String jwtAlgorithm, String signingAlgorithm, String ecDomainParameters) {
        this(algorithm, jwtAlgorithm, signingAlgorithm);
        this.ecDomainParameters = ecDomainParameters;
    }

    AsymmetricKeyPairAlgorithm(String algorithm, String jwtAlgorithm, String signingAlgorithm, int keyLength) {
        this(algorithm, jwtAlgorithm, signingAlgorithm);
        this.keyLength = keyLength;
    }

    AsymmetricKeyPairAlgorithm(String algorithm, String jwtAlgorithm, String signingAlgorithm) {
        this.algorithm = algorithm;
        this.jwtAlgorithm = jwtAlgorithm;
        this.signingAlgorithm = signingAlgorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getJwtAlgorithm() {
        return jwtAlgorithm;
    }

    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public String getECDomainParameters() {
        return ecDomainParameters;
    }

    public int getKeyLength() {
        return keyLength;
    }
}
