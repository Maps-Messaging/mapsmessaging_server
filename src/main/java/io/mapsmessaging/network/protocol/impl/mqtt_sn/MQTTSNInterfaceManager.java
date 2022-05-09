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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Advertise;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.SearchGateway;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.MQTT_SNProtocolV2;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.PacketFactoryV2;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;

// The protocol is MQTT_SN so it makes sense
@java.lang.SuppressWarnings("squid:S00101")
public class MQTTSNInterfaceManager implements SelectorCallback {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final HashMap<SocketAddress, MQTT_SNProtocol> currentSessions;
  private final PacketFactory[] packetFactory;
  private final AdvertiserTask advertiserTask;
  private final byte gatewayId;
  private final RegisteredTopicConfiguration registeredTopicConfiguration;
  private final ProtocolMessageTransformation transformation;

  public MQTTSNInterfaceManager(byte gatewayId, SelectorTask selectorTask, EndPoint endPoint) {
    logger = LoggerFactory.getLogger("MQTT-SN Protocol on " + endPoint.getName());
    this.gatewayId = gatewayId;
    this.selectorTask = selectorTask;
    advertiserTask = null;
    this.endPoint = endPoint;
    currentSessions = new LinkedHashMap<>();
    packetFactory = new PacketFactory[2];
    packetFactory[0] = new PacketFactory();
    packetFactory[1] = new PacketFactoryV2();
    transformation = TransformationManager.getInstance().getTransformation(getName(), "<registered>");
    registeredTopicConfiguration = new RegisteredTopicConfiguration(endPoint.getConfig().getProperties());
  }

  public MQTTSNInterfaceManager(InterfaceInformation info, EndPoint endPoint, byte gatewayId) throws IOException {
    logger = LoggerFactory.getLogger("MQTT-SN Protocol on " + endPoint.getName());
    this.endPoint = endPoint;
    this.gatewayId = gatewayId;
    currentSessions = new LinkedHashMap<>();
    packetFactory = new PacketFactory[2];
    packetFactory[0] = new PacketFactory();
    packetFactory[1] = new PacketFactoryV2();

    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    if (startAdvertiseTask(info)) {
      advertiserTask = new AdvertiserTask(gatewayId, endPoint, info, info.getBroadcast(), DefaultConstants.ADVERTISE_INTERVAL);
    } else {
      advertiserTask = null;
    }
    registeredTopicConfiguration = new RegisteredTopicConfiguration(endPoint.getConfig().getProperties());
    transformation = TransformationManager.getInstance().getTransformation(getName(), "<registered>");
  }

