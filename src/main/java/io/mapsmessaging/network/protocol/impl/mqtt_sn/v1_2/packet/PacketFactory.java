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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;

import java.io.IOException;

public class PacketFactory {

  public MQTT_SNPacket parseFrame(Packet packet) throws IOException {
    int packetLength = packet.get() & 0xff;
    int dif;
    if (packetLength == 1) { // Special case so the next 2 bytes indicates the length
      packetLength = MQTTPacket.readShort(packet);
      dif = packet.available() - (packetLength - 3);
    } else {
      dif = packet.available() - (packetLength - 1);
    }
    if (dif < 0) {
      throw new IOException("Truncated Packet received");
    } else if (dif > 0) {
      throw new IOException("Extended Packet received");
    }

    int type = packet.get();

    MQTT_SNPacket mqtt = create(type, packetLength, packet);
    mqtt.setFromAddress(packet.getFromAddress());
    return mqtt;
  }

  protected MQTT_SNPacket create(int type, int length, Packet packet) throws IOException {
    switch (type) {
      case MQTT_SNPacket.CONNECT:
        try {
          return new Connect(packet, length);
        } catch (Exception e) {
          return getConnectError(ReasonCodes.NOT_SUPPORTED);
        }

      case MQTT_SNPacket.DISCONNECT:
        return new Disconnect(packet, length);

      case MQTT_SNPacket.SUBSCRIBE:
        return new Subscribe(packet);

      case MQTT_SNPacket.ADVERTISE:
        return new Advertise(packet, length);

      case MQTT_SNPacket.GWINFO:
        return new GatewayInfo(packet);

      case MQTT_SNPacket.SEARCHGW:
        return new SearchGateway(packet);

      case MQTT_SNPacket.WILLTOPIC:
        return new WillTopic(packet, length);

      case MQTT_SNPacket.WILLMSG:
        return new WillMessage(packet, length);

      case MQTT_SNPacket.REGISTER:
        return new Register(packet, length);

      case MQTT_SNPacket.REGACK:
        return new RegisterAck(packet, length);

      case MQTT_SNPacket.PUBLISH:
        return new Publish(packet, length);

      case MQTT_SNPacket.PUBACK:
        return new PubAck(packet);

      case MQTT_SNPacket.PUBREC:
        return new PubRec(packet);

      case MQTT_SNPacket.PUBCOMP:
        return new PubComp(packet);

      case MQTT_SNPacket.PUBREL:
        return new PubRel(packet);

      case MQTT_SNPacket.UNSUBSCRIBE:
        return new Unsubscribe(packet);

      case MQTT_SNPacket.PINGREQ:
        return new PingRequest(packet, length);

      case MQTT_SNPacket.PINGRESP:
        return new PingResponse();

      case MQTT_SNPacket.WILLTOPICUPD:
        return new WillTopicUpdate(packet, length);

      case MQTT_SNPacket.WILLMSGUPD:
        return new WillMessageUpdate(packet, length);

      default:
    }

    return null;
  }

  public MQTT_SNPacket getConnectError(ReasonCodes reasonCode) {
    return new ConnAck(reasonCode);
  }
}
