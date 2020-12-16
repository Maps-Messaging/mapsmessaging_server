/*
 * Copyright [2020] [Matthew Buckton]
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

package org.maps.network.io.impl.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.admin.EndPointManagerJMX;
import org.maps.network.io.AcceptHandler;
import org.maps.network.io.Selectable;
import org.maps.network.io.impl.Selector;
import org.maps.network.io.impl.SelectorLoadManager;
import org.maps.network.io.impl.tcp.TCPEndPointServer;

public class SSLEndPointServer extends TCPEndPointServer {

  private final SSLContext sslContext;
  private final boolean requiresClientAuth;

  public SSLEndPointServer(
      InetSocketAddress bindAddr,
      SelectorLoadManager sel,
      AcceptHandler accept,
      NetworkConfig config,
      EndPointURL url,
      EndPointManagerJMX managerMBean)
      throws IOException {
    super(bindAddr, sel, accept, config, url, managerMBean);
    logger.log(LogMessages.SSL_SERVER_START);
    requiresClientAuth = Boolean.parseBoolean(config.getProperties().getProperty("ssl_clientCertificateRequired", "false"));

    // We have a physical socket bound, so now build up the SSL Context for this interface
    //
    try {
      // <editor-fold desc="Load and initialize the Key Store">
      //
      // Physically load the key stores from file
      //
      KeyStore keyStore =
          loadKeyStore(
              config.getProperties().getProperty("ssl_keyStore"),
              config.getProperties().getProperty("ssl_keyStoreFile"),
              config.getProperties().getProperty("ssl_keyStorePassphrase").toCharArray());

      //
      // Initialise the Key Manager Factory so we can use it in the SSL Engine
      //
      String sslKeyManagerFactory = config.getProperties().getProperty("ssl_keyManagerFactory");
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(sslKeyManagerFactory);
      keyManagerFactory.init(keyStore, config.getProperties().getProperty("ssl_keyStorePassphrase").toCharArray());
      logger.log(LogMessages.SSL_SERVER_INITIALISE, sslKeyManagerFactory);
      // </editor-fold>

      // <editor-fold desc="Load and initialise the Trust Store">
      //
      // Load and initialise the trust store
      //
      KeyStore trustStore =
          loadKeyStore(
              config.getProperties().getProperty("ssl_trustStore"),
              config.getProperties().getProperty("ssl_trustStoreFile"),
              config.getProperties().getProperty("ssl_trustStorePassphrase").toCharArray());

      //
      // Initialise the Trust Manager Factory from the trust store so we can validate it in the SSL
      // Context
      //
      String trustStoreManagerFactory = config.getProperties().getProperty("ssl_trustManagerFactory");
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustStoreManagerFactory);
      trustManagerFactory.init(trustStore);
      logger.log(LogMessages.SSL_SERVER_TRUST_MANAGER, trustStoreManagerFactory);
      // </editor-fold>

      // <editor-fold desc="Create the SSL Context">
      //
      // Put it all together and create the SSL Context to generate SSL Engines
      logger.log(LogMessages.SSL_SERVER_CONTEXT_CONSTRUCT);
      sslContext = SSLContext.getInstance(config.getProperties().getProperty("ssl_SSLContext"));
      sslContext.init(
          keyManagerFactory.getKeyManagers(),
          trustManagerFactory.getTrustManagers(),
          new SecureRandom());
      logger.log(LogMessages.SSL_SERVER_SSL_CONTEXT_COMPLETE);
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
    } finally {
      logger.log(LogMessages.SSL_SERVER_COMPLETED);
    }
  }

  @Override
  public void selected(Selectable selectable, Selector sel, int selection) {
    try {
      SSLEngine sslEngine = sslContext.createSSLEngine();
      sslEngine.setNeedClientAuth(requiresClientAuth);
      sslEngine.setUseClientMode(false);

      SSLEndPoint sslEndPoint =
          new SSLEndPoint(
              generateID(),
              sslEngine,
              serverSocket.accept().socket(),
              selector,
              authenticationConfig,
              this,
              managerMBean);
      handleNewEndPoint(sslEndPoint);
    } catch (IOException e) {
      logger.log(LogMessages.SSL_SERVER_ACCEPT_FAILED);
    }
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(SSLEndPointServer.class.getName() + "_" + url);
  }

  private KeyStore loadKeyStore(String type, String fileName, char[] passphrase)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    logger.log(LogMessages.SSL_SERVER_LOAD_KEY_STORE, fileName, type);
    KeyStore keyStore = KeyStore.getInstance(type);
    keyStore.load(new FileInputStream(fileName), passphrase);
    logger.log(LogMessages.SSL_SERVER_LOADED_KEY_STORE, fileName, type);
    return keyStore;
  }
}
