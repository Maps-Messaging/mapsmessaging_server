/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.ssl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.security.certificates.CertificateManager;
import io.mapsmessaging.security.certificates.CertificateManagerFactory;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import lombok.Getter;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class SSLHelper {

  @Getter
  private static final SSLHelper instance = new SSLHelper();

  private SSLHelper() {
  }


  public SSLContext createContext(String context, ConfigurationProperties config, Logger logger) throws IOException {
    SSLContext sslContext;
    // We have a physical socket bound, so now build up the SSL Context for this interface
    //
    ConfigurationProperties keyStoreProps = (ConfigurationProperties) config.get("keyStore");
    ConfigurationProperties trustStoreProps = (ConfigurationProperties) config.get("trustStore");

    String alias = keyStoreProps.getProperty("alias");
    try {
      // <editor-fold desc="Load and initialize the Key Store">
      //
      // Physically load the key stores from file
      //
      KeyStore keyStore = loadKeyStore(keyStoreProps);
      //
      // Initialise the Key Manager Factory, so we can use it in the SSL Engine
      //
      String sslKeyManagerFactory = keyStoreProps.getProperty("managerFactory");
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(sslKeyManagerFactory);
      keyManagerFactory.init(keyStore, keyStoreProps.getProperty("passphrase").toCharArray());
      logger.log(ServerLogMessages.SSL_SERVER_INITIALISE, sslKeyManagerFactory);
      KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
      if (alias != null && !alias.isEmpty()) {
        for (int i = 0; i < keyManagers.length; i++) {
          if (keyManagers[i] instanceof X509ExtendedKeyManager) {
            keyManagers[i] = new CustomKeyManager((X509ExtendedKeyManager) keyManagers[i], alias);
          }
        }
      }

      // </editor-fold>

      // <editor-fold desc="Load and initialise the Trust Store">
      //
      // Load and initialise the trust store
      //
      KeyStore trustStore = loadKeyStore(trustStoreProps);

      //
      // Initialise the Trust Manager Factory from the trust store so we can validate it in the SSL
      // Context
      //
      String trustStoreManagerFactory = trustStoreProps.getProperty("managerFactory");
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustStoreManagerFactory);
      trustManagerFactory.init(trustStore);
      logger.log(ServerLogMessages.SSL_SERVER_TRUST_MANAGER, trustStoreManagerFactory);
      // </editor-fold>

      // <editor-fold desc="Create the SSL Context">
      //
      // Put it all together and create the SSL Context to generate SSL Engines
      logger.log(ServerLogMessages.SSL_SERVER_CONTEXT_CONSTRUCT);
      sslContext = SSLContext.getInstance(context);
      sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(), new SecureRandom());
      logger.log(ServerLogMessages.SSL_SERVER_SSL_CONTEXT_COMPLETE);
      // </editor-fold>

    } catch (KeyStoreException
             | IOException
             | NoSuchAlgorithmException
             | CertificateException
             | UnrecoverableKeyException
             | KeyManagementException e) {
      IOException exception = new IOException(e.getMessage());
      exception.initCause(e);
      throw exception;
    }
    return sslContext;
  }

  private KeyStore loadKeyStore(ConfigurationProperties properties)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    CertificateManager mananger = CertificateManagerFactory.getInstance().getManager(properties.getMap());
    return mananger.getKeyStore();
  }
}
