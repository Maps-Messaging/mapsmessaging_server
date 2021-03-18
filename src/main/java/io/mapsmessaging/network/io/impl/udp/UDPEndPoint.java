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

package io.mapsmessaging.network.io.impl.udp;

import static io.mapsmessaging.logging.LogMessages.UDP_CREATED;
import static io.mapsmessaging.logging.LogMessages.UDP_READ_BYTES;
import static io.mapsmessaging.logging.LogMessages.UDP_SENT_BYTES;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;

public class UDPEndPoint extends EndPoint {

  protected final String authenticationConfig;
  private final DatagramChannel datagramChannel;
  private final Selector selector;
  private final EndPointJMX mbean;
  private final String name;

  public UDPEndPoint(InetSocketAddress inetSocketAddress, Selector selector, long id, EndPointServer server, String authConfig, EndPointManagerJMX managerMBean) throws IOException {
    super(id, server);
    this.selector = selector;
    datagramChannel = DatagramChannel.open();
    datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    datagramChannel.configureBlocking(false);
    datagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
    datagramChannel.socket().bind(inetSocketAddress);
    authenticationConfig = authConfig;
    name = getProtocol() + "_" + datagramChannel.getLocalAddress().toString();
    mbean = new EndPointJMX(managerMBean.getTypePath(), this);
    jmxParentPath = mbean.getTypePath();
    logger.log(UDP_CREATED, inetSocketAddress);
  }

  @Override
  public void close() {
    mbean.close();
  }

  @Override
  public String getProtocol() {
    return "udp";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    int result = datagramChannel.send(packet.getRawBuffer(), packet.getFromAddress());
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
  public String getName() {
    return name;
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(UDPEndPoint.class.getName() + "_" + getId());
  }

  @Override
  public boolean isUDP() {
    return true;
  }
}
