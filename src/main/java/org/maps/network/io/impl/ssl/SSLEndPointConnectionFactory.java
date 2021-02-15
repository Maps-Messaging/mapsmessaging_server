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

package org.maps.network.io.impl.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.EndPointURL;
import org.maps.network.io.EndPointConnectedCallback;
import org.maps.network.io.EndPointConnectionFactory;
import org.maps.network.io.EndPointServerStatus;
import org.maps.network.io.impl.SelectorLoadManager;

public class SSLEndPointConnectionFactory extends EndPointConnectionFactory {

  private final Logger logger = LoggerFactory.getLogger(SSLEndPointConnectionFactory.class);

  @Override
  public void connect(EndPointURL url, SelectorLoadManager selector, EndPointConnectedCallback callback, EndPointServerStatus endPointServerStatus, List<String> jmxPath) throws IOException {
    SSLContext context = SSLHelper.getInstance().createContext(endPointServerStatus.getConfig().getProperties(), logger);
    SSLEngine engine = context.createSSLEngine();
    SocketChannel channel = SocketChannel.open();
    InetSocketAddress address = new InetSocketAddress(url.getHost(), url.getPort());
    channel.configureBlocking(true);
    channel.connect(address);
    channel.configureBlocking(false);
    new SSLEndPoint(generateID(), engine, channel.socket(), selector.allocate(), callback, endPointServerStatus, jmxPath);
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