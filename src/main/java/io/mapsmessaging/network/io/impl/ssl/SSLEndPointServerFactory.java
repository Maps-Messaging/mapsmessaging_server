/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.ssl;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.tcp.TCPEndPointServerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SSLEndPointServerFactory extends TCPEndPointServerFactory {

  public SSLEndPointServerFactory() {
    super();
  }

  @Override
  public EndPointServer instance(
      EndPointURL url,
      SelectorLoadManager selector,
      AcceptHandler acceptHandler,
      NetworkConfig config,
      EndPointManagerJMX managerMBean)
      throws IOException {
    InetAddress bindAddress = InetAddress.getByName(url.getHost());
    InetSocketAddress inetSocketAddress = new InetSocketAddress(bindAddress, url.getPort());
    return new SSLEndPointServer(inetSocketAddress, selector, acceptHandler, config, url, managerMBean);
  }

  @Override
  public String getName() {
    return "ssl";
  }

  @Override
  public String getDescription() {
    return "SSL End Point Server Factory";
  }

}
