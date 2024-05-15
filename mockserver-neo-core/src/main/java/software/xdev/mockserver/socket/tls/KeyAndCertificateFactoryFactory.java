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

import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.socket.tls.bouncycastle.BCKeyAndCertificateFactory;

import java.lang.reflect.Constructor;

public class KeyAndCertificateFactoryFactory {

    private static KeyAndCertificateFactorySupplier customKeyAndCertificateFactorySupplier = null;

    private static final ClassLoader CLASS_LOADER = KeyAndCertificateFactoryFactory.class.getClassLoader();

    public static KeyAndCertificateFactory createKeyAndCertificateFactory(Configuration configuration, MockServerLogger mockServerLogger) {
        return createKeyAndCertificateFactory(configuration, mockServerLogger, true);
    }

    public static KeyAndCertificateFactory createKeyAndCertificateFactory(Configuration configuration, MockServerLogger mockServerLogger, boolean forServer) {
        if (customKeyAndCertificateFactorySupplier != null) {
            return customKeyAndCertificateFactorySupplier
                .buildKeyAndCertificateFactory(mockServerLogger, forServer, configuration);
        } else {
            return new BCKeyAndCertificateFactory(configuration, mockServerLogger);
        }
    }

    public static KeyAndCertificateFactorySupplier getCustomKeyAndCertificateFactorySupplier() {
        return customKeyAndCertificateFactorySupplier;
    }

    public static void setCustomKeyAndCertificateFactorySupplier(
        KeyAndCertificateFactorySupplier customKeyAndCertificateFactorySupplier) {
        KeyAndCertificateFactoryFactory.customKeyAndCertificateFactorySupplier = customKeyAndCertificateFactorySupplier;
    }
}