  private boolean startAdvertiseTask(InterfaceInformation info) throws SocketException {
    boolean configToSend = getEndPoint().getConfig().getProperties().getBooleanProperty("advertiseGateway", false);
    return configToSend && info.getBroadcast() != null && !info.isLoopback();
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    // OK, we have received a packet, lets find out if we have an existing context for it
    if (packet.getFromAddress() == null) {
      return true; // Ignoring packet since unknown client
    }
    MQTT_SNProtocol protocol = currentSessions.get(packet.getFromAddress());
    if (protocol != null) {
      // OK we have an existing protocol, so simply hand over the packet for processing
      protocol.processPacket(packet);
    } else {
      int offset = 0;
      if(packet.get(0) == 1){
        offset = 2;
      }
      int version = -1;
      boolean isConnect = packet.get(1+offset) == MQTT_SNPacket.CONNECT;
      if(isConnect && (packet.get(2+offset) & 0b11111000) == 0){
        version = packet.get(3+offset);
      }
      //
      // OK so this is either a new connection request or an admin request
      //
      PacketFactory factory = packetFactory[0];
      if(version == 2){
        factory = packetFactory[1];
      }

      try {
        processIncomingPacket(packet, factory);
      } catch (IOException ioException) {

      }
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private void processIncomingPacket(Packet packet, PacketFactory factory) throws IOException {
    MQTT_SNPacket mqttSn = factory.parseFrame(packet);

    if (mqttSn instanceof Connect) {
      // Cool, so we have a new connect, so let's create a new protocol Impl and add it into our list
      // of current sessions
      MQTT_SNProtocol impl = new MQTT_SNProtocol(this, endPoint, packet.getFromAddress(), selectorTask, (Connect) mqttSn);
      currentSessions.put(packet.getFromAddress(), impl);
    }
    else if (mqttSn instanceof io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect) {
      // Cool, so we have a new connect, so let's create a new protocol Impl and add it into our list
      // of current sessions
      io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect connectV2 = (io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Connect)mqttSn;
      MQTT_SNProtocol impl = new MQTT_SNProtocolV2(this, endPoint, packet.getFromAddress(), selectorTask, connectV2);
      currentSessions.put(packet.getFromAddress(), impl);
    }else if (mqttSn instanceof SearchGateway) {
      handleSearch(packet);
    } else if (mqttSn instanceof Publish) {
      handlePublish(packet, (Publish) mqttSn);
    }
    else if(mqttSn instanceof Advertise){
      handleAdvertise(packet, (Advertise) mqttSn);
    }
    else if(mqttSn instanceof ConnAck || mqttSn instanceof io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.ConnAck){
      Packet error = new Packet(32, false);
      mqttSn.packFrame(error);
      error.setFromAddress(packet.getFromAddress());
      error.flip();
      endPoint.sendPacket(error);
    }else {
      packet.flip();
    }
  }

  private void handleSearch(Packet packet) throws IOException {
    // This is a client asking for information about existing gateways on the network
    GatewayInfo gatewayInfo = new GatewayInfo(gatewayId);
    Packet gwInfo = new Packet(3, false);
    gatewayInfo.packFrame(gwInfo);
    gwInfo.setFromAddress(packet.getFromAddress());
    endPoint.sendPacket(gwInfo);
  }

  private void handlePublish(Packet packet, Publish publish) throws IOException {
    if (publish.getQoS().equals(QualityOfService.MQTT_SN_REGISTERED)) {
      String topic = registeredTopicConfiguration.getTopic(packet.getFromAddress(), publish.getTopicId());
      if (topic != null) {
        logger.log(ServerLogMessages.MQTT_SN_REGISTERED_EVENT, topic);
        publishRegisteredTopic(topic, publish);
      } else {
        logger.log(ServerLogMessages.MQTT_SN_REGISTERED_EVENT_NOT_FOUND, packet.getFromAddress(), publish.getTopicId());
      }
    } else {
      logger.log(ServerLogMessages.MQTT_SN_INVALID_QOS_PACKET_DETECTED, packet.getFromAddress(), publish.getQoS());
    }
  }

  private void handleAdvertise(Packet packet, Advertise advertise ){
    logger.log(ServerLogMessages.MQTT_SN_GATEWAY_DETECTED, advertise.getGatewayId(), packet.getFromAddress().toString());
  }

  private void publishRegisteredTopic(String topic, Publish publish) throws IOException {
    MessageBuilder messageBuilder = new MessageBuilder();
    if (publish.retain()) {
      messageBuilder.storeOffline(true)
          .setRetain(true)
          .setTransformation(transformation)
          .setQoS(QualityOfService.AT_LEAST_ONCE); // Store for Retain
    } else {
      messageBuilder.storeOffline(false)
          .setRetain(false)
          .setTransformation(transformation)
          .setQoS(QualityOfService.AT_MOST_ONCE); // Always for these events
    }
    messageBuilder.setOpaqueData(publish.getMessage());
    try {
      SessionManager.getInstance().publish(topic, messageBuilder.build()).get();
    } catch (ExecutionException|InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() {
    advertiserTask.stop();
  }

  @Override
  public String getName() {
    return "MQTT_SN";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }

  public void close(SocketAddress remoteClient) {
    currentSessions.remove(remoteClient);
  }

}
