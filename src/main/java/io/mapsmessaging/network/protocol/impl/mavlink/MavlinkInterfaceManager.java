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

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionManager;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;

// The protocol is Mavlink so it makes sense
@SuppressWarnings("squid:S00101")
public class MavlinkInterfaceManager implements SelectorCallback {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final UDPSessionManager<MavlinkProtocol> currentSessions;
  private final byte gatewayId;
  private final ProtocolMessageTransformation transformation;

  private final boolean enablePortChanges;
  private final boolean enableAddressChanges;
  private final boolean advertiseGateway;

  private final MqttSnConfig mqttSnConfig;


  public MavlinkInterfaceManager(byte gatewayId, SelectorTask selectorTask, EndPoint endPoint) {
    logger = LoggerFactory.getLogger("MQTT-SN Protocol on " + endPoint.getName());
    this.gatewayId = gatewayId;
    this.selectorTask = selectorTask;
    this.endPoint = endPoint;
    mqttSnConfig = (MqttSnConfig) endPoint.getConfig().getProtocolConfig("mqtt-sn");
    long timeout = mqttSnConfig.getIdleSessionTimeout();
    enablePortChanges = mqttSnConfig.isEnablePortChanges();
    enableAddressChanges = mqttSnConfig.isEnableAddressChanges();
    advertiseGateway = mqttSnConfig.isAdvertiseGateway();
    currentSessions = new UDPSessionManager<>(timeout);
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "mavlink",
        "<registered>"
    );
  }

  public MavlinkInterfaceManager(InterfaceInformation info, EndPoint endPoint, byte gatewayId) throws IOException {
    logger = LoggerFactory.getLogger("Mavlink Protocol on " + endPoint.getName());
    this.endPoint = endPoint;
    this.gatewayId = gatewayId;
    mqttSnConfig = (MqttSnConfig) endPoint.getConfig().getProtocolConfig("mqtt-sn");
    long timeout = mqttSnConfig.getIdleSessionTimeout();
    enablePortChanges = mqttSnConfig.isEnablePortChanges();
    enableAddressChanges = mqttSnConfig.isEnableAddressChanges();
    advertiseGateway = mqttSnConfig.isAdvertiseGateway();

    currentSessions = new UDPSessionManager<>(timeout);

    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    if (startAdvertiseTask(info)) {
    }
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "mavlink",
        "<registered>"
    );
  }

  private boolean startAdvertiseTask(InterfaceInformation info) throws SocketException {
    return advertiseGateway && info.getBroadcast() != null && !info.isLoopback();
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    // OK, we have received a packet, lets find out if we have an existing context for it
    if (packet.getFromAddress() == null) {
      return true; // Ignoring packet since unknown client
    }
    UDPSessionState<MavlinkProtocol> state = currentSessions.getState(packet.getFromAddress());
    if(state == null && enablePortChanges){
      state = lookupByPacket(packet);
    }

    if (state != null && state.getContext() != null) {
      MavlinkProtocol protocol = state.getContext();
      protocol.processPacket(packet);
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private UDPSessionState<MavlinkProtocol> lookupByPacket(Packet packet) throws IOException {
    byte type = packet.get(1);
    return null;
  }

  private void processIncomingPacket(Packet packet) throws IOException {
    int len = packet.available();

  }

  @Override
  public void close() {
    currentSessions.close();
  }

  @Override
  public String getName() {
    return "Mavlink";
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
    currentSessions.deleteState(remoteClient);
  }

}
