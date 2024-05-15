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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

public class AsymmetricKeyGenerator {

    public static AsymmetricKeyPair createAsymmetricKeyPair(AsymmetricKeyPairAlgorithm algorithm) {
        return new AsymmetricKeyPair(UUID.randomUUID().toString(), algorithm, createKeyPair(algorithm));
    }

    public static KeyPair createKeyPair(AsymmetricKeyPairAlgorithm algorithm) {
        try {
            KeyPairGenerator generator;
            switch (algorithm) {
                case RSA2048_SHA256:
                case RSA3072_SHA384:
                case RSA4096_SHA512:
                    generator = KeyPairGenerator.getInstance(algorithm.getAlgorithm());
                    generator.initialize(algorithm.getKeyLength());
                    break;
                case EC256_SHA256:
                case EC384_SHA384:
                case ECP512_SHA512:
                    generator = KeyPairGenerator.getInstance(algorithm.getAlgorithm());
                    generator.initialize(new ECGenParameterSpec(algorithm.getECDomainParameters()));
                    break;
                default:
                    throw new IllegalArgumentException(algorithm + " is not a valid key algorithm");
            }
            return generator.generateKeyPair();
        } catch (Throwable throwable) {
            throw new RuntimeException("Exception generating key for algorithm \"" + algorithm + "\" " + throwable.getMessage(), throwable);
        }
    }

}