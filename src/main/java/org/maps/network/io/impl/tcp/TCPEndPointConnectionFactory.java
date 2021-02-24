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

package org.maps.network.io.impl.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import org.maps.network.EndPointURL;
import org.maps.network.admin.EndPointConnectionJMX;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointConnectedCallback;
import org.maps.network.io.EndPointConnectionFactory;
import org.maps.network.io.EndPointServerStatus;
import org.maps.network.io.impl.SelectorLoadManager;

public class TCPEndPointConnectionFactory extends EndPointConnectionFactory {

  // We need to open a socket, its a socket library so we can ignore this issue
  @java.lang.SuppressWarnings({"squid:S4818", "squid:S2095"})
  @Override
  public EndPoint connect(EndPointURL url, SelectorLoadManager selector, EndPointConnectedCallback connectedCallback, EndPointServerStatus endPointServerStatus, List<String> jmxPath) throws IOException {
    SocketChannel channel = SocketChannel.open();
    InetSocketAddress address = new InetSocketAddress(url.getHost(), url.getPort());
    channel.connect(address);
    EndPoint endPoint = new TCPEndPoint(generateID(), channel, selector.allocate(), endPointServerStatus, jmxPath);
    connectedCallback.connected(endPoint);
    return endPoint;
  }

  @Override
  public String getName() {
    return Constants.name;
  }

  @Override
  public String getDescription() {
    return "tcp connection end point factory";
  }

}
