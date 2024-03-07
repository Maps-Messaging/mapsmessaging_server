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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.tcp.TCPEndPointServer;
import io.mapsmessaging.security.ssl.SslHelper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;

public class SSLEndPointServer extends TCPEndPointServer {

  private final SSLContext sslContext;
  private final ConfigurationProperties tls;

  public SSLEndPointServer(
      InetSocketAddress bindAddr,
      SelectorLoadManager sel,
      AcceptHandler accept,
      NetworkConfig config,
      EndPointURL url,
      EndPointManagerJMX managerMBean)
      throws IOException {
    super(bindAddr, sel, accept, config, url, managerMBean);
    logger.log(ServerLogMessages.SSL_SERVER_START);
    ConfigurationProperties securityProps = (ConfigurationProperties) config.getProperties().get("security");
    tls = (ConfigurationProperties) securityProps.get("tls");

    try {
      sslContext = SslHelper.createContext("tls", tls, logger);
    } finally {
      logger.log(ServerLogMessages.SSL_SERVER_COMPLETED);
    }
  }

  @Override
  public void selected(Selectable selectable, Selector sel, int selection) {
    try {
      SSLEngine sslEngine = SslHelper.createSSLEngine(sslContext, tls);
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
      logger.log(ServerLogMessages.SSL_SERVER_ACCEPT_FAILED);
    }
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(SSLEndPointServer.class.getName() + "_" + url);
  }
}
