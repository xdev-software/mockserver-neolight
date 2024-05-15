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

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.uuid.UUIDService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import static software.xdev.mockserver.configuration.Configuration.configuration;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.SERVER_CONFIGURATION;
import static software.xdev.mockserver.socket.tls.KeyAndCertificateFactoryFactory.createKeyAndCertificateFactory;
import static org.slf4j.event.Level.*;

/**
 * @author jamesdbloom, ganskef
 */
public class KeyStoreFactory {

    public static final String KEY_STORE_TYPE = "jks";
    public static final String KEY_STORE_PASSWORD = "changeit";
    public static final String KEY_STORE_CERT_ALIAS = "mockserver-client-cert";
    public static final String KEY_STORE_CA_ALIAS = "mockserver-ca-cert";
    public final String keyStoreFileName = "mockserver_keystore_" + UUIDService.getUUID() + "_" + KEY_STORE_TYPE;
    /**
     * Enforce TLS 1.2 if available, since it's not default up to Java 8.
     * <p>
     * Java 7 disables TLS 1.1 and 1.2 for clients. From <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html">Java Cryptography Architecture Oracle Providers Documentation:</a>
     * Although SunJSSE in the Java SE 7 release supports TLS 1.1 and TLS 1.2,
     * neither version is enabled by default for client connections. Some
     * servers do not implement forward compatibility correctly and refuse to
     * talk to TLS 1.1 or TLS 1.2 clients. For interoperability, SunJSSE does
     * not enable TLS 1.1 or TLS 1.2 by default for client connections.
     */
    private static final String SSL_CONTEXT_PROTOCOL = "TLSv1.2";
    /**
     * {@link SSLContext}: Every implementation of the Java platform is required
     * to support the following standard SSLContext protocol: TLSv1
     */
    private static final String SSL_CONTEXT_FALLBACK_PROTOCOL = "TLSv1";

    private SSLContext sslContext;
    private final Configuration configuration;
    private final MockServerLogger mockServerLogger;
    private final KeyAndCertificateFactory keyAndCertificateFactory;

    /**
     * @deprecated use constructor that specifies configuration explicitly
     */
    @Deprecated
    public KeyStoreFactory(MockServerLogger mockServerLogger) {
        this.configuration = configuration();
        this.mockServerLogger = mockServerLogger;
        this.keyAndCertificateFactory = createKeyAndCertificateFactory(configuration, mockServerLogger);
    }

    public KeyStoreFactory(Configuration configuration, MockServerLogger mockServerLogger) {
        this.configuration = configuration;
        this.mockServerLogger = mockServerLogger;
        this.keyAndCertificateFactory = createKeyAndCertificateFactory(configuration, mockServerLogger);
    }

    public synchronized SSLContext sslContext() {
        if (keyAndCertificateFactory.certificateNotYetCreated()) {
            keyAndCertificateFactory.buildAndSavePrivateKeyAndX509Certificate();
        }
        return sslContext(
            keyAndCertificateFactory.privateKey(),
            keyAndCertificateFactory.x509Certificate(),
            keyAndCertificateFactory.certificateAuthorityX509Certificate(),
            new X509Certificate[]{keyAndCertificateFactory.certificateAuthorityX509Certificate()}
        );
    }

