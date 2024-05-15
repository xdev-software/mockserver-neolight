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
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import software.xdev.mockserver.keys.AsymmetricKeyConverter;
import software.xdev.mockserver.keys.AsymmetricKeyPair;
import software.xdev.mockserver.serialization.ObjectMapperFactory;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class JWTGenerator {

    private final ObjectWriter objectWriter = ObjectMapperFactory.createObjectMapper(true, false);

    private final JWKGenerator jwkGenerator;
    private final AsymmetricKeyPair asymmetricKeyPair;

    public JWTGenerator(AsymmetricKeyPair asymmetricKeyPair) {
        this.asymmetricKeyPair = asymmetricKeyPair;
        this.jwkGenerator = new JWKGenerator();
    }

    public String generateJWT() {
        try {
            this.jwkGenerator.generateJWK(asymmetricKeyPair);
            Instant now = Instant.now();
            JWSObject jwt = new JWSObject(
                new JWSHeader
                    .Builder(JWSAlgorithm.RS256)
                    .keyID(asymmetricKeyPair.getKeyId())
                    .type(JOSEObjectType.JWT)
                    .build(),
                new Payload(ImmutableMap.of(
                    "sub", UUID.randomUUID().toString(),
                    "aud", "https://www.mock-server.com",
                    "iss", "https://www.mock-server.com",
                    "nbf", now.minus(4, ChronoUnit.HOURS).getEpochSecond(),
                    "exp", now.plus(4, ChronoUnit.HOURS).getEpochSecond(),
                    "iat", now.getEpochSecond()
                ))
            );
            RSASSASigner signer = new RSASSASigner(asymmetricKeyPair.getKeyPair().getPrivate());
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error while generating JWT", e);
        }
    }

    public String signJWT(final Map<String, Serializable> claims) {
        try {
            final JWSSigner signer;
            byte[] privateKeyBytes = asymmetricKeyPair.getKeyPair().getPrivate().getEncoded();
            switch (asymmetricKeyPair.getAlgorithm()) {
                case EC256_SHA256:
                    signer = new ECDSASigner(
                        AsymmetricKeyConverter.getECPrivateKey(privateKeyBytes),
                        Curve.P_256
                    );
                    break;
                case EC384_SHA384:
                    signer = new ECDSASigner(
                        AsymmetricKeyConverter.getECPrivateKey(privateKeyBytes),
                        Curve.P_384
                    );
                    break;
                case ECP512_SHA512:
                    signer = new ECDSASigner(
                        AsymmetricKeyConverter.getECPrivateKey(privateKeyBytes),
                        Curve.P_521
                    );
                    break;
                case RSA2048_SHA256:
                case RSA3072_SHA384:
                case RSA4096_SHA512:
                    signer = new RSASSASigner(AsymmetricKeyConverter.getRSAPrivateKey(privateKeyBytes));
                    break;
                default:
                    throw new IllegalArgumentException("Error invalid algorithm has been provided");
            }

            JWSAlgorithm signingAlgorithm = JWSAlgorithm.parse(asymmetricKeyPair.getAlgorithm().getJwtAlgorithm());
            JWSObject jwt = new JWSObject(
                new JWSHeader
                    .Builder(signingAlgorithm)
                    .keyID(asymmetricKeyPair.getKeyId())
                    .build(),
                new Payload(objectWriter.writeValueAsString(claims))
            );

            jwt.sign(signer);
            return jwt.serialize();
        } catch (Throwable throwable) {
            throw new RuntimeException("Exception signing JWT", throwable);
        }
    }

}
