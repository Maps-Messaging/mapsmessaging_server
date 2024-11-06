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

import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.security.ssl.SslHelper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class SSLEndPointConnectionFactory implements EndPointConnectionFactory {

  private final Logger logger = LoggerFactory.getLogger(SSLEndPointConnectionFactory.class);

  // We need to open a socket, it's a socket library so we can ignore this issue
  @java.lang.SuppressWarnings({"squid:S4818", "squid:S2095"})
  @Override
  public EndPoint connect(EndPointURL url, SelectorLoadManager selector, EndPointConnectedCallback callback, EndPointServerStatus endPointServerStatus, List<String> jmxPath)
      throws IOException {
    TlsConfig securityProps = (TlsConfig) endPointServerStatus.getConfig().getEndPointConfig();
    SSLContext context = SslHelper.createContext(securityProps.getSslConfig().getContext(), securityProps.getSslConfig().toConfigurationProperties(), logger);
    SSLEngine engine = SslHelper.createSSLEngine(context, securityProps.getSslConfig().toConfigurationProperties());
    SocketChannel channel = SocketChannel.open();
    InetSocketAddress address = new InetSocketAddress(url.getHost(), url.getPort());
    channel.configureBlocking(true);
    channel.connect(address);
    channel.configureBlocking(false);
    return new SSLEndPoint(generateID(), engine, channel, selector.allocate(), callback, endPointServerStatus, jmxPath);
  }

  @Override
  public String getName() {
    return "ssl";
  }

  @Override
  public String getDescription() {
    return "SSL connection end point factory";
  }

}