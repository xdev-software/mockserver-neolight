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

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.Callable;

public class AsymmetricKeyConverter {

    public static RSAPublicKey getRSAPublicKey(byte[] publicKey) {
        return convertKey(
            "RSA",
            "public",
            () -> (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKey))
        );
    }

    public static RSAPrivateKey getRSAPrivateKey(byte[] privateKey) {
        return convertKey(
            "RSA",
            "private",
            () -> (RSAPrivateKey) KeyFactory
                .getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey))
        );
    }

    public static ECPublicKey getECPublicKey(byte[] publicKey) {
        return convertKey(
            "EC",
            "public",
            () -> (ECPublicKey) KeyFactory
                .getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(publicKey))
        );
    }

    public static ECPrivateKey getECPrivateKey(byte[] privateKey) {
        return convertKey(
            "EC",
            "private",
            () -> (ECPrivateKey) KeyFactory
                .getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey))
        );
    }

    private static <T> T convertKey(String algorithm, String keyType, Callable<T> callable) {
        try {
            return callable.call();
        } catch (Throwable throwable) {
            throw new RuntimeException("Exception converting " + keyType + " key for algorithm \"" + algorithm + "\" " + throwable.getMessage(), throwable);
        }
    }

}
