/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt_sn;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.SessionManager;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.network.io.EndPoint;
import org.maps.network.io.InterfaceInformation;
import org.maps.network.io.Packet;
import org.maps.network.io.impl.SelectorCallback;
import org.maps.network.io.impl.SelectorTask;
import org.maps.network.protocol.ProtocolMessageTransformation;
import org.maps.network.protocol.impl.mqtt_sn.packet.Advertise;
import org.maps.network.protocol.impl.mqtt_sn.packet.Connect;
import org.maps.network.protocol.impl.mqtt_sn.packet.GatewayInfo;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.PacketFactory;
import org.maps.network.protocol.impl.mqtt_sn.packet.Publish;
import org.maps.network.protocol.impl.mqtt_sn.packet.SearchGateway;
import org.maps.network.protocol.transformation.TransformationManager;

// The protocol is MQTT_SN so it makes sense
@java.lang.SuppressWarnings("squid:S00101")
public class MQTTSNInterfaceManager implements SelectorCallback {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final HashMap<SocketAddress, MQTT_SNProtocol> currentSessions;
  private final PacketFactory packetFactory;
  private final AdvertiserTask advertiserTask;
  private final byte gatewayId;
  private final RegisteredTopicConfiguration registeredTopicConfiguration;
  private final ProtocolMessageTransformation transformation;

  public MQTTSNInterfaceManager(byte gatewayId, SelectorTask selectorTask, EndPoint endPoint) {
    logger = LoggerFactory.getLogger("MQTT-SN 1.2 Protocol on " + endPoint.getName());
    this.gatewayId = gatewayId;
    this.selectorTask = selectorTask;
    advertiserTask = null;
    this.endPoint = endPoint;
    currentSessions = new LinkedHashMap<>();
    packetFactory = new PacketFactory();
    transformation = TransformationManager.getInstance().getTransformation(getName(), "<registered>");
    registeredTopicConfiguration = new RegisteredTopicConfiguration(endPoint.getConfig().getProperties());
  }

  public MQTTSNInterfaceManager(InterfaceInformation info, EndPoint endPoint, byte gatewayId) throws IOException {
    logger = LoggerFactory.getLogger("MQTT-SN 1.2 Protocol on " + endPoint.getName());
    this.endPoint = endPoint;
    this.gatewayId = gatewayId;
    currentSessions = new LinkedHashMap<>();
    packetFactory = new PacketFactory();

    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    if (info.getBroadcast() != null && !info.isLoopback()) {
      advertiserTask = new AdvertiserTask(gatewayId, endPoint, info, info.getBroadcast(), DefaultConstants.ADVERTISE_INTERVAL);
    } else {
      advertiserTask = null;
    }
    registeredTopicConfiguration = new RegisteredTopicConfiguration(endPoint.getConfig().getProperties());
    transformation = TransformationManager.getInstance().getTransformation(getName(), "<registered>");
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
      //
      // OK so this is either a new connection request or an admin request
      //
      MQTT_SNPacket mqttSn = null;
      try {
        mqttSn = packetFactory.parseFrame(packet);
      } catch (IOException ioException) {
        // We received a corrupt packet, there is not much we can do but ignore it since it could just be noise
      }
      // Cool, so we have a new connect, so lets create a new protocol Impl and add it into our list
      // of current sessions
      if (mqttSn instanceof Connect) {
        MQTT_SNProtocol impl = new MQTT_SNProtocol(this, endPoint, packet.getFromAddress(), selectorTask, (Connect) mqttSn);
        currentSessions.put(packet.getFromAddress(), impl);
      } else if (mqttSn instanceof SearchGateway) {
        // This is a client asking for information about existing gateways on the network
        GatewayInfo gatewayInfo = new GatewayInfo(gatewayId);
        Packet gwInfo = new Packet(3, false);
        gatewayInfo.packFrame(gwInfo);
        gwInfo.setFromAddress(packet.getFromAddress());
        endPoint.sendPacket(gwInfo);
      } else if (mqttSn instanceof Publish) {
        Publish publish = (Publish) mqttSn;
        if (publish.getQoS().equals(QualityOfService.MQTT_SN_REGISTERED)) {
          String topic = registeredTopicConfiguration.getTopic(packet.getFromAddress(), publish.getTopicId());
          if (topic != null) {
            logger.log(LogMessages.MQTT_SN_REGISTERED_EVENT, topic);
            publishRegisteredTopic(topic, publish);
          } else {
            logger.log(LogMessages.MQTT_SN_REGISTERED_EVENT_NOT_FOUND, packet.getFromAddress(), publish.getTopicId());
          }
        } else {
          logger.log(LogMessages.MQTT_SN_INVALID_QOS_PACKET_DETECTED, packet.getFromAddress(), publish.getQoS());

        }
      }
      else if(mqttSn instanceof Advertise){
        Advertise advertise = (Advertise) mqttSn;
        logger.log(LogMessages.MQTT_SN_GATEWAY_DETECTED, advertise.getId(), packet.getFromAddress().toString());
      } else {
        packet.flip();
      }
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
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
    SessionManager.getInstance().publish(topic, messageBuilder.build());
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
