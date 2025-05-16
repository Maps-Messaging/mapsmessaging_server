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

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.udp.UDPFacadeEndPoint;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import lombok.Getter;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class CoapInterfaceManager implements SelectorCallback {

  @Getter
  private final int mtu;
  private final EndPoint endPoint;
  private final HashMap<SocketAddress, CoapProtocol> currentSessions;
  private final ProtocolMessageTransformation transformation;

  public CoapInterfaceManager(EndPoint endPoint, int mtu) throws IOException {
    this.endPoint = endPoint;
    this.mtu = mtu;
    currentSessions = new LinkedHashMap<>();
    SelectorTask selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "coap",
        "anonymous"
    );

  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    // OK, we have received a packet, lets find out if we have an existing context for it
    if (packet.getFromAddress() == null) {
      return true; // Ignoring packet since unknown client
    }

    CoapProtocol protocol = currentSessions.get(packet.getFromAddress());
    if (protocol == null) {
      try {
        UDPFacadeEndPoint coapClientEndPoint = new UDPFacadeEndPoint(endPoint, packet.getFromAddress(), endPoint.getServer());
        protocol = new CoapProtocol(coapClientEndPoint, this, packet.getFromAddress());
        endPoint.getServer().handleNewEndPoint(coapClientEndPoint);
        currentSessions.put(packet.getFromAddress(), protocol);
      } catch (LoginException e) {
        throw new IOException(e);
      }
    }
    if (protocol.getSession().isClosed()) {
      currentSessions.remove(packet.getFromAddress());
      endPoint.getServer().handleCloseEndPoint(protocol.getEndPoint());
      return processPacket(packet);
    }
    return protocol.processPacket(packet);
  }

  @Override
  public void close() {
    for (CoapProtocol protocol : currentSessions.values()) {
      try {
        protocol.close();
      } catch (IOException e) {
        // Ignore we are closing
      }
      currentSessions.clear();
    }
  }

  @Override
  public String getName() {
    return "CoAP";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }

  public void close(SocketAddress remoteClient) {
    currentSessions.remove(remoteClient);
  }

}