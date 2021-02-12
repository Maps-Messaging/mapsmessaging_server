/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package org.maps.network.io.impl.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.admin.EndPointManagerJMX;
import org.maps.network.io.AcceptHandler;
import org.maps.network.io.EndPointServer;
import org.maps.network.io.impl.SelectorLoadManager;
import org.maps.network.io.impl.tcp.TCPEndPointServerFactory;

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
    return new SSLEndPointServer(
        inetSocketAddress, selector, acceptHandler, config, url, managerMBean);
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
