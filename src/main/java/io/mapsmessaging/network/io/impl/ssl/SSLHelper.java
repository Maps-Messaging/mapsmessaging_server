/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.network.io.impl.ssl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLHelper {

  private static final SSLHelper instance = new SSLHelper();

  public static SSLHelper getInstance(){
    return instance;
  }

  private SSLHelper(){}

  
  public SSLContext createContext(ConfigurationProperties configurationProperties, Logger logger) throws IOException{
    SSLContext sslContext;
    // We have a physical socket bound, so now build up the SSL Context for this interface
    //
    try {
      // <editor-fold desc="Load and initialize the Key Store">
      //
      // Physically load the key stores from file
      //
      KeyStore keyStore =
          loadKeyStore(
              configurationProperties.getProperty("ssl_keyStore"),
              configurationProperties.getProperty("ssl_keyStoreFile"),
              configurationProperties.getProperty("ssl_keyStorePassphrase").toCharArray(),
              logger);

      //
      // Initialise the Key Manager Factory so we can use it in the SSL Engine
      //
      String sslKeyManagerFactory = configurationProperties.getProperty("ssl_keyManagerFactory");
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(sslKeyManagerFactory);
      keyManagerFactory.init(keyStore, configurationProperties.getProperty("ssl_keyStorePassphrase").toCharArray());
      logger.log(ServerLogMessages.SSL_SERVER_INITIALISE, sslKeyManagerFactory);
      // </editor-fold>

      // <editor-fold desc="Load and initialise the Trust Store">
      //
      // Load and initialise the trust store
      //
      KeyStore trustStore =
          loadKeyStore(
              configurationProperties.getProperty("ssl_trustStore"),
              configurationProperties.getProperty("ssl_trustStoreFile"),
              configurationProperties.getProperty("ssl_trustStorePassphrase").toCharArray(),
              logger);

      //
      // Initialise the Trust Manager Factory from the trust store so we can validate it in the SSL
      // Context
      //
      String trustStoreManagerFactory = configurationProperties.getProperty("ssl_trustManagerFactory");
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustStoreManagerFactory);
      trustManagerFactory.init(trustStore);
      logger.log(ServerLogMessages.SSL_SERVER_TRUST_MANAGER, trustStoreManagerFactory);
      // </editor-fold>

      // <editor-fold desc="Create the SSL Context">
      //
      // Put it all together and create the SSL Context to generate SSL Engines
      logger.log(ServerLogMessages.SSL_SERVER_CONTEXT_CONSTRUCT);
      sslContext = SSLContext.getInstance(configurationProperties.getProperty("ssl_SSLContext"));
      sslContext.init(
          keyManagerFactory.getKeyManagers(),
          trustManagerFactory.getTrustManagers(),
          new SecureRandom());
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

  private KeyStore loadKeyStore(String type, String fileName, char[] passphrase, Logger logger)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    logger.log(ServerLogMessages.SSL_SERVER_LOAD_KEY_STORE, fileName, type);
    KeyStore keyStore = KeyStore.getInstance(type);
    keyStore.load(new FileInputStream(fileName), passphrase);
    logger.log(ServerLogMessages.SSL_SERVER_LOADED_KEY_STORE, fileName, type);
    return keyStore;
  }
}
