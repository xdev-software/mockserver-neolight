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
package software.xdev.mockserver.authentication.jwt;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import software.xdev.mockserver.keys.AsymmetricKeyConverter;
import software.xdev.mockserver.keys.AsymmetricKeyPair;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.util.Collections;
import java.util.Map;

public class JWKGenerator {

    private final ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);

    public String generateJWK(AsymmetricKeyPair asymmetricKeyPair) {
        try {
            Map<String, Object> singleKey;
            switch (asymmetricKeyPair.getAlgorithm()) {
                case EC256_SHA256:
                    singleKey = getEllipticCurveJWK(asymmetricKeyPair, Curve.P_256);
                    break;
                case EC384_SHA384:
                    singleKey = getEllipticCurveJWK(asymmetricKeyPair, Curve.P_384);
                    break;
                case ECP512_SHA512:
                    singleKey = getEllipticCurveJWK(asymmetricKeyPair, Curve.P_521);
                    break;
                case RSA2048_SHA256:
                case RSA3072_SHA384:
                case RSA4096_SHA512:
                    singleKey = getRSAJWK(asymmetricKeyPair);
                    break;
                default:
                    throw new IllegalArgumentException("Error invalid algorithm has been provided");
            }
            return objectWriter.writeValueAsString(
                ImmutableMap.of(
                    "keys", Collections.singletonList(singleKey)
                )
            );
        } catch (Throwable throwable) {
            throw new RuntimeException("Exception creating JWK", throwable);
        }
    }

    private Map<String, Object> getRSAJWK(AsymmetricKeyPair asymmetricKeyPair) {
        return new RSAKey
            .Builder(AsymmetricKeyConverter.getRSAPublicKey(asymmetricKeyPair.getKeyPair().getPublic().getEncoded()))
            .keyID(asymmetricKeyPair.getKeyId())
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(new Algorithm(asymmetricKeyPair.getAlgorithm().getJwtAlgorithm()))
            .build()
            .toJSONObject();
    }

    private Map<String, Object> getEllipticCurveJWK(AsymmetricKeyPair asymmetricKeyPair, Curve curve) {
        return new ECKey
            .Builder(curve, AsymmetricKeyConverter.getECPublicKey(asymmetricKeyPair.getKeyPair().getPublic().getEncoded()))
            .keyID(asymmetricKeyPair.getKeyId())
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(new Algorithm(asymmetricKeyPair.getAlgorithm().getJwtAlgorithm()))
            .build()
            .toJSONObject();
    }
}
