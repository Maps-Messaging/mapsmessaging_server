/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.*;
import io.mapsmessaging.network.io.impl.Selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class UDPEndPoint extends EndPoint {

  protected final String authenticationConfig;
  private final DatagramChannel datagramChannel;
  private final Selector selector;
  private final EndPointJMX mbean;
  private final InetSocketAddress remoteAddress;

  public UDPEndPoint(InetSocketAddress remote, Selector selector, long id, EndPointServerStatus endPointServerStatus, List<String> jmxParent) throws IOException {
    super(id, endPointServerStatus);
    remoteAddress = remote;
    this.selector = selector;
    authenticationConfig = null;
    datagramChannel = DatagramChannel.open();
    datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    datagramChannel.configureBlocking(false);
    datagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
    datagramChannel.socket().bind(null);
    name = getProtocol() + "_" + datagramChannel.getLocalAddress().toString();
    if (jmxParent != null && !jmxParent.isEmpty()) {
      mbean = new EndPointJMX(jmxParent, this);
      jmxParentPath = mbean.getTypePath();
    } else {
      mbean = null;
      jmxParentPath = new ArrayList<>();
    }
    logger.log(UDP_CREATED, datagramChannel.getLocalAddress());
  }

  public UDPEndPoint(InetSocketAddress inetSocketAddress, Selector selector, long id, EndPointServer server, String authConfig, EndPointManagerJMX managerMBean)
      throws IOException {
    super(id, server);
    currentConnections.decrement();
    remoteAddress = null;
    this.selector = selector;
    datagramChannel = DatagramChannel.open();
    datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    datagramChannel.configureBlocking(false);
    datagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
    datagramChannel.socket().bind(inetSocketAddress);
    authenticationConfig = authConfig;
    name = getProtocol() + "_" + datagramChannel.getLocalAddress().toString();
    if (managerMBean != null) {
      mbean = new EndPointJMX(managerMBean.getTypePath(), this);
      jmxParentPath = mbean.getTypePath();
    } else {
      mbean = null;
      jmxParentPath = new ArrayList<>();
    }
    logger.log(UDP_CREATED, inetSocketAddress);
  }

  @Override
  public String getProtocol() {
    return "udp";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    SocketAddress host = packet.getFromAddress();
    if(remoteAddress != null){
      host = remoteAddress;
    }
    int result = datagramChannel.send(packet.getRawBuffer(), host);
    logger.log(UDP_SENT_BYTES, result);
    updateWriteBytes(result);
    return result;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int pos = packet.position();
    packet.setFromAddress(datagramChannel.receive(packet.getRawBuffer()));
    int result = packet.position() - pos;
    logger.log(UDP_READ_BYTES, result);
    updateReadBytes(result);
    return result;
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return selector.register(datagramChannel, selectionKey, runner);
  }

  @Override
  public FutureTask<SelectionKey> deregister(int opRead) throws ClosedChannelException {
    return selector.register(datagramChannel, 0, null);
  }

  @Override
  public String getAuthenticationConfig() {
    return authenticationConfig;
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(UDPEndPoint.class.getName() + "_" + getId());
  }

  @Override
  public String getRemoteSocketAddress() {
    return "";
  }

  @Override
  public boolean isUDP() {
    return true;
  }
}