    public synchronized SSLContext sslContext(PrivateKey privateKey, X509Certificate x509Certificate, X509Certificate certificateAuthorityX509Certificate, X509Certificate[] trustX509CertificateChain) {
        if (sslContext == null || configuration.rebuildTLSContext()) {
            try {
                // key manager
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(loadOrCreateKeyStore(
                    privateKey,
                    x509Certificate,
                    certificateAuthorityX509Certificate,
                    trustX509CertificateChain
                ), KEY_STORE_PASSWORD.toCharArray());

                // ssl context
                sslContext = getSSLContextInstance();
                sslContext.init(keyManagerFactory.getKeyManagers(), InsecureTrustManagerFactory.INSTANCE.getTrustManagers(), null);
            } catch (Throwable throwable) {
                throw new RuntimeException("Failed to initialize the SSLContext", throwable);
            }
        }
        return sslContext;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    public KeyStore loadOrCreateKeyStore() {
        if (keyAndCertificateFactory.certificateNotYetCreated()) {
            keyAndCertificateFactory.buildAndSavePrivateKeyAndX509Certificate();
        }
        return loadOrCreateKeyStore(
            keyAndCertificateFactory.privateKey(),
            keyAndCertificateFactory.x509Certificate(),
            keyAndCertificateFactory.certificateAuthorityX509Certificate(),
            new X509Certificate[]{keyAndCertificateFactory.certificateAuthorityX509Certificate()}
        );
    }

    public KeyStore loadOrCreateKeyStore(PrivateKey privateKey, X509Certificate x509Certificate, X509Certificate certificateAuthorityX509Certificate, X509Certificate[] trustX509CertificateChain) {
        KeyStore keystore = null;
        File keyStoreFile = new File(keyStoreFileName);
        if (keyStoreFile.exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(keyStoreFile)) {
                keystore = KeyStore.getInstance(KEY_STORE_TYPE);
                keystore.load(fileInputStream, KEY_STORE_PASSWORD.toCharArray());
            } catch (Exception e) {
                throw new RuntimeException("Exception while loading KeyStore from " + keyStoreFile.getAbsolutePath(), e);
            }
        }
        System.setProperty("javax.net.ssl.trustStore", keyStoreFile.getAbsolutePath());

        return savePrivateKeyAndX509InKeyStore(
            keystore,
            privateKey,
            KEY_STORE_PASSWORD.toCharArray(),
            new X509Certificate[]{
                x509Certificate,
                certificateAuthorityX509Certificate
            },
            trustX509CertificateChain
        );
    }

    private SSLContext getSSLContextInstance() throws NoSuchAlgorithmException {
        try {
            if (MockServerLogger.isEnabled(DEBUG) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setType(SERVER_CONFIGURATION)
                        .setLogLevel(DEBUG)
                        .setMessageFormat("using protocol{}")
                        .setArguments(SSL_CONTEXT_PROTOCOL)
                );
            }
            return SSLContext.getInstance(SSL_CONTEXT_PROTOCOL);
        } catch (NoSuchAlgorithmException nsae) {
            if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(WARN)
                        .setMessageFormat("protocol{}not available, falling back to{}")
                        .setArguments(SSL_CONTEXT_PROTOCOL, SSL_CONTEXT_FALLBACK_PROTOCOL)
                        .setThrowable(nsae)
                );
            }
            return SSLContext.getInstance(SSL_CONTEXT_FALLBACK_PROTOCOL);
        }
    }

    private KeyStore savePrivateKeyAndX509InKeyStore(KeyStore existingKeyStore, Key privateKey, char[] keyStorePassword, Certificate[] chain, X509Certificate... caCerts) {
        try {
            KeyStore keyStore = existingKeyStore;
            if (keyStore == null) {
                // create new key store
                keyStore = KeyStore.getInstance("jks");
                keyStore.load(null, keyStorePassword);
            }

            // add certificate
            try {
                keyStore.deleteEntry(KeyStoreFactory.KEY_STORE_CERT_ALIAS);
            } catch (KeyStoreException kse) {
                // ignore as may not exist in keystore yet
            }
            keyStore.setKeyEntry(KeyStoreFactory.KEY_STORE_CERT_ALIAS, privateKey, keyStorePassword, chain);

            for (X509Certificate caCert : caCerts) {
                // add CA certificate
                try {
                    keyStore.deleteEntry(KEY_STORE_CA_ALIAS);
                } catch (KeyStoreException kse) {
                    // ignore as may not exist in keystore yet
                }
                keyStore.setCertificateEntry(KEY_STORE_CA_ALIAS, caCert);
            }

            // save as JKS file
            String keyStoreFileAbsolutePath = new File(keyStoreFileName).getAbsolutePath();
            try (FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFileAbsolutePath)) {
                keyStore.store(fileOutputStream, keyStorePassword);
                if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(TRACE)
                            .setMessageFormat("saving key store to file [" + keyStoreFileAbsolutePath + "]")
                    );
                }
            }
            new File(keyStoreFileAbsolutePath).deleteOnExit();
            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Exception while saving KeyStore", e);
        }
    }

}
